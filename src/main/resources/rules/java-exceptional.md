---
paths:
  - "**/*.java"
---

# Java exceptional error handling

**Mandatory.** Any code path that can fail — network, filesystem, parsing, git, external services — **must** use `org.dempsay.utils:exceptional` and return `ExceptionalResponse<T>`. No `throws`, no `throw` to callers, no business `try/catch` on those paths. Violations are **must-fix**. Pure domain logic, DTOs, and records do not need exceptional unless they perform I/O. **Record compact constructors** that throw to enforce invariants are explicitly out of scope (see below).

When reviewing **full files** (no diff fences), audit all I/O and external call paths.

## Diff review discipline

When the prompt contains unified diffs (fenced `diff` blocks below each file):

- Lines starting with `+` are **added**; `-` are **removed**; leading-space lines are **unchanged context**.
- Report an exceptional violation **only** if it appears on a **`+` line** in the diff.
- **Never** flag context lines or removed `-` lines — even when they show `throws`, `throw`, or `*Required` methods, they are not in the code after the change.
- **Removals are not violations** — deleting `throws` declarations, removing `*Required` helpers, replacing `throw` with `ExceptionalSupport.fail(listener, error)`, and returning `ExceptionalResponse` instead of throwing are **correct migrations**; do not flag the removed `-` lines.
- Each finding must cite a **`+` line** (path:line and the added content or a faithful paraphrase). If you cannot point to a `+` line that introduces the violation, **omit** the finding.
- Allowed on `+` lines: JDK I/O inside `ExceptionalSupport.supply` / `ExceptionalResource` lambdas; `ExceptionalSupport.fail(listener, error)` for expected failures; record compact-constructor `throw` (out of scope).
- Context lines showing old `throws`/`throw` patterns above new `+` exceptional code are **out of scope** — judge only what the `+` lines add.

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

**Forbidden inside `.chain()` callbacks and `ExceptionalSupplier.of(() -> { ... })` lambdas:**

- `throw new IllegalArgumentException(...)`
- `throw new IllegalStateException(...)`
- Any other explicit `throw` for an expected failure you are handling

**Required instead:**

- In `.chain()` callbacks — `listener.onError(error);` then `return ExceptionalResponse.failure();`
- At method entry (no listener in scope) — return a failure `ExceptionalResponse` via a shared project helper (not inline `throw` in application code)
- Composing steps — `.chain()` from the prior `ExceptionalResponse`; never unwrap with a test-only helper in production code

```java
// BAD — throw in chain callback
return runExternal(command)
    .chain((listener, result) -> {
      if (result.exitCode() != 0) {
        throw new IllegalStateException("external command failed");
      }
      return ExceptionalResponse.success(parse(result));
    });

// GOOD
return runExternal(command)
    .chain((listener, result) -> {
      if (result.exitCode() != 0) {
        listener.onError(new IllegalStateException("external command failed"));
        return ExceptionalResponse.failure();
      }
      return ExceptionalResponse.success(parse(result));
    });

// BAD — unwrap without checking wasError() (often throws AssertionError in tests)
final Config config = unwrap(loadConfig(path));

// GOOD — chain propagates wasError() to the caller
return loadConfig(path)
    .chain((listener, config) -> buildReport(config), listener);
```

Unwrapping an `ExceptionalResponse` without checking `wasError()` — including test helpers that throw on failure — is for **tests only**.

JDK calls such as `Files.readString(path)` may throw inside a supply/resource lambda; the library captures those. Do not add a separate `throw` for the same failure path.

### Out of scope (do not report)

**Record compact constructors** — `throw new IllegalArgumentException(...)` (or similar unchecked exception) in a record's compact constructor to reject invalid field combinations is allowed. Records are immutable value types, not I/O orchestration; if the args are illegal, the object must not exist. This is invariant enforcement at construction time, not an operational failure that should flow as `ExceptionalResponse`.

```java
public record OrderRequest(String customerId, int quantity) {
  public OrderRequest {
    if (customerId == null || customerId.isBlank()) {
      throw new IllegalArgumentException("customerId is required");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }
}
```

Do **not** flag compact-constructor `throw` as a §1 violation. Do flag the same pattern inside `.chain()` callbacks or `ExceptionalSupplier` lambdas.

### Report as findings

- Methods that declare `throws` instead of returning `ExceptionalResponse`
- `throw` used for expected failure paths (I/O, network, parse errors, operational validation, external-process exit codes) — not record compact constructors
- Explicit `throw` inside `.chain()` callbacks or `ExceptionalSupplier` lambdas (use `listener.onError` + `ExceptionalResponse.failure()` instead)
- Unwrapping `ExceptionalResponse` without `wasError()` in production code (tests only)
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
- In diff mode, every bullet must correspond to a **`+` line** that **introduces** a violation; otherwise output `## Clean`
- In diff mode, do not emit a finding and later retract it — if it is not on a `+` line, omit the bullet on the first pass
- **Clean** — `## Clean` only when there are zero findings; otherwise omit Clean or say `Clean: all other files in scope` — never enumerate every clean file
- Do not restate the rules; only report violations
- If the diff lacks context to judge exceptional handling on a `+` line, omit the finding — do not guess or emit "insufficient context" bullets