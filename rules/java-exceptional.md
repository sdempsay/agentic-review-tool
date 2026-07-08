---
paths:
  - "**/*.java"
---

# Java exceptional error handling

**Mandatory.** Any code path that can fail — network, filesystem, parsing, git, external services — **must** use `org.dempsay.utils:exceptional` and return `ExceptionalResponse<T>`. No `throws`, no `throw` to callers, no business `try/catch` on those paths. Violations are **must-fix**. Pure domain logic, DTOs, and records do not need exceptional unless they perform I/O. **Record compact constructors** that throw to enforce invariants are explicitly out of scope (see below).

When reviewing **diffs**, flag only added or changed lines that perform or orchestrate failing work. When reviewing **full files**, audit all I/O and external call paths.

## 1. Exception Handling (Exceptional pattern)

Every project uses exceptional for **failing** work. Methods that touch I/O or external systems must express failures as `ExceptionalResponse<T>`, not thrown exceptions — no `throws`, no `throw` to callers.

### Required shape (network, filesystem, parsing, external services)

1. **Methods that can fail** — return `ExceptionalResponse<T>` (never `throws`)
2. **Failing work** — wrap in `ExceptionalSupplier.of(() -> { ... }).execute()`, `ExceptionalResource.of(...)`, or project equivalent — JDK calls that throw stay inside the lambda
3. **Callers** — `.chain()` / `.then()` with `ExceptionalListener`, or `wasError()` / `wasNoError()` at decision points

```java
public static ExceptionalResponse<Result> load(final Path path) {
  return ExceptionalSupplier.of(() -> {
    final String raw = Files.readString(path);
    return parse(raw);
  }).execute();
}

public static ExceptionalResponse<Result> loadAndEnrich(final Path path) {
  return load(path)
      .chain((listener, result) -> enrich(result), listener);
}

// Graceful fallback at call site (not inside catch)
public static int resolveLimit(final Config config) {
  final ExceptionalResponse<Integer> response = fetchLimit(config);
  return response.wasError() ? DEFAULT_LIMIT : response.response();
}
```

At the **application boundary** (CLI `run()`, HTTP handler, job entry point), check the final `ExceptionalResponse`, surface the root cause to the user (stderr, response body, exit code), and stop — still without adding `throws` or re-throwing through the stack.

### No explicit `throw` inside exceptional lambdas

The exceptional library captures **unexpected** exceptions thrown by JDK I/O inside `ExceptionalSupplier` / `ExceptionalResource` lambdas. That is **not** permission to `throw` for **expected** business or validation failures inside those lambdas or inside `.chain()` callbacks.

**Forbidden inside `.chain()` callbacks and `ExceptionalSupport.supply()` lambdas:**

- `throw new IllegalArgumentException(...)`
- `throw new IllegalStateException(...)`
- Any other explicit `throw` for an expected failure you are handling

**Required instead:**

- In `.chain()` callbacks — `return ExceptionalSupport.fail(listener, error);`
- At method entry (no listener in scope) — `return ExceptionalSupport.fail(error);`
- Composing steps — `.chain()` from the prior `ExceptionalResponse`; never `ExceptionalSupport.response(...)` in production code

```java
// BAD — throw in chain callback
return gitRun(repoRoot, "diff")
    .chain((listener, result) -> {
      if (result.exitCode() != 0) {
        throw new IllegalStateException("git diff failed");
      }
      return ExceptionalResponse.success(parse(result));
    });

// GOOD
return gitRun(repoRoot, "diff")
    .chain((listener, result) -> {
      if (result.exitCode() != 0) {
        return ExceptionalSupport.fail(listener, new IllegalStateException("git diff failed"));
      }
      return ExceptionalResponse.success(parse(result));
    });

// BAD — response() bridge crashes the CLI on failure (AssertionError)
final Supplements s = ExceptionalSupport.response(Supplements.load(dir));

// GOOD — chain propagates wasError() to the caller
return Supplements.load(dir)
    .chain((listener, supplements) -> buildReport(supplements), listener);
```

`ExceptionalSupport.response(...)` is for **tests only** — unwrapping a response you expect to succeed.

JDK calls such as `Files.readString(path)` may throw inside a supply/resource lambda; the library captures those. Do not add a separate `throw` for the same failure path.

### Out of scope (do not report)

**Record compact constructors** — `throw new IllegalArgumentException(...)` (or similar unchecked exception) in a record's compact constructor to reject invalid field combinations is allowed. Records are immutable value types, not I/O orchestration; if the args are illegal, the object must not exist. This is invariant enforcement at construction time, not an operational failure that should flow as `ExceptionalResponse`.

```java
public record IngestRequest(Path repoRoot, DiffScope scope, int maxDiffKb) {
  public IngestRequest {
    if (repoRoot == null) {
      throw new IllegalArgumentException("repoRoot is required");
    }
    if (maxDiffKb <= 0) {
      throw new IllegalArgumentException("maxDiffKb must be positive");
    }
  }
}
```

Do **not** flag compact-constructor `throw` as a §1 violation. Do flag the same pattern inside `.chain()` callbacks or `ExceptionalSupport.supply()` lambdas.

### Report as findings

- Methods that declare `throws` instead of returning `ExceptionalResponse`
- `throw` used for expected failure paths (I/O, network, parse errors, operational validation, git exit codes) — not record compact constructors
- Explicit `throw` inside `.chain()` callbacks or `ExceptionalSupport.supply()` lambdas (use `ExceptionalSupport.fail` instead)
- `ExceptionalSupport.response(...)` in production code (tests only)
- Business `try/catch` where `ExceptionalResponse` should carry the failure
- Swallowed errors (`catch (...) { return default; }`) — use `wasError()` at the call site instead
- Broad `catch (Exception)` — checkstyle `IllegalCatch` rejects this
- Manual `try-with-resources` + catch where `ExceptionalResource` should be used

### Prefer `ExceptionalResource` over manual resource catches

Use `ExceptionalResource.of(() -> open(), resource -> use(resource))` so cleanup and failure handling stay inside the exceptional library — not a hand-written catch block.

## 2. Severity

- All findings in this ruleset are **must-fix**

## 3. Response format

- One bullet per finding: `path:line — must-fix — §1 — brief description`
- List files with no issues under **Clean**
- Do not restate the rules; only report violations
- If the diff lacks context to judge exceptional handling, say "insufficient context" — do not guess