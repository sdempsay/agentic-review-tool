# agentic-review-tool

*Or: How I Learned to Stop Paying by the Syllable and Love the Ruleset Agent*

---

## The Hook

You know what the best thing about AI code review is? There's always *more* of it!

Every team eventually tries the same experiment: paste a 3,000-line diff into a frontier model and ask for a verdict. The model responds with confidence, citations, and at least three findings on lines you deleted six commits ago. You squint at `GitRunner.java:1600` in a 104-line file and wonder whether you're reviewing code or reviewing the prompt.

Someone, somewhere, once thought: *"One big context window can understand everything at once!"* And thus, the monolithic PR review was born.

*Quality sometimes achieved. Bill definitely sent.*

---

## The Vibe

I watched our own pipeline fail the same way for a while. One ruleset, one enormous diff, one local model with a 24,000-token generation budget and no off switch. It would loop. It would flag removed `throws` declarations as must-fix violations. It would list every clean file in the repo because the output format said "be thorough."

And honestly? The model wasn't stupid. We were asking it to do too much in one breath.

What if we stopped pretending one prompt could hold our entire engineering culture and instead... sent each concern to a specialist who only sees the rules it actually cares about?

**That's what this tool does.**

It ingests your changes, classifies files against markdown rules (YAML `paths` globs, no redeploy), runs **one agent per ruleset** (exceptional handling, formatting, Javadoc, POM security, and friends), then a summarizer reads the pile and tells you whether to merge, nitpick, or walk away. Local Ollama by default. OpenRouter when you want a second opinion that bills by the syllable anyway.

