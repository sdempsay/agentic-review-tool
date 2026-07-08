# Code Review Report

**Generated:** 2026-07-08T00:33:58.413215Z
**Scope:** changes against origin/master

## Ingest

- Files changed: 23
- Reviewable diffs: 23
- Skipped: 0

- `ACTIONS.md` [MODIFIED, 1956 bytes]
- `AGENTS.md` [MODIFIED, 3510 bytes]
- `rules/java-exceptional.md` [MODIFIED, 5585 bytes]
- `src/main/java/org/dempsay/codereview/cli/DiffCommand.java` [MODIFIED, 1176 bytes]
- `src/main/java/org/dempsay/codereview/cli/RepoCommand.java` [MODIFIED, 1176 bytes]
- `src/main/java/org/dempsay/codereview/cli/ReportExporter.java` [MODIFIED, 1629 bytes]
- `src/main/java/org/dempsay/codereview/cli/ReviewChatLoop.java` [MODIFIED, 2462 bytes]
- `src/main/java/org/dempsay/codereview/config/ConfigLoader.java` [MODIFIED, 2466 bytes]
- `src/main/java/org/dempsay/codereview/ingest/GitChangeLister.java` [MODIFIED, 1250 bytes]
- `src/main/java/org/dempsay/codereview/ingest/GitIngestService.java` [MODIFIED, 10467 bytes]
- `src/main/java/org/dempsay/codereview/ingest/GitRunner.java` [MODIFIED, 4987 bytes]
- `src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java` [MODIFIED, 3432 bytes]
- `src/main/java/org/dempsay/codereview/model/ModelHealthChecker.java` [MODIFIED, 4036 bytes]
- `src/main/java/org/dempsay/codereview/model/OllamaModelInspector.java` [MODIFIED, 2617 bytes]
- `src/main/java/org/dempsay/codereview/review/LlmReviewService.java` [MODIFIED, 5257 bytes]
- `src/main/java/org/dempsay/codereview/review/LlmSummarizeService.java` [MODIFIED, 2434 bytes]
- `src/main/java/org/dempsay/codereview/review/ReviewGuardrailsLoader.java` [MODIFIED, 5669 bytes]
- `src/main/java/org/dempsay/codereview/review/ReviewOutputFormatLoader.java` [MODIFIED, 2467 bytes]
- `src/main/java/org/dempsay/codereview/review/ReviewPromptSupplements.java` [MODIFIED, 1544 bytes]
- `src/main/java/org/dempsay/codereview/rules/RulesEngine.java` [MODIFIED, 6459 bytes]
- `src/main/java/org/dempsay/codereview/support/ExceptionalSupport.java` [MODIFIED, 1866 bytes]
- `src/main/resources/rules/java-exceptional.md` [MODIFIED, 5661 bytes]
- `src/test/java/org/dempsay/codereview/review/ReviewPromptBuilderTest.java` [MODIFIED, 1820 bytes]

## Classification

### `ACTIONS.md`
- (no matching rules)

### `AGENTS.md`
- (no matching rules)

### `rules/java-exceptional.md`
- (no matching rules)

### `src/main/java/org/dempsay/codereview/cli/DiffCommand.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/cli/RepoCommand.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/cli/ReportExporter.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/cli/ReviewChatLoop.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/config/ConfigLoader.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/ingest/GitChangeLister.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/ingest/GitIngestService.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/ingest/GitRunner.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/model/ModelHealthChecker.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/model/OllamaModelInspector.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/review/LlmReviewService.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/review/LlmSummarizeService.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/review/ReviewGuardrailsLoader.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/review/ReviewOutputFormatLoader.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/review/ReviewPromptSupplements.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/rules/RulesEngine.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/java/org/dempsay/codereview/support/ExceptionalSupport.java`
- java-exceptional
- java-formatting
- java-javadoc

### `src/main/resources/rules/java-exceptional.md`
- (no matching rules)

