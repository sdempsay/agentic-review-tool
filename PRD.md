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

### Configuration (`config.json`)
```json
{
  "model": { "provider": "ollama", "name": "qwen3", "temperature": 0.2 },
  "rulesDir": "~/.code-review/rules",
  "maxTokens": 8000
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

- Additional classification methods beyond path globs.
- Full repo review, MCP integration, signed prompts, etc.

## 8. Success Criteria (MVP)

- Effective dynamic rule selection via path globs.
- High-quality Java style/formatting reviews.
- Easy rule extension and maintenance.
