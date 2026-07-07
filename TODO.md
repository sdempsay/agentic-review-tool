# TODO.md

Task tracker for the Code Review Pipeline MVP (`PRD.md`).

## Pipeline Tasks

| ID | Task | Status | Notes |
|----|------|--------|-------|
| 1 | Project scaffold (`pom.xml`, shade JAR, `CodeReviewApplication`) | complete | langchain4j 1.17.2, picocli, Jackson, SnakeYAML |
| 2 | CI workflow (build + test + checkstyle) | complete | `.github/workflows/ci.yml` |
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
| 15 | **Summarize** — aggregate findings, health score, recommendation | pending | **Next task** — fourth pipeline stage |
| 16 | Interactive terminal results viewer | pending | MVP CLI output enhancement |
| 17 | Markdown report export (`--output`) | pending | MVP deliverable |
| 18 | Create and maintain `ACTIONS.md` work log | complete | Created 2026-07-07 |

## Future / Out of Scope (MVP)

| ID | Task | Status | Notes |
|----|------|--------|-------|
| 19 | OpenRouter model provider | pending | Mentioned in PRD overview; not in MVP config example |
| 20 | Full repository review | pending | Phase 2 |
| 21 | Auto-fix suggestions | pending | Out of scope for MVP |
| 22 | GitLab MCP comment posting | pending | Out of scope for MVP |
| 23 | Advanced RAG over codebase | pending | Out of scope for MVP |
| 24 | Classification beyond path globs | pending | Content analysis, commit messages, etc. |
| 25 | Digitally signed prompts | pending | Security/auditability future item |
| 26 | Enhanced CLI — live progress, per-agent/file status, streaming thinking | pending | Post-MVP; see PRD §5 |