# TODO.md

Task tracker for the Code Review Pipeline MVP (`PRD.md`).

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

| ID | Task | Status | Notes |
|----|------|--------|-------|
| 19 | OpenRouter model provider + token usage reporting | complete | `langchain4j-open-ai`; `LlmTokenLedger`; per-call + total tokens in report |
| 20 | Full repository review (`code-review repo`) | complete | Phase 2 slices 20a–20e |
| 20a | **Repo ingest** — `RepoIngestService`, tracked + untracked via `git ls-files` / `--others --exclude-standard`, default ext filter | complete | `code-review repo`; `RepoPathFilter`; default skip `.md`/`.json` |
| 20b | **Review** — full-file `ReviewPromptBuilder` mode + wire `LlmReviewService` | complete | `ReviewContentMode.FULL_FILE`; repo review via `--dry-run=false` |
| 20c | **CLI** — `RepoCommand` end-to-end, `--path` / `--exclude-ext`, report export | complete | Mirrors `DiffCommand`; default full review; `RepoScopeDescriber`; CLI tests |
| 20d | **Summarize** — repo-level health, hotspots, cross-cutting findings | complete | `RepoHotspotAnalyzer`; repo summarize prompt + fixture tests |
| 20e | **Config** — `repoExcludeExtensions` in `config.json`; update `PRD.md` §3 | complete | `AppConfig` + `RepoIngestRequest`; merged with `--exclude-ext` |
| 23 | Advanced RAG over codebase | pending | Out of scope for MVP |
| 24 | Classification beyond path globs | pending | Content analysis, commit messages, etc. |
| 25 | Digitally signed prompts | pending | Security/auditability future item |
| 26 | Enhanced CLI — live progress, per-agent/file status, streaming thinking | complete | `--quiet` / `--verbose`; stderr progress + Ollama streaming |
| 27 | Ruleset batch splitting with configurable `maxAgentDiffKb` / `maxFilesPerAgent` | complete | Soft cap (`maxAgentDiffKb`) + hard cap (Ollama `num_ctx`); `OllamaModelInspector`, `AgentBatchLimits` |
| 28 | MCP service — Maven dependency CVE lookup | pending | **Low priority.** Accept GAV coordinates (or `pom.xml` deps), return known CVEs; complements `pom-security.md` curated floors |
| 29 | Parallel ruleset LLM calls (bounded executor) | pending | Independent specialists in `LlmReviewService.runAgentReviews()`; biggest wall-clock win on multi-ruleset diffs; aligns with langchain4j parallel workflow pattern |
| 30 | Doc alignment — workflow orchestration, not langchain4j-agentic | complete | PRD.md, README.md, AGENTS.md: langchain4j = ChatModel/streaming/tokens only; custom Java workflow orchestration |
| 31 | Rule frontmatter `description` for agent metadata | pending | Optional YAML `description` per rule; `FrontmatterParser` + `ReviewPromptBuilder`; mirrors `@Agent("…")` from langchain4j tutorial |