# Code Review Report

**Generated:** 2026-07-08T00:25:19.060123Z
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
Looking at the diff, I can see extensive refactoring to convert methods from throwing exceptions to returning `ExceptionalResponse<T>`. Let me review each file for compliance with the java-exceptional ruleset.

## Findings

**Clean**
- src/main/java/org/dempsay/codereview/cli/DiffCommand.java
- src/main/java/org/dempsay/codereview/cli/RepoCommand.java
- src/main/java/org/dempsay/codereview/cli/ReportExporter.java
- src/main/java/org/dempsay/codereview/cli/ReviewChatLoop.java
- src/main/java/org/dempsay/codereview/ingest/GitChangeLister.java
- src/main/java/org/dempsay/codereview/ingest/GitIngestService.java
- src/main/java/org/dempsay/codereview/ingest/GitRunner.java
- src/main/java/org/dempsay/codereview/model/ModelHealthChecker.java
- src/main/java/org/dempsay/codereview/model/OllamaModelInspector.java
- src/main/java/org/dempsay/codereview/review/LlmReviewService.java
- src/main/java/org/dempsay/codereview/review/LlmSummarizeService.java
- src/main/java/org/dempsay/codereview/review/ReviewGuardrailsLoader.java
- src/main/java/org/dempsay/codereview/review/ReviewOutputFormatLoader.java
- src/main/java/org/dempsay/codereview/review/ReviewPromptSupplements.java
- src/main/java/org/dempsay/codereview/rules/RulesEngine.java
- src/main/java/org/dempsay/codereview/support/ExceptionalSupport.java

• src/main/java/org/dempsay/codereview/config/ConfigLoader.java:27 — must-fix — §1 — unwrapping ExceptionalResponse without checking wasError() in production code

• src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java:38 — must-fix — §1 — unwrapping ExceptionalResponse without checking wasError() in production code

• src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java:39 — must-fix — §1 — unwrapping ExceptionalResponse without checking wasError() in production code

• src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java:62 — must-fix — §1 — unwrapping ExceptionalResponse without checking wasError() in production code

The violations are all instances of `ExceptionalSupport.response()` being used to unwrap `ExceptionalResponse` objects without checking `wasError()` first. According to the ruleset, this pattern is only allowed in tests, but these are in production code and should use `.chain()` to propagate the exceptional response instead.

### Review: java-formatting
Looking at the provided diffs, I'll review them against the Java formatting and style rules.

## Findings

- `src/main/java/org/dempsay/codereview/cli/ReportExporter.java:25` — **nit** — §1 — File does not end with an empty newline
- `src/main/java/org/dempsay/codereview/ingest/GitChangeLister.java:17` — **nit** — §1 — File does not end with an empty newline
- `src/main/java/org/dempsay/codereview/review/LlmReviewService.java:157` — **nit** — §1 — File does not end with an empty newline
- `src/main/java/org/dempsay/codereview/review/LlmSummarizeService.java:49` — **nit** — §1 — File does not end with an empty newline
- `src/main/java/org/dempsay/codereview/review/ReviewGuardrailsLoader.java:113` — **nit** — §1 — File does not end with an empty newline
- `src/main/java/org/dempsay/codereview/review/ReviewOutputFormatLoader.java:48` — **nit** — §1 — File does not end with an empty newline
- `src/main/java/org/dempsay/codereview/review/ReviewPromptSupplements.java:16` — **nit** — §1 — File does not end with an empty newline
- `src/main/java/org/dempsay/codereview/rules/RulesEngine.java:127` — **nit** — §1 — File does not end with an empty newline

## Clean

- `src/main/java/org/dempsay/codereview/cli/DiffCommand.java`
- `src/main/java/org/dempsay/codereview/cli/RepoCommand.java`
- `src/main/java/org/dempsay/codereview/cli/ReviewChatLoop.java`
- `src/main/java/org/dempsay/codereview/config/ConfigLoader.java`
- `src/main/java/org/dempsay/codereview/ingest/GitIngestService.java`
- `src/main/java/org/dempsay/codereview/ingest/GitRunner.java`
- `src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java`
- `src/main/java/org/dempsay/codereview/model/ModelHealthChecker.java`
- `src/main/java/org/dempsay/codereview/model/OllamaModelInspector.java`
- `src/main/java/org/dempsay/codereview/support/ExceptionalSupport.java`
- `src/test/java/org/dempsay/codereview/review/ReviewPromptBuilderTest.java`

### Review: java-javadoc
## Clean

