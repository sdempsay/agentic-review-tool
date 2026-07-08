# ACTIONS.md

## 2026-07-08

- Added `reviewMaxTokens` config (default 4096): caps per-call LLM output for agent review, summarize, and chat; batch budgeting reserves `resolvedReviewMaxTokens()` instead of full `maxTokens`; bundled `maxTokens` 24000 unchanged for backward-compat fallback
- Added `**/*.java.ftl` to java-exceptional/formatting/javadoc rules with project-agnostic FreeMarker preambles (review emitted Java, ignore FTL directives); classifier tests; aether already uses `Builder.java.ftl` / `validation.java.ftl`
- Tightened `java-javadoc.md` diff discipline: only new public API on `+` lines; private/test/listener overloads out of scope; Ollama origin/master v3 — all agents Clean, 10/10 APPROVE (~52s), matches OpenRouter
- Tightened `java-exceptional.md` diff discipline: flag only `+` lines that introduce violations; never flag removed `throws`/`*Required` lines; removals are correct migrations
- Tightened summarize for retracted agent bullets: `AgentFindingsSanitizer` collapses re-evaluated/`invalid` output ending in `## Clean` before summarize; diff summarization rules in `SummarizePromptBuilder`; Ollama v3 rerun — all agents Clean, summarize 10/10 APPROVE, Top Actions none
- Tightened `java-formatting.md` diff discipline: flag only `+` lines, never context/`else` openers; omit non-`+` findings; Ollama rerun on same 3-file diff (~10s) — exceptional/javadoc Clean; formatting agent self-corrected context-line nits (v1 brace false positives gone) but still emitted then retracted bullets
- Added diff review discipline prompting: `review-output-format.md` section (`+`/`-` line rules, cite only added lines, exceptional carve-outs); `ReviewPromptBuilder` diff-mode intro + ` ```diff ` fences; unit tests updated; Ollama dogfood on 3-file uncommitted diff completed in ~106s (java-exceptional Clean, no loop)
- Removed explicit `throw` from git/health exceptional chains: `GitRunner.run` passes listener into `supply`; `GitIngestService` threads listener into git runs; `OllamaModelInspector` uses HTTP `.chain()` + `ExceptionalSupport.fail`; `ModelHealthChecker` resolves OpenRouter API key via `ExceptionalSupport.fail` instead of `requireApiKey`
- Threaded `ExceptionalListener` through chain callbacks: supplements loaders, `LlmReviewService`/`LlmSummarizeService`, `ReviewChatLoop`, `GitIngestService`/`RepoIngestService`, `GitRunner`, `ModelHealthChecker`, `RulesEngine`, `ConfigLoader`; CLI passes `failures.listener()` and stage listeners into ingest/review/chat
- Fixed OpenRouter-flagged throws: `ReportExporter` null-path validation via `ExceptionalSupport.fail`; `DiffCommand`/`RepoCommand` chat chain passes `reviewListener` into `ReviewChatLoop.run`
- Removed all production `ExceptionalSupport.response()` bridges: `ConfigLoader`, `RepoIngestService`, `RulesEngine`, `ModelHealthChecker`, `ReviewGuardrailsLoader`, `ReviewOutputFormatLoader`, `PromptBudgetEstimator`, `RulesetReviewPlanner`, `ReviewChatLoop`/`ReviewChatOrchestrator`; supplements flow via `.chain()` into batch planning and chat
- Default Ollama model `qwen3-coder-next-256k:latest` (replaced `qwen3.6-35b-mlx-256k`); bundled `maxTokens` 24000

## 2026-07-07

- Tightened review output rules: findings first; `## Clean` only when zero findings; do not enumerate every clean file in large batches (`review-output-format.md` + java rules)
- Diff ingest skips `.md` and `.json` by default (same as repo); shared `IngestExtensionFilter` with `RepoPathFilter`
- Generalized `java-exceptional.md`: removed repo-specific types (`IngestRequest`, `ExceptionalSupport`); examples use `ExceptionalSupplier`, `listener.onError`, and generic `OrderRequest` record
- Added record compact-constructor carve-out to `java-exceptional.md` and `AGENTS.md` (invariant `throw` in compact constructors is out of scope; not exceptional lambdas)
- Fixed summarize pipeline failure path: `LlmReviewService` chains `LlmSummarizeService` (no `ExceptionalSupport.response()` bridge); `ReviewPhase` carries `changedFiles` into summarize; summarize errors return `wasError()` instead of crashing with `AssertionError`
- Tightened `java-exceptional.md`: explicit `throw` inside `.chain()` / `supply()` lambdas is must-fix; use `ExceptionalSupport.fail`; `ExceptionalSupport.response()` is tests-only; updated `AGENTS.md` to match
- Replaced `throw` in exceptional lambdas with `ExceptionalSupport.fail` in `GitIngestService`, `GitRunner`, `RepoIngestService`
- Refactored `GitIngestService` to compose git/file I/O via `.chain()` and `ExceptionalResource` (no `ExceptionalSupport.response()` bridges)
- Fixed diff-review nits: EOF newlines on 7 Java files; extracted `final String content` in `GitIngestService.ingestUntrackedFile`
- Refactored I/O and service layers to exceptional contract: removed public `*Required` methods and `throws` declarations; `GitRunner` returns `ExceptionalResponse`; `ReviewChatLoop.run` returns `ExceptionalResponse<Void>`; CLI chat wired via `.chain()`
- Fixed `pom-security.md` path globs: Java `**/pom.xml` does not match root `pom.xml`; added explicit `pom.xml` glob
- Added TODO 28 (low priority): MCP service for Maven dependency CVE lookup
- Added `pom-security.md` review rule for `pom.xml` (curated version floors, parent policy; not a live CVE scanner)
- Added `rules/guardrails/*.md` with `ReviewGuardrailsLoader`; wired into review, summarize, and chat prompts via `ReviewPromptSupplements`
- Added `review-output-format.md` (rules dir override + bundled); `ReviewOutputFormatLoader` appends to ruleset/general review prompts
- Split local review rules: `java-formatting.md`, `java-exceptional.md`, `java-javadoc.md`; removed `java-general.md`; updated bundled resources and tests
- Strengthened local `java-general.md` §9 Java 21 conventions (switch expressions, var, sealed types)
- Expanded local `java-general.md` §8 with equals/hashCode, enum switch, utility-class checks
- Scoped `java-general.md` §7 Javadoc to new public API in the diff
- Generalized `java-general.md` §3 internal imports for any project root package
- Trimmed local `java-general.md`: removed §1 whitespace meta-line, §2 examples, §6 link; §4 Java 21 wording
- Added `java-general.md` §10 severity tiers (must-fix vs nit) to local review rules
- Added diff vs full-file scope guidance to local `java-general.md` opener
- Added `java-general.md` §10 response format to local review rules (finding bullets, Clean list, insufficient context)
- Split `java-general.md` audiences: local `rules/` = review pipeline (findings, trimmed §8); `~/.grok/rules` = coding agents (apply when editing, §8 checkstyle URL, §6 imperative)
- Removed Java 8 opt-out from `java-general.md` §9 (local `rules/` + `~/.grok/rules`); Java 21 only
- Trimmed local `java-general.md` §8 for review pipeline: dropped XML URL and parent-POM note; kept actionable checkstyle-aligned bullets only
- Updated `java-general.md` §8 Checkstyle URL to `sdempsay/java-checkstyle` `checkstyle-java21.xml` on GitHub (`~/.grok/rules` only)
- Aligned `AGENTS.md` exceptional handling with `java-general.md` §6: no `throws`/re-throw, `ExceptionalSupplier`/`ExceptionalResource` shape, boundary exits without propagating throws
- Corrected local `java-general.md` §6: no `throws`/re-throw — failures as `ExceptionalResponse` via `ExceptionalSupplier`/`ExceptionalResource`; synced `rules/` and bundled resources only (not `~/.grok/rules`)
- Pointed `AGENTS.md` at `~/.grok/rules/maven.md`; removed local `rules/maven.md` (not a review rule)
- Removed `pom-tidy` review rule; `mvn tidy:pom` is a build step, not LLM review
- Copied `~/.grok/rules` into repo `rules/` (java-general, xml-formatter, maven reference); default `rulesDir` is `rules`; removed legacy java-formatting rule
- Implemented task 19 OpenRouter provider via `langchain4j-open-ai`; `model.apiKey` / `OPENROUTER_API_KEY`; `ModelHealthChecker` for `/models`; `LlmTokenLedger` token usage per call and in review report (Ollama + OpenRouter)
- Removed TODO items 21 (auto-fix) and 22 (GitLab MCP); custom GitLab MCP not in scope for this tracker
- Removed `.github/workflows/ci.yml` (GitHub Actions); no in-repo CI — use `mvn verify` locally
- Implemented task 20e `repoExcludeExtensions` config: `AppConfig` + `ConfigLoader`; merged into `RepoIngestRequest` default deny list with `.md`/`.json` and `--exclude-ext`; updated `PRD.md` §3 and repo config table
- Implemented task 20d repo summarize: `RepoHotspotAnalyzer` directory coverage stats; `SummarizePromptBuilder` FULL_FILE mode adds repository coverage, Hotspot Areas, and Cross-Cutting Findings sections; fixture tests under `src/test/resources/fixtures/repo-summarize/`
- Implemented task 20c `RepoCommand` end-to-end CLI: aligned orchestration with `DiffCommand` (dry-run shows classification only; full review default); `RepoScopeDescriber` for report scope with `--path` / `--include-ext` / `--exclude-ext`; `RepoCommandTest` subprocess E2E for dry-run report export
- Implemented task 20b full-file review prompts: `ReviewContentMode`, fenced file content in prompts, planner batch caps by mode; `LlmReviewService` + `RepoCommand --dry-run=false` run repo LLM review
- Implemented task 20a repo ingest: `RepoIngestService`, `RepoPathFilter`, `RepoIngestRequest`, `code-review repo` (classification-only); tracked + untracked via `git ls-files --others --exclude-standard`; default `.md`/`.json` exclusions; `--path` / `--include-ext` / `--exclude-ext`
- Documented Phase 2 full repository review (task 20) in `PRD-updated.md`: `code-review repo`, tracked + untracked ingest via `git ls-files --others --exclude-standard`, default exclusions for `.md`/`.json`; split into TODO 20a–20e
- Added project `AGENTS.md` with mandatory exceptional error-handling rules, reference classes, and try/catch allowlist
- Implemented soft/hard batch caps: `maxAgentDiffKb` soft target, Ollama `num_ctx` hard limit via `OllamaModelInspector`; `AgentBatchLimits`, `PromptBudgetEstimator`; context warnings in `ReviewProgress` and `doctor`
- Refactored `OllamaModelInspector` to exceptional pattern (`fetchContextTokens` / `resolveContextTokens` with `wasError()` fallback)
- Implemented ruleset batch splitting (task 27): `maxAgentDiffKb` / `maxFilesPerAgent` in config; `RulesetBatchSplitter` splits agent calls with batch labels
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