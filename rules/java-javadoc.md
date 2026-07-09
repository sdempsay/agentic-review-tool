---
paths:
  - "**/*.java"
  - "**/*.java.ftl"
---

# Java Javadoc

Review the Java files provided in this prompt for Javadoc on new public API. Report concrete violations with file and line context.

When reviewing **full files** (no diff fences), check all public types and methods.

## Diff review discipline

When the prompt contains unified diffs (fenced `diff` blocks below each file):

- Lines starting with `+` are **added**; `-` are **removed**; leading-space lines are **unchanged context**.
- Report a Javadoc violation **only** if a **`+` line** introduces a **new public** type or **new public** method declaration without Javadoc on the lines above it in the same hunk.
- **Never** flag context lines or removed `-` lines.
- Each finding must cite the **`+` line** of the new public declaration (path:line and the added signature). If you cannot point to that `+` line, **omit** the finding.
- **Out of scope on `+` lines** — do not report:
  - `private` or package-private members (methods, fields, nested types) — verify the **`+` line** actually declares `public`/`protected` before reporting
  - Methods under `src/test/**`
  - Listener-threading overloads that mirror an existing public method (e.g. adding `ExceptionalListener` beside an established API)
  - Signature migrations (`throws` → `ExceptionalResponse`, renaming `*Required` helpers) unless a **brand-new public** type or method is introduced
  - Static factory helpers, package-private types, and internal refactorings
  - **`@Override` methods** — Javadoc is inherited from the supertype; do not require duplicate Javadoc on overrides unless the override introduces materially new contract (rare)
  - **Interface `default` methods** that already have Javadoc in the same hunk (context or `+` lines above the method) — do not report "missing" when a `/** ... */` block is visible above the declaration
  - **OSGi / DI lifecycle** — `@Activate` / `@Deactivate` constructors, `@Reference` setters, and internal `@ObjectClassDefinition` / metatype `Config` nested interfaces (the annotation `name` / `description` attributes are sufficient documentation)
- Do not infer a "new method" from context alone — the violation must attach to the **`+` line** that declares `public`/`protected` API.
- **Verify before reporting** — if the finding claims `public` but the `+` line shows `private`, `protected`, or package-private, omit the finding. Do not misread field or method visibility.

## FreeMarker Java templates (`*.java.ftl`)

When reviewing files whose paths end in `.java.ftl`:

- These are **code-generation templates**, not compiled Java.
- **Ignore** FreeMarker directives and expressions entirely.
- Flag missing Javadoc only on **static `public` API** visible as Java in the template (e.g. generated class or `public` method declarations in the emitted shape).
- **Fragment templates** and FTL control flow — out of scope for Javadoc.
- In diff mode, only flag `+` lines that introduce new public declarations without Javadoc above them in the hunk.

## 1. Javadoc

- Flag missing Javadoc only on **new public** types and **new public** methods introduced on **`+` lines**
- New public API should include `@since 1.0.0` (first-release baseline; use the introducing release version after 1.0.0 ships) and `@author Shawn Dempsay {@literal <shawn@dempsay.org>}`
- Document parameters and return values on non-obvious public methods
- Only require `@return` when behavior is not obvious from the signature
- Do not require Javadoc on private helpers, private constants, package-private types, tests, OSGi wiring, or trivial one-line changes

## 2. Severity

- **nit** — missing or incomplete Javadoc unless the change introduces undocumented public API surface

## 3. Response format

- One bullet per finding: `path:line — nit — §1 — brief description`
- In diff mode, every bullet must correspond to a **`+` line** declaring new public API; otherwise output `## Clean`
- In diff mode, do not emit a finding and later retract it — if it is not on a `+` line, omit the bullet on the first pass
- **Clean** — `## Clean` only when there are zero findings; otherwise omit Clean or say `Clean: all other files in scope` — never enumerate every clean file
- Do not restate the rules; only report violations
- If the diff lacks context to judge Javadoc on a `+` public declaration, omit the finding — do not guess or emit "insufficient context" bullets