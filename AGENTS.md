# AGENTS.md — Code Review Pipeline

Project-specific rules for agents working in this repository. Global rules in `~/.grok/AGENTS.md` also apply.

## Session start

- Read `TODO.md` for task status
- Read `PRD.md` and `PRD-updated.md` for requirements
- Read `~/.grok/rules/maven.md` (this project has `pom.xml`)

## Exceptional error handling (mandatory)

This project uses the [exceptional](https://github.com/dempsay/exceptional) library (`org.dempsay.utils:exceptional`). Failures are expressed as `ExceptionalResponse<T>`, not thrown exceptions. Application methods must not declare `throws` or `throw` to callers. See `../exceptional/WhyBeExceptional.md` for rationale. Review rules in `rules/java-exceptional.md` match this contract.

### Required pattern for I/O and external calls

Every service that can fail (network, filesystem, parsing, git, LLM) must follow this shape:

1. **Public methods** — return `ExceptionalResponse<T>` (never `throws`)
2. **Failing work** — wrap in `ExceptionalSupplier.of(() -> { ... }).execute()`, `ExceptionalResource.of(...)`, or `ExceptionalSupport.supply(() -> { ... })` — JDK calls that throw stay inside the lambda
3. **Callers** — `.chain()` / `.then()` with `ExceptionalListener`, or `wasError()` / `wasNoError()` at decision points

Canonical references in this repo (copy these, do not invent a new pattern):

| Layer | Example |
|-------|---------|
| Service entry | `GitIngestService.ingest()`, `ModelHealthChecker.check()`, `LlmReviewService.review()` |
| Resource I/O | `RulesEngine` — `ExceptionalResource.of(...)` for rule files |
| CLI orchestration | `DiffCommand` / `RepoCommand` — `.chain()` with `FailureCapture.listener()` |
| Optional fallback | `OllamaModelInspector.resolveContextTokens()` — `wasError()` then default |

```java
public static ExceptionalResponse<HealthReport> check(final ModelConfig model) {
  return ExceptionalSupport.supply(() -> {
    // HTTP, parse, validate — throws inside lambda are captured
    return probeHealth(model);
  });
}

public static ExceptionalResponse<HealthReport> checkAndLog(final ModelConfig model) {
  return check(model)
      .chain((listener, report) -> enrich(report), listener);
}

// Graceful degradation at call site (not inside catch)
public static int resolveContextTokens(final ModelConfig model) {
  final ExceptionalResponse<Integer> response = fetchContextTokens(model);
  return response.wasError() ? 0 : response.response();
}
```

At the **application boundary** (`DiffCommand.run()`, `RepoCommand.run()`, etc.), check the final `ExceptionalResponse`, print the captured root cause to stderr, and exit non-zero — without adding `throws` or re-throwing through the stack. Do not surface a generic `"Operation failed"` when the listener captured a real message.

Prefer `ExceptionalResource.of(() -> open(), resource -> use(resource))` over manual `try-with-resources` + catch.

### Forbidden in `src/main/java`

- **`throws` declarations** on application methods — return `ExceptionalResponse` instead
- **`throw` for expected failure paths** (I/O, network, parse errors)
- **`try/catch` for business error handling** — use `ExceptionalResponse` instead
- **Swallowing exceptions** — `catch (...) { return default; }` belongs at the call site via `wasError()`, never inside a catch block
- **`catch (Exception)`** — checkstyle `IllegalCatch` rejects this; do not work around it with broad catches

Do not add new `try/catch` blocks. Legacy catches are being removed; do not extend them.

### Tests

- Use `ExceptionalSupport.response(...)` to unwrap expected successes in tests
- Test failure paths via `response.wasError()` when relevant

### Before committing Java changes

1. `mvn verify` (includes checkstyle)
2. Grep for new throws/catches: `rg 'throws |throw new|catch \(' src/main/java`

## Task acceptance

A task is not complete until:

- `ACTIONS.md` is updated
- New requirements are captured in `PRD-updated.md` when applicable
- `mvn verify` passes
- New Java code follows the exceptional pattern above
- Changes are committed with a useful message