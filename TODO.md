# TODO.md

Task tracker for the Code Review Pipeline MVP (`PRD.md`).

**Backlog sync:** Pending work is tracked on [GitHub Issues](https://github.com/sdempsay/agentic-review-tool/issues). This file is a thin index (ID, status, issue link) for offline agent session start. Details and discussion live on the issue. When shipping: `Fixes #N` in PR → close issue → mark row `complete` here.

## Pipeline Tasks

| ID | Task | Status | Notes |
|----|------|--------|-------|
| 1 | Project scaffold (`pom.xml`, shade JAR, `CodeReviewApplication`) | complete | langchain4j 1.17.2, picocli, Jackson, SnakeYAML |
| 2 | CI workflow (build + test + checkstyle) | cancelled | Removed; verify locally with `mvn verify` |
| 3 | CLI root command (`code-review`) | complete | Subcommands: `diff`, `doctor` |
| 4 | Configuration loading (`config.json`, bundled defaults) | complete | `~/.code-review/config.json` fallback |
| 5 | **Ingest** — git diff ingestion | complete | Staged, uncommitted, `--base` ref; size limits |
| 6 | **Classify** — rules engine with YAML frontmatter path globs | complete | `RulesEngine`, `RulesClassifier`, `--dry-run` |
| 7 | Java rule files (`java-general`, `java-formatting`) | complete | Bundled + `rules/` directory copies |
| 8 | `doctor` command — config + LLM connectivity check | complete | Ollama health probe |
| 9 | Ollama model provider | complete | `ChatModelFactory`, `ModelHealthChecker` |
| 10 | **Review** — basic LLM call with generic prompt | complete | Does not yet use classified rules |
| 11 | Unit tests for ingest, classify, config, review | complete | `mvn verify` passes |
| 12 | Commit initial implementation to git | complete | Initial MVP scaffold committed 2026-07-07 |
| 13 | **Review** — wire classified rules into LLM prompt | complete | Loads `~/.grok/rules`, classifies, injects into prompt |
| 14 | **Review** — per-ruleset specialized sub-agents | complete | One LLM call per matched ruleset + general fallback |
| 15 | **Summarize** — aggregate findings, health score, recommendation | complete | Summarizer agent: health score, recommendation, top actions |
| 16 | Interactive terminal — orchestrator chat with ruleset agent delegation | complete | `--chat` / `--no-chat`; delegates to ruleset agents when file referenced |
| 17 | Markdown report export (`--output`) | complete | Ingest + classification + review; works with `--dry-run` |
| 18 | Create and maintain `ACTIONS.md` work log | complete | Created 2026-07-07 |

## Future / Out of Scope (MVP)

| ID | Task | Status | Issue |
|----|------|--------|-------|
| 19 | OpenRouter model provider + token usage reporting | complete | — |
| 20 | Full repository review (`code-review repo`) | complete | — |
| 20a | **Repo ingest** — `RepoIngestService`, tracked + untracked | complete | — |
| 20b | **Review** — full-file `ReviewPromptBuilder` mode | complete | — |
| 20c | **CLI** — `RepoCommand` end-to-end | complete | — |
| 20d | **Summarize** — repo-level health, hotspots | complete | — |
| 20e | **Config** — `repoExcludeExtensions` | complete | — |
| 23 | Advanced RAG over codebase | pending | — |
| 24 | Classification beyond path globs | pending | — |
| 25 | Digitally signed prompts | pending | — |
| 26 | Enhanced CLI — live progress, streaming thinking | complete | — |
| 27 | Ruleset batch splitting (`maxAgentDiffKb` / `maxFilesPerAgent`) | complete | — |
| 28 | MCP service — Maven dependency CVE lookup | pending | — |
| 29 | Parallel ruleset LLM calls (bounded executor) | pending | [#3](https://github.com/sdempsay/agentic-review-tool/issues/3) (blocked by #1) |
| 30 | Doc alignment — workflow orchestration, not langchain4j-agentic | complete | — |
| 31 | Rule frontmatter `description` for agent metadata | pending | [#4](https://github.com/sdempsay/agentic-review-tool/issues/4) |
| 32 | Hard output guardrails (Java validators + one retry) | pending | [#1](https://github.com/sdempsay/agentic-review-tool/issues/1) |
| 33 | Document guardrail layers in AGENTS.md | pending | [#2](https://github.com/sdempsay/agentic-review-tool/issues/2) |
| 35 | MCP stdio server for code review (Grok/Claude/OpenCode) | pending | [#5](https://github.com/sdempsay/agentic-review-tool/issues/5) (after #1; host resolves findings) |

**Suggested order:** 32 → 33 → 29 → 31 → 35