### `src/test/java/org/dempsay/codereview/review/ReviewPromptBuilderTest.java`
- java-exceptional
- java-formatting
- java-javadoc

## Review

### Agent Reviews
### Review: java-exceptional
null

### Review: java-formatting
null

### Review: java-javadoc
**Clean**
- `src/main/java/org/dempsay/codereview/cli/DiffCommand.java`
- `src/main/java/org/dempsay/codereview/cli/RepoCommand.java`
- `src/main/java/org/dempsay/codereview/cli/ReportExporter.java`
- `src/main/java/org/dempsay/codereview/cli/ReviewChatLoop.java`
- `src/main/java/org/dempsay/codereview/config/ConfigLoader.java`
- `src/main/java/org/dempsay/codereview/ingest/GitChangeLister.java`
- `src/main/java/org/dempsay/codereview/ingest/GitIngestService.java`
- `src/main/java/org/dempsay/codereview/ingest/GitRunner.java`
- `src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java`
- `src/main/java/org/dempsay/codereview/model/ModelHealthChecker.java`
- `src/main/java/org/dempsay/codereview/model/OllamaModelInspector.java`
- `src/main/java/org/dempsay/codereview/review/LlmReviewService.java`
- `src/main/java/org/dempsay/codereview/review/LlmSummarizeService.java`
- `src/main/java/org/dempsay/codereview/review/ReviewGuardrailsLoader.java`
- `src/main/java/org/dempsay/codereview/review/ReviewOutputFormatLoader.java`
- `src/main/java/org/dempsay/codereview/review/ReviewPromptSupplements.java`
- `src/main/java/org/dempsay/codereview/rules/RulesEngine.java`
- `src/main/java/org/dempsay/codereview/support/ExceptionalSupport.java`
- `src/test/java/org/dempsay/codereview/review/ReviewPromptBuilderTest.java`

### Review: general
AGENTS.md:35 — [must-fix] — Example uses `ExceptionalSupport.fail(listener, error)` but `rules/java-exceptional.md` describes using `listener.onError(error); return ExceptionalResponse.failure();`. Align documentation on the correct API usage; if `fail` is a helper, document it in `rules/java-exceptional.md`, otherwise update the example.
rules/java-exceptional.md:75 — [nit] — Reference to "§1 violation" may be stale; verify section numbering matches current file structure and the reference resolves correctly.
ACTIONS.md:6 — [nit] — Phrasing "no `ExceptionalSupport.response()` bridge" is ambiguous; clarify if the bridge was removed or simply not used in the fix.

### Summary
### Health Score
9/10

### Recommendation
REQUEST_CHANGES

### Summary
The codebase exhibits high quality with clean Java exception handling, formatting, and Javadoc across all 23 reviewed files. Documentation requires attention due to a critical API usage mismatch in `AGENTS.md` that must be aligned with the actual implementation or helper documentation. Minor nits regarding stale section references and ambiguous phrasing should also be resolved to maintain clarity.

### Top Actions
*   **Fix API Alignment:** Update `AGENTS.md` example to use `listener.onError(error); return ExceptionalResponse.failure();` or document `ExceptionalSupport.fail` as a valid helper in `rules/java-exceptional.md`.
*   **Verify Section References:** Check and update the "§1 violation" reference in `rules/java-exceptional.md` to ensure it matches current file structure.
*   **Clarify Phrasing:** Refine the ambiguous phrasing regarding the `ExceptionalSupport.response()` bridge in `ACTIONS.md`.

--- Token Usage ---
Provider: ollama
Model: qwen3.6-35b-mlx-256k:latest
59108 input, 28307 output, 87415 total

Per call:
- java-exceptional: 18597 in / 8000 out / 26597 total
- java-formatting: 17920 in / 8000 out / 25920 total
- java-javadoc: 17305 in / 6151 out / 23456 total
- general: 4426 in / 4774 out / 9200 total
- summarize: 860 in / 1382 out / 2242 total
