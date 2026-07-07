# ACTIONS.md

## 2026-07-07

- Implemented enhanced CLI progress and streaming (task 26): pipeline stage timing, per-agent status, `--quiet`/`--verbose`, `StreamingLlmClient` with Ollama streaming to stderr
- Implemented follow-up chat orchestrator (task 16): `ReviewChatLoop`, `ReviewChatOrchestrator`, file-based delegation to ruleset agents with diff + question; `--chat` / `--no-chat`
- Refined task 16 chat design: orchestrator delegates to ruleset agents with file diff + question when follow-up needs deeper analysis
- Documented follow-up chat agent requirement in `PRD.md` (task 16): interactive Q&A on review report after pipeline completes
- Added Markdown report export via `--output` (task 17): `MarkdownReportBuilder`, `ReportExporter`; includes ingest, classification, and review sections
- Implemented **Summarize** stage (task 15): `LlmSummarizeService` aggregates agent findings into health score, recommendation, summary, and top actions; `ReviewReportComposer` outputs agent reviews + summary sections
- Documented configurable ruleset batch context caps (`maxAgentDiffKb`, `maxFilesPerAgent`) in `PRD.md`; added `TODO.md` task 27
- Added Enhanced CLI Experience requirements to `PRD.md` (live progress, per-agent/file status, streaming thinking); captured in `PRD-updated.md` and `TODO.md` task 26
- Fixed rules loading failure when `~/.grok/rules/maven.md` (no YAML frontmatter) is present — skip instructional docs without `paths` globs; wire error listener into rules load for clearer CLI errors
- Implemented per-ruleset specialized review sub-agents (task 14): `RulesetReviewPlanner` groups files by rule, one LLM call per ruleset via `ReviewPromptBuilder.buildForRuleset`, general fallback for unmatched files, `ReviewAggregator` combines output
- Wired classified rules into LLM review prompt (task 13): `DiffCommand` loads rules, shows classification, `ReviewPromptBuilder` injects matched rule bodies; default `rulesDir` set to `~/.grok/rules`
- Created `TODO.md` task tracker from `PRD.md` and current implementation state
- Committed initial MVP implementation (ingest, classify, review scaffold, config, doctor, CI)
- Reviewed agent rules setup: confirmed global `~/.grok/AGENTS.md` loads via `grok inspect`; removed §6 Communication (caveman style) to avoid conflict with Cursor user rules

## Implementation (pre-commit baseline)

- Added Maven project (`pom.xml`) with langchain4j, picocli, Jackson, SnakeYAML, exceptional utils; shade JAR `code-review`
- Added CI workflow (`.github/workflows/ci.yml`) — `mvn verify` with checkstyle
- Implemented CLI: `code-review` root, `diff` and `doctor` subcommands
- Implemented configuration loading (`ConfigLoader`, `AppConfig`, `ModelConfig`) with `~/.code-review/config.json` fallback and bundled defaults
- Implemented **Ingest** stage: `GitIngestService`, `GitChangeLister`, `DiffParser` — staged, uncommitted, and `--base` ref modes with diff size limits
- Implemented **Classify** stage: `RulesEngine`, `RulesClassifier`, `FrontmatterParser`, `PathGlobMatcher`; `--dry-run` on `diff` command
- Added bundled Java rules (`java-general.md`, `java-formatting.md`) and `rules/` directory copies
- Implemented **Review** stage (partial): `LlmReviewService`, `ReviewPromptBuilder` — generic LLM prompt, rules not yet wired in
- Implemented Ollama provider: `ChatModelFactory`, `ModelHealthChecker`
- Added unit tests for ingest, classify, config, and review; all passing via `mvn verify`