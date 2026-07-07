# Code Review Pipeline PRD (Preliminary)

**Version:** 0.1 (MVP - CLI Focused)
**Date:** 2026-07-07
**Authors:** Senior Software Architect (with input from Principal)
**Status:** Preliminary - Open for review and iteration

## 1. Overview

Commercial code review tools using frontier models incur high costs due to large context windows and prompt bloat. This project builds a cost-effective alternative using **langchain-4j** with local (Ollama) or lower-cost (OpenRouter) LLMs.

**Goal:** Create an extensible, agentic code review pipeline that supports differential reviews (primary) and repository-wide reviews, with strong customization for organizational rules.

**Core Pipeline:**
**Ingest → Classify → Review → Summarize**

## 2. Objectives

- Deliver high-quality code reviews at significantly lower cost than frontier models.
- Support dynamic, file-path-aware Java formatting and style rules (leverage existing Claude prompt files).
- Provide an interactive CLI as the MVP interface.
- Ensure high extensibility at every stage.
- Maintain security and auditability (local-first, future digitally signed prompts).

## 3. Scope

### In Scope (MVP)
- CLI tool (`code-review` command).
- Differential review using Git diffs.
- Model configuration via simple JSON.
- Dynamic rules loaded from directory with YAML frontmatter (`paths` globs).
- Java formatting and style review agents.
- Interactive terminal output + Markdown export option.
- Basic summarization and recommendation.

### Out of Scope (MVP)
- Full repository review (Phase 2).
- Auto-fix suggestions.
- Direct GitLab MCP comment posting.
- Advanced RAG over entire codebase.

## 4. Architecture

### High-Level Flow
1. **Ingest** — Use Git tools (`git diff`, etc.) to load changes + file paths.
2. **Classify** — Match changed files against rule frontmatter `paths` globs to select applicable rulesets (primary mechanism for MVP).
3. **Review** — Execute specialized sub-agents with relevant context and matched rules.
4. **Summarize** — Aggregate findings, provide overall health score and recommendation.

### Key Components
- **CLI Layer**: Interactive command line.
- **Configuration**: JSON file for models, rules directory, etc.
- **Rules Engine**: Load markdown files from directory. Each file contains YAML frontmatter (`paths:` globs) + prompt content.
- **Orchestration**: langchain-4j (chains + agentic module).
- **Models**: Configurable via JSON.

### Extensibility Notes
- **Primary Classification (MVP)**: Path-based globs (e.g. `**/*.java`, `**/service/**`).
- Future enhancements: Other classification signals (e.g. content analysis, commit messages, annotations, etc.) can be added without breaking existing rules.
- Easy to drop in new rule files.

## 5. User Stories & Features (MVP)

### CLI Interface
- `code-review diff [options]` — Main command.
- Interactive results viewer.
- Option to save full report to Markdown.
- **Follow-up chat** — After the review report, an optional interactive Q&A session
  lets the developer ask clarifying questions about findings, trade-offs, or specific
  files. The chat agent receives the completed report (agent reviews + summary) as
  context and maintains conversation history for the session.

#### Follow-up chat (task 16)

**Flow**
1. Pipeline completes (Ingest → Classify → Review → Summarize).
2. Report prints to terminal; optionally saved via `--output`.
3. If interactive mode is enabled and stdin is a TTY, enter a REPL:
   `Ask questions about this review (exit to end).`
4. Each user message is sent to the LLM with the report as system context plus prior
   turns in the session.
5. Session ends on `exit`, `quit`, or EOF.

**CLI flags**
- `--chat` — Enable follow-up chat after review (default on when stdin is a TTY).
- `--no-chat` — Skip chat; print report and exit (for CI/scripting).

**Context strategy**
- The chat agent starts with the completed report (agent findings, summary, file list,
  classification) — enough for high-level follow-ups.
- When the user drills into a specific file, rule, or finding, the chat agent should
  be able to **re-invoke the relevant ruleset review agent** with expanded context:
  - The ruleset's prompt (e.g. `java-general`)
  - That file's diff (re-loaded from ingest, not from memory)
  - The original finding + the user's follow-up question
- Routing uses the existing classification map: a question about `Foo.java` delegates to
  whichever ruleset agents matched that file; unmatched files go to the `general` agent.
- The chat agent acts as an **orchestrator** — it answers directly when the report
  suffices, delegates to a specialist when deeper analysis is needed.

**Example**
```
You> Look closer at indentation in AetherBuilderProcessor — is it really wrong?
```
→ Chat orchestrator re-invokes `java-general` with that file's diff + question.
→ Returns the specialist's deeper analysis, framed as a chat reply.

