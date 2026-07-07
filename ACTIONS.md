# ACTIONS.md

## 2026-07-07

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