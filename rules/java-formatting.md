---
paths:
  - "**/*.java"
---

# Java formatting and style

Review the Java files provided in this prompt against the sections below. Report concrete violations with file and line context.

When reviewing **full files** (no diff fences), apply all sections to the entire file.

## Diff review discipline

When the prompt contains unified diffs (fenced `diff` blocks below each file):

- Lines starting with `+` are **added**; `-` are **removed**; leading-space lines are **unchanged context**.
- Report a formatting violation **only** if it appears on a **`+` line** in the diff.
- **Never** flag context lines or removed `-` lines — even when they violate §1–§7, they are not introduced by this change.
- Each finding must cite a **`+` line** (path:line and the added content or a faithful paraphrase). If you cannot point to a `+` line, **omit** the finding.
- Do not flag brace placement, indentation, or block structure on `if`/`else`/`for` openers unless that opener is itself a **`+` line**.
- Context lines shown only for orientation (e.g. `} else {` above your `+` strings) are **out of scope** — do not report them.

## 1. Generalized Java rules

- 4-space indentation (no tabs)
- Line length: 120 characters (checkstyle)
- One blank line between import groups and between class members
- No trailing whitespace
- Always use braces for single-line statements
- Use `final` for method parameters
- Use `final` for local variables where practical
- Use Objects.isNull or Objects.nonNull when checking null
- Formatting *ALWAYS* applies for test sources
- Files should always end with an empty newline

## 2. Block formatting

- Opening brace on same line (`if (...) {`)
- No blank lines after opening braces

## 3. Import Organization

1. `java.*` packages
2. Third-party imports (e.g., `com.google.*`, `org.osgi.*`)
3. Project-internal imports (same root package as the file under review, e.g. `com.acme.widget.*` when editing `com.acme.widget.Foo`)
   - Use fully qualified imports, no wildcard imports (enforced by checkstyle)
4. Always import classes when possible, *DO NOT* fully qualify them inside the code
5. Be sure to remove unused imports

## 4. Naming Conventions

- Naming in Java should *NEVER* use snake casing (hello_world)!
- **Classes/Interfaces**: PascalCase (`HelloWorld`, `MyCoolInterface`)
- **Methods**: camelCase (`hello`, `doWork`)
- **Constants**: UPPER_SNAKE_CASE (`CLASS_NAME`, `MY_LOGGING_CONSTANT`)
- **Records**: PascalCase with camelCase fields (`HelloMessage(String message, ...)`)
- **Packages**: lowercase with dots (`com.example.demo`)

## 5. Annotations Style

- Annotations on separate lines before class/method
- Use `@SuppressWarnings("boxing")` for unnecessary boxing warnings
- Use `@Override` consistently

## 6. Checkstyle-aligned review checks

Flag when visible in the diff. Full enforcement runs via `mvn verify`, not in this review.

- Space before opening parens on control keywords (`if (`, `for (`, `while (`)
- Modifier order: `public protected private abstract static final transient volatile synchronized native strictfp`
- Empty methods or constructors: `<ReturnType if needed> method(<parameters if needed) { }`
- Nested `if` depth greater than 3
- Methods growing very long — split when readability suffers (checkstyle `MethodLength`)
- Classes used as `Map`/`Set` keys must implement `equals` and `hashCode` together
- `switch` on enums should be exhaustive or have an explicit `default`
- Utility classes (only static members) should not expose a public constructor

## 7. Java 21 conventions

- Target Java 21 (source and target compatibility)
- Prefer records for immutable data carriers; use compact canonical constructors for validation
- Prefer `switch` expressions over fall-through `switch` statements
- Use `var` only when the type is obvious from the right-hand side; not in public API signatures
- Use sealed classes/interfaces when the hierarchy is intentionally closed

## 8. Severity

- **must-fix** — correctness issues in §6; checkstyle `IllegalCatch`
- **nit** — formatting, imports, naming, annotations, §6 style checks, §7 conventions

## 9. Response format

- One bullet per finding: `path:line — [must-fix|nit] — §N — brief description`
- In diff mode, every bullet must correspond to a **`+` line**; otherwise output `## Clean`
- In diff mode, do not emit a finding and later retract it — if it is not on a `+` line, omit the bullet on the first pass
- **Clean** — `## Clean` only when there are zero findings; otherwise omit Clean or say `Clean: all other files in scope` — never enumerate every clean file
- Do not restate the rules; only report violations
- If the diff lacks context to judge a rule on a `+` line, omit the finding — do not guess or emit "insufficient context" bullets