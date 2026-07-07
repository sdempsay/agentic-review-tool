# AGENTS.md — Code Review Pipeline

Project-specific rules for agents working in this repository. Global rules in `~/.grok/AGENTS.md` also apply.

## Session start

- Read `TODO.md` for task status
- Read `PRD.md` and `PRD-updated.md` for requirements
- Read `~/.grok/rules/maven.md` (this project has `pom.xml`)

## Exceptional error handling (mandatory)

This project uses the [exceptional](https://github.com/dempsay/exceptional) library (`org.dempsay.utils:exceptional`). Failures stay explicit until the CLI boundary. See `../exceptional/WhyBeExceptional.md` for rationale.

### Required pattern for I/O and external calls

Every service that can fail (network, filesystem, parsing, git, LLM) must follow the three-layer shape:

1. **`fooRequired(...)`** — throws `Exception`; contains the real logic
2. **`foo(...)`** — returns `ExceptionalResponse<T>` via `ExceptionalSupport.supply(() -> fooRequired(...))`
3. **Callers** — use `.chain()` / `.then()` with `ExceptionalListener`, or check `wasError()` explicitly

Canonical references (copy these, do not invent a new pattern):

| Layer | Example |
|-------|---------|
| Service entry | `ModelHealthChecker.check()`, `OllamaModelInspector.fetchContextTokens()`, `GitIngestService.ingest()` |
| Required impl | `ModelHealthChecker.checkRequired()`, `ConfigLoader.loadRequired()` |
| Resource I/O | `ConfigLoader` + `ExceptionalResource.of(...)` |
| CLI orchestration | `DiffCommand` chains + `FailureCapture.listener()` |
| Optional fallback | `OllamaModelInspector.resolveContextTokens()` — checks `wasError()`, returns default |

```java
// Service
public static ExceptionalResponse<HealthReport> check(final ModelConfig model) {
  return ExceptionalSupport.supply(() -> checkRequired(model));
}

public static HealthReport checkRequired(final ModelConfig model) throws Exception {
  // HTTP, parse, validate — throw on failure
}

// Graceful degradation at call site (not in catch)
public static int resolveContextTokens(final ModelConfig model) {
  final ExceptionalResponse<Integer> response = fetchContextTokens(model);
  return response.wasError() ? 0 : response.response();
}
```

### Forbidden in `src/main/java`

- **`try/catch` for business error handling** — use `ExceptionalResponse` instead
- **Swallowing exceptions** — `catch (...) { return default; }` belongs at the call site via `wasError()`, never inside a catch block
- **`catch (Exception)`** — checkstyle `IllegalCatch` rejects this; do not work around it with broad catches
- **Throwing from CLI lambdas without chaining** — wire `FailureCapture` so the user sees the root cause, not `"Operation failed"`

### Allowed exceptions (do not "fix" these)

| Location | Reason |
|----------|--------|
| `RulesEngine`, `GitRunner` | `try-with-resources` only |
| `StreamingLlmClient.awaitCompletion` | `CountDownLatch.await()` → `InterruptedException` |
| `DiffCommand.runChatIfEnabled` | `IOException` from stdin REPL |

New `try/catch` blocks require justification in `ACTIONS.md` and must be added to this table.

### Tests

- Use `ExceptionalSupport.response(...)` to unwrap expected successes in tests
- Test failure paths via `response.wasError()` when relevant

### Before committing Java changes

1. `mvn verify` (includes checkstyle)
2. Grep for new catches: `rg 'catch \(' src/main/java` — only allowlisted files above

## Task acceptance

A task is not complete until:

- `ACTIONS.md` is updated
- New requirements are captured in `PRD-updated.md` when applicable
- `mvn verify` passes
- New Java code follows the exceptional pattern above
- Changes are committed with a useful message