We dogfood it. We wrote prompt discipline about `+` lines and `-` lines because we had to. The `java-exceptional` agent enforces the same ideas we preach in [exceptional-java](https://github.com/sdempsay/exceptional-java) — failures as responses, not tourism through `throws` clauses.

---

## What Is This?

A CLI review pipeline:

```
Ingest → Classify → Review → Summarize → (optional) Chat
```

Instead of:

```text
"Review my PR" + entire diff + vibes + hope
```

You get:

```text
java-exceptional agent  → diff + exceptional rules only
java-formatting agent   → diff + formatting rules only
java-javadoc agent      → diff + javadoc rules only
summarize agent         → findings → health score + APPROVE / REQUEST_CHANGES
```

Rules live in markdown files. Add a rule, add a glob, done. No Java changes in the pipeline itself.

Orchestration is **workflow-style plain Java** — plan specialist tasks, run them, summarize. [langchain4j](https://docs.langchain4j.dev/) supplies `ChatModel`, streaming, and token accounting only; we do **not** use the experimental `langchain4j-agentic` module (`@Agent`, `AgenticServices`). That is intentional: predictable pipelines beat framework magic for cost-controlled review.

---

## Quick Start

### Prerequisites

Java 21+, Maven, a Git repository, and either [Ollama](https://ollama.com) or an OpenRouter API key.

### Build

```bash
git clone https://github.com/sdempsay/agentic-review-tool.git
cd agentic-review-tool
mvn package -DskipTests
```

### Configure

```bash
mkdir -p ~/.code-review
cp src/main/resources/default-config.json ~/.code-review/config.json
```

Edit `provider`, `baseUrl`, model name, and `rulesDir` as needed. For OpenRouter, set `"provider": "openrouter"` and `OPENROUTER_API_KEY` (or `model.apiKey` in config) — never commit keys.

Worth turning:

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

- **`reviewMaxTokens`** — caps generation per agent, summarize, and chat call
- **`maxAgentDiffKb`** / **`maxFilesPerAgent`** — split oversized reviews into batches
- **`rulesDir`** — bundled `rules/` is a starting set; point at your own tree when ready

### Sanity check

```bash
java -jar target/code-review.jar doctor
```

### Review your work

```bash
# Uncommitted changes
java -jar target/code-review.jar diff

# Against a base branch
java -jar target/code-review.jar diff --base origin/master --output review.md

# Full repo (tracked + untracked, gitignore-safe)
java -jar target/code-review.jar repo --output repo-review.md
```

Use `--no-chat` in CI. Use `--verbose` to watch Ollama stream its thinking in real time.

### Sample output (Ollama)

We keep a reproducible diff from this repo in [`samples/`](samples/). Commit `3bc265e` touches three Java files (diff-discipline prompting); regenerate anytime:

```bash
./samples/diff-discipline-small/reproduce.sh
```

Or read the checked-in report: [`samples/diff-discipline-small/report.md`](samples/diff-discipline-small/report.md).

Excerpt — per-agent findings and suggested follow-ups:

```markdown
### Review: java-formatting
- ReviewPromptBuilder.java:49–51 — nit — Opening brace on new line for `else` block
- ReviewPromptBuilder.java:182–183 — nit — Opening brace on new line for `else` block
- ReviewPromptBuilder.java:251–254 — nit — Opening brace on new line for `else` block

### Recommendation
APPROVE_WITH_NITS

### Top Actions
- Fix opening braces for `else` blocks in ReviewPromptBuilder.java (lines 49–51, 182–183, 251–254).
- Remove trailing whitespace after `System.lineSeparator()` in ReviewPromptBuilder.java (~line 254).
```

`java-exceptional` was clean on this diff — no phantom must-fix items on removed `throws` lines. That is the point.

---

## Why This Matters

### 1. Specialized Agents Don't Argue With Deleted Code

```diff
- public void load() throws IOException {
+ public ExceptionalResponse<Void> load() {
```

The `java-exceptional` agent reports violations on **`+` lines** only. That removed `throws` is a migration win, not a regression. We learned that the hard way so you don't have to read fourteen bullet points about `loadRequired` methods that no longer exist.

### 2. Rules Are Data, Not Code

```yaml
---
paths:
  - "**/*.java"
  - "**/*.java.ftl"
---

# Team standards

FreeMarker templates that emit Java use the `.java.ftl` suffix.
Review the Java they would emit; ignore `<#if>` — it's template syntax, not a style violation.
```

Drop a file in `rulesDir`. The pipeline classifies, prompts, and ships. Your team's standards evolve in Git, not in a vendor dashboard.

### 3. Local-First Doesn't Mean Low-Quality

We run the same diffs on Ollama and OpenRouter and compare token usage per call. Tight prompts and output caps (`reviewMaxTokens`, default 4096) keep local models from narrating every file in a forty-file change until the heat death of the universe.

### 4. You Get Receipts

Every run can export Markdown: ingest scope, classification, per-agent findings, health score, recommendation, top actions, and token counts. "LGTM" is not a valid output format. **"Clean: all 47 files in scope"** is also not a valid output format. We fixed that too.

---

## The Trade

Yes, using multiple agents means more moving parts than one chat box.

You're saying: *"I'll trade one omniscient oracle for several opinionated specialists who read less context each."*

That's a feature, not a bug. You're trading prompt bloat and mystery findings for **scoped prompts, explicit rules, and output you can sanity-check**.

Local models still need discipline. Frontier models still cost money. This pipeline doesn't pretend otherwise — it gives you knobs, reports, and rules you own.

*(Combined Java agents and smarter batching are on the horizon. For now, clarity beats cleverness.)*

---

## The Philosophy

Not every line in a diff needs a lecture. Most review noise comes from asking one model to be your formatter, your architect, your security auditor, and your therapist at the same time.

Agentic review makes scope explicit. It makes failure modes visible. It makes your standards readable.

**Now go merge something with receipts.**

---

For more detail, see [WhyBeExceptional.md](https://github.com/sdempsay/exceptional-java/blob/master/WhyBeExceptional.md) — why the `java-exceptional` agent cares about your error handling — and `PRD.md` / `AGENTS.md` in this repo.

Licensed under the [MIT License](LICENSE).

```bash
mvn verify   # checkstyle included; no excuses
```