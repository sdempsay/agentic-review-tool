---
paths:
  - "**/*.java"
---

# Java Javadoc

Review the Java files provided in this prompt for Javadoc on new public API. Report concrete violations with file and line context.

When reviewing **diffs**, flag only added or changed lines. When reviewing **full files**, check all public types and methods.

## 1. Javadoc

- Flag missing Javadoc on **new public** types and methods introduced in the diff
- New public API should include `@since` and `@author Name {@literal <email@domain>}`
- Document parameters and return values on non-obvious public methods
- Only require `@return` when behavior is not obvious from the signature
- Do not require Javadoc on private helpers, tests, or trivial one-line changes

## 2. Severity

- **nit** — missing or incomplete Javadoc unless the change introduces undocumented public API surface

## 3. Response format

- One bullet per finding: `path:line — nit — §1 — brief description`
- List files with no issues under **Clean**
- Do not restate the rules; only report violations
- If the diff lacks context to judge Javadoc requirements, say "insufficient context" — do not guess