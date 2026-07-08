# agentic-review-tool

**Your code review team fits in a JAR. They argue with each other so you don't have to.**

Frontier-model PR reviews are excellent and expensive — like hiring a senior engineer who bills by the syllable. This project is the other path: a **local-first, rules-driven review pipeline** that sends your diff to specialized agents (exceptional handling, formatting, Javadoc, POM security, …), then has a summarizer judge the pile and tell you whether to merge, nitpick, or go touch grass.

Built with **langchain4j**, **picocli**, and enough prompt discipline to stop local models from flagging deleted `throws` lines as must-fix violations. We have scars. We wrote rules about it.

```
Ingest → Classify → Review → Summarize → (optional) Chat
```

## Why bother?

| Problem | What we do |
|---------|------------|
| One giant "review my PR" prompt | **One agent per ruleset** — focused prompts, less hallucination |
| $$$ per review on cloud models | **Ollama at home** or **OpenRouter** when you want a tie-breaker |
| "Our standards" live in someone's head | **Markdown rules** with YAML `paths:` globs — no redeploy to add a rule |
| LLM loops for 10 minutes on a 30-file diff | **`reviewMaxTokens` cap**, batch splitting, diff-only discipline |
| "LGTM" with no receipts | **Markdown report** + health score + token usage per call |

## Quick start

**Prerequisites:** Java 21+, Maven, Git repo, and either [Ollama](https://ollama.com) or an OpenRouter API key.

```bash
git clone https://github.com/sdempsay/agentic-review-tool.git
cd agentic-review-tool
mvn package -DskipTests
```

Copy config and point at your model:

```bash
mkdir -p ~/.code-review
cp src/main/resources/default-config.json ~/.code-review/config.json
# edit provider, baseUrl, model name, rulesDir as needed
```

Sanity check:

```bash
java -jar target/code-review.jar doctor
```

Review uncommitted work:

```bash
java -jar target/code-review.jar diff
```

Review everything against a base branch:

```bash
java -jar target/code-review.jar diff --base origin/master --output review.md
```

Full repo (tracked + untracked, gitignore-safe):

```bash
java -jar target/code-review.jar repo --output repo-review.md
```

Add `--no-chat` in CI. Add `--verbose` when you want to watch the models think in real time (Ollama streaming).

## The cast

Each changed file gets matched to rules by path glob. Every matched ruleset gets its own agent call:

- **java-exceptional** — `ExceptionalResponse` everywhere it hurts; no `throws` tourism
- **java-formatting** — checkstyle-aligned nits, diff-aware
- **java-javadoc** — public API documentation police (optional resentment)
- **pom-security** / **xml-formatter** — Maven and XML hygiene
- **general** — files that matched nothing else; still gets a fair shake

Then **Summarize** rolls it up: health score (1–10), `APPROVE` / `APPROVE_WITH_NITS` / `REQUEST_CHANGES` / `BLOCK`, and top actions. Token counts per call are printed so you can compare Ollama vs Sonnet without spreadsheet cosplay.

## Configuration highlights

```json
{
  "model": {
    "provider": "ollama",
    "name": "qwen3-coder-next-256k:latest",
    "baseUrl": "http://localhost:11434",
    "temperature": 0.2
  },
  "rulesDir": "rules",
  "reviewMaxTokens": 4096,
  "maxAgentDiffKb": 256,
  "maxFilesPerAgent": 0
}
```

- **`reviewMaxTokens`** — output cap per agent/summarize/chat call (stops runaway generation)
- **`maxAgentDiffKb`** / **`maxFilesPerAgent`** — split huge reviews into batches
- **`rulesDir`** — your team's markdown rules (bundled `rules/` is a starting set)

OpenRouter: set `"provider": "openrouter"`, model name, and `OPENROUTER_API_KEY` or `model.apiKey`.

## Writing rules

Drop a `.md` file in `rulesDir`:

```yaml
---
paths:
  - "**/*.java"
  - "**/*.java.ftl"
---

# My team's sacred law

FreeMarker templates that emit Java use the `.java.ftl` suffix.
Review the Java they would emit; ignore `<#if>` — it's not a style violation, it's a lifestyle choice.
```

No Java changes required. The pipeline classifies, prompts, and ships.

## Diff discipline (for the LLM, and for your sanity)

Unified diffs are first-class. Agents are trained (via `review-output-format.md` and ruleset preambles) to:

- Report violations only on **`+` lines** in diffs
- Never cite removed **`-` lines** as current bugs
- Treat **removing `throws`** as a migration win, not a regression

We dogfood this repo against `origin/master` on Ollama and OpenRouter. The prompts are tight because the alternative is reading 400 lines of `GitRunner.java:1600 — throw in chain`.

## Follow-up chat

Finished the report and still confused? Stay in the terminal:

```
Ask questions about this review (exit to end).
> Is the listener threading in RulesEngine actually necessary?
```

The orchestrator can delegate back to ruleset agents with file context when you name a path.

## Build & test

```bash
mvn verify
```

Checkstyle included. Exceptional error handling in production code — because we review for it and we live it. See `AGENTS.md` for contributor patterns.

## Project docs

- `PRD.md` — product shape and MVP scope
- `PRD-updated.md` — implementation learnings
- `TODO.md` — task tracker
- `ACTIONS.md` — work log

## License & attitude

Open source spirit, local-first privacy, and a firm belief that **"Clean: all 47 files in scope"** should never be a valid review output.

Star the repo if it saved you from a bad merge. Open an issue if your model still thinks deleted code is alive. We have more prompt discipline where that came from.