**Non-goals**
- Persistent chat sessions across CLI invocations.
- Applying fixes or committing changes from chat.

### Enhanced CLI Experience (Post-MVP)

The current CLI prints ingest/classification output, then blocks silently while each
ruleset agent calls the LLM (multi-minute runs are common). A richer terminal UI should
keep the developer informed throughout the pipeline.

**Goals**
- Make long reviews feel responsive and debuggable.
- Surface per-stage and per-file progress without waiting for the final aggregate.
- Optionally expose model reasoning ("thinking") when the provider supports streaming.

**Planned capabilities**
- **Pipeline progress** — Show current stage (Ingest → Classify → Review → Summarize)
  with timestamps and elapsed time.
- **Per-agent status** — When a ruleset sub-agent starts, report agent name, matched
  file count, and which files are in scope (e.g. `Reviewing: java-general (6 files)`).
- **Per-file progress** — As each file enters review, emit a concise status line
  (path, change type, applicable rules). Useful when one agent covers many files.
- **Streaming output** — Stream LLM tokens to the terminal as they arrive (thinking
  traces, interim analysis, final findings). Fall back to batch output when streaming
  is unavailable.
- **Verbosity controls** — `--quiet` (errors + summary only), default (progress +
  findings), `--verbose` (classification detail + streamed thinking).
- **Activity indicator** — Spinner or progress bar during LLM calls so a silent hang
  is never mistaken for a crash.

**Non-goals (for this enhancement)**
- Full-screen TUI framework (e.g. curses) — keep plain terminal + ANSI unless needs
  prove otherwise.
- Persisting streamed thinking to disk by default — optional `--output` capture only.

**Success criteria**
- A `diff` run against a typical multi-file commit shows continuous progress within
  the first second of each stage.
- User can identify which agent/file is running if a review stalls.
- Streaming mode works with Ollama; degrades gracefully for non-streaming providers.

### Ruleset batching and context caps

Per-ruleset agents batch all matching files into a single LLM call by default (e.g. six
`*.java` files → one `java-general` call). This is efficient but can exceed the model
context window on large commits.

**Batching policy**
- Group reviewable files by matched ruleset (current behaviour).
- Before calling the LLM, estimate prompt size: ruleset instructions + combined diffs.
- When the batch exceeds configured caps, split into multiple sub-batches for the same
  agent (e.g. `java-general` batch 1/2, batch 2/2).
- Sub-batch boundaries prefer keeping related files together where possible; fall back to
  greedy packing by file order when over cap.

**Configurable limits** (in `config.json`)

| Field | Purpose | Default |
|-------|---------|---------|
| `maxDiffKb` | Per-file ingest cap; files over this are skipped | `512` |
| `maxAgentDiffKb` | Max combined diff KB sent per ruleset agent call | `256` |
| `maxFilesPerAgent` | Optional file-count cap per agent call (0 = unlimited) | `0` |
| `maxTokens` | Model generation token limit | `8000` |

`maxAgentDiffKb` is the primary context cap for batching. `maxFilesPerAgent` is a
secondary guard for many small files. Both are user-configurable.

**CLI feedback when splitting**
- Report sub-batch index (e.g. `Reviewing: java-general (batch 2/3, 4 files)`).
- Tie into Enhanced CLI progress reporting (§5).

### Configuration (`config.json`)
```json
{
  "model": { "provider": "ollama", "name": "qwen3", "temperature": 0.2 },
  "rulesDir": "~/.grok/rules",
  "maxTokens": 8000,
  "maxDiffKb": 512,
  "maxAgentDiffKb": 256,
  "maxFilesPerAgent": 0
}
```

### Rules Example (`rules/java-general.md`)
```markdown
---
paths:
  - "**/*.java"
---

# Java styling rules

## 1. Generalized Java rules
...
```

## 6. Non-Functional Requirements

- Performance suitable for typical developer workflows.
- Local-first security model.
- Rules extensible by adding/editing files (no code changes needed).

## 7. Open Items / Future Phases

- Enhanced CLI experience with live progress and streaming thinking (see §5).
- Configurable ruleset batch context caps (`maxAgentDiffKb`, `maxFilesPerAgent`).
- Additional classification methods beyond path globs.
- Full repo review, MCP integration, signed prompts, etc.

## 8. Success Criteria (MVP)

- Effective dynamic rule selection via path globs.
- High-quality Java style/formatting reviews.
- Easy rule extension and maintenance.
