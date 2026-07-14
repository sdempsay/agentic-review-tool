---
paths:
  - "**/*.java"
  - "**/*.java.ftl"
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

## Verification (mandatory before each bullet)

- Quote the exact `+` line that introduces the violation (`throws`, `throw`, or `try`/`catch` on an I/O path).
- If you cannot quote that line verbatim from the prompt, **omit** the bullet.
- **One pass only** — do not emit a bullet, then retract or re-evaluate in the same response.
- Scan **every** new or changed `src/main/java/**` method on `+` lines — including `private`, `package-private`, and internal helper types (`*Io`, `*Support`, `readPersisted`, `writePersisted`). **No boundary layer:** helpers cannot use `throws` while public API returns `ExceptionalResponse`.
- A `+` line with `try {` whose `catch` handles `IOException` (or other checked I/O) and returns `ExceptionalResponse.failure()` / project `fail(...)` is still a **must-fix** — use `ExceptionalSupplier` / `ExceptionalResource` instead of hand-written catch.
- Public store/service methods already returning `ExceptionalResponse` do **not** excuse `throws` or `try/catch` on helpers they call in the same class or package.

## FreeMarker Java templates (`*.java.ftl`)

When reviewing files whose paths end in `.java.ftl`:

- These are **code-generation templates**, not compiled Java. Review the **Java source they would emit** after template expansion.
- **Ignore** FreeMarker directives and expressions (`<#if>`, `<#list>`, `<#include>`, `<#assign>`, `${...}`, `<#-- ... -->`, etc.). Do not treat them as Java violations.
- **Full-unit templates** emit a compilable Java type (package, imports, class). **Fragment templates** emit only a snippet — do not require package or class structure on fragments.
- In diff mode, apply diff review discipline to static emitted Java on `+` lines only.
- Flag `throws` or explicit `throw` only in **static emitted Java lines** inside the template. Prefer `ExceptionalResponse` and listener-based failure paths where the emitted code handles failures.

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

### No adapter boundary (filesystem / store internals)

Internal I/O helpers are **not** exempt. The pattern below is **always** a must-fix on `+` lines — even when the public port already returns `ExceptionalResponse`:

```java
// BAD — private helper throws; public method try/catch converts to failure response
private Persisted readPersisted(final Path path) throws IOException {
  return documentIo.read(path);
}

public ExceptionalResponse<Persisted> read(final ExceptionalListener onError, final String id) {
  try {
    return ExceptionalResponse.success(readPersisted(path));
  } catch (final IOException ex) {
    return fail(onError, Internal, "Failed to read", ex);
  }
}

// GOOD — exceptional through the stack
private ExceptionalResponse<Persisted> readPersisted(final Path path) {
  return documentIo.read(path)
      .chain((listener, document) -> ExceptionalResponse.success(toPersisted(document)), listener);
}
```

Flag **each** `+` line that adds: `throws IOException` (or any `throws` on I/O), a `try`/`catch` around filesystem/network work, or `throw new IllegalStateException` / `IllegalStateException` for I/O setup failures in `src/main` (use `ExceptionalSupplier` / defer work to a method that returns `ExceptionalResponse`).

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

**Third-party `@Override` contracts** — methods implementing framework interfaces that **require** checked exceptions in the signature (e.g. Gson `TypeAdapter.read` / `write` declaring `throws IOException`) are out of scope when the `+` line is only the mandated `@Override` signature. Still flag **new** `throws` on project-owned methods and any hand-written `try/catch` around Gson/file I/O in project code.

### Report as findings

- Methods that declare `throws` instead of returning `ExceptionalResponse`
- `throw` used for expected failure paths (I/O, network, parse errors, operational validation, external-process exit codes) — not record compact constructors
- Explicit `throw` inside `.chain()` callbacks or `ExceptionalSupplier` lambdas (use `listener.onError` + `ExceptionalResponse.failure()` instead)
- Unwrapping `ExceptionalResponse` without `wasError()` in production code (tests only)
- Business `try/catch` where `ExceptionalResponse` should carry the failure — including `catch (IOException)` (or similar) that returns a failure `ExceptionalResponse` / project `fail(...)`
- Private or package-private helpers (`*Io`, `readPersisted`, `writePersisted`, etc.) that declare `throws` on filesystem or network work
- `throw new IllegalStateException` (or similar) in `src/main` for I/O index rebuild / constructor setup that should be `ExceptionalSupplier` or lazy `ExceptionalResponse` initialization
- Swallowed errors (`catch (...) { return default; }`) — use `wasError()` at the call site instead
- Broad `catch (Exception)` — checkstyle `IllegalCatch` rejects this
- Manual `try-with-resources` + catch where `ExceptionalResource` should be used

### Prefer `ExceptionalResource` over manual resource catches

Use `ExceptionalResource.of(() -> open(), resource -> use(resource))` so cleanup and failure handling stay inside the exceptional library — not a hand-written catch block.

## 2. Severity

- All findings in this ruleset are **must-fix**
- `src/test/**` — only flag when a `+` line introduces a new I/O or external call path that uses `throws`, `throw`, or business `try/catch` instead of exceptional

## 3. Response format

- One bullet per finding: `path:line — must-fix — §1 — brief description`
- In diff mode, every bullet must correspond to a **`+` line** that **introduces** a violation; otherwise output `## Clean`
- In diff mode, do not emit a finding and later retract it — if it is not on a `+` line, omit the bullet on the first pass
- **Clean** — `## Clean` only when there are zero findings; otherwise omit Clean or say `Clean: all other files in scope` — never enumerate every clean file
- Do not restate the rules; only report violations
- If the diff lacks context to judge exceptional handling on a `+` line, omit the finding — do not guess or emit "insufficient context" bullets