- src/main/java/org/dempsay/codereview/cli/DiffCommand.java
- src/main/java/org/dempsay/codereview/cli/RepoCommand.java
- src/main/java/org/dempsay/codereview/cli/ReportExporter.java
- src/main/java/org/dempsay/codereview/cli/ReviewChatLoop.java
- src/main/java/org/dempsay/codereview/config/ConfigLoader.java
- src/main/java/org/dempsay/codereview/ingest/GitChangeLister.java
- src/main/java/org/dempsay/codereview/ingest/GitIngestService.java
- src/main/java/org/dempsay/codereview/ingest/GitRunner.java
- src/main/java/org/dempsay/codereview/ingest/RepoIngestService.java
- src/main/java/org/dempsay/codereview/model/ModelHealthChecker.java
- src/main/java/org/dempsay/codereview/model/OllamaModelInspector.java
- src/main/java/org/dempsay/codereview/review/LlmReviewService.java
- src/main/java/org/dempsay/codereview/review/LlmSummarizeService.java
- src/main/java/org/dempsay/codereview/review/ReviewGuardrailsLoader.java
- src/main/java/org/dempsay/codereview/review/ReviewOutputFormatLoader.java
- src/main/java/org/dempsay/codereview/review/ReviewPromptSupplements.java
- src/main/java/org/dempsay/codereview/rules/RulesEngine.java
- src/main/java/org/dempsay/codereview/support/ExceptionalSupport.java
- src/test/java/org/dempsay/codereview/review/ReviewPromptBuilderTest.java

The changes in these files are refactoring existing code to use a different exception handling pattern (ExceptionalResponse) rather than introducing new public API. No new public types or methods are being added that would require Javadoc documentation.

### Review: general
Looking at these documentation updates for Java exceptional error handling patterns:

## Clean

- ACTIONS.md
- AGENTS.md  
- rules/java-exceptional.md
- src/main/resources/rules/java-exceptional.md

The changes appear to be comprehensive documentation updates that:

1. **Generalize examples** - Remove repo-specific types and use more generic examples like `OrderRequest` and `ExceptionalSupplier`

2. **Add important clarifications** - Explicitly carve out record compact constructors as allowed to use `throw` for invariant validation, which is a reasonable distinction between construction-time validation vs operational error handling

3. **Tighten restrictions** - Make it clear that explicit `throw` statements inside `.chain()` callbacks and `ExceptionalSupplier` lambdas are forbidden, requiring use of `listener.onError()` + `ExceptionalResponse.failure()` instead

4. **Provide clear examples** - Show both "BAD" and "GOOD" patterns for common scenarios like handling external command failures

5. **Maintain consistency** - The same content appears in both `rules/java-exceptional.md` and `src/main/resources/rules/java-exceptional.md`, suggesting proper synchronization

The documentation changes are well-structured, provide clear guidance on the exceptional library usage patterns, and include appropriate examples. The distinction between construction-time invariant validation (allowed) vs operational error handling (must use ExceptionalResponse) is particularly well articulated.

### Summary
### Health Score
7/10

### Recommendation
REQUEST_CHANGES

### Summary
This is a comprehensive refactoring to adopt an ExceptionalResponse pattern for error handling across the codebase. The changes are well-structured and include thorough documentation updates. However, there are critical violations where ExceptionalResponse objects are being unwrapped without proper error checking in production code, which defeats the purpose of the new error handling pattern.

### Top Actions
• Fix ConfigLoader.java:27 - check wasError() before unwrapping ExceptionalResponse
• Fix RepoIngestService.java:38,39,62 - use .chain() instead of direct unwrapping with ExceptionalSupport.response()
• Add missing newlines at end of 8 Java files (ReportExporter, GitChangeLister, LlmReviewService, LlmSummarizeService, ReviewGuardrailsLoader, ReviewOutputFormatLoader, ReviewPromptSupplements, RulesEngine)
• Review error propagation strategy to ensure ExceptionalResponse failures are properly handled throughout the call chain
• Consider adding integration tests to verify the new error handling pattern works end-to-end

--- Token Usage ---
Provider: openrouter
Model: anthropic/claude-sonnet-4
73222 input, 2692 output, 75914 total

Per call:
- java-exceptional: 22560 in / 776 out / 23336 total
- java-formatting: 21769 in / 747 out / 22516 total
- java-javadoc: 21081 in / 567 out / 21648 total
- general: 5105 in / 335 out / 5440 total
- summarize: 2707 in / 267 out / 2974 total
