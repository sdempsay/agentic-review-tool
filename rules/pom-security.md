---
paths:
  - "**/pom.xml"
---

# Maven POM security review

Review `pom.xml` changes for dependency hygiene and known-vulnerable direct dependency versions. This ruleset is **not** a CVE scanner — it applies a curated policy and minimum-version floors. Recommend `mvn org.owasp:dependency-check-maven:check` for full transitive CVE coverage.

When reviewing **diffs**, flag only added or changed dependency versions, properties, parent, or plugin coordinates unless a change weakens an existing floor. When reviewing **full files**, check all direct dependencies.

## 1. Parent and structure

- Dempsay projects should inherit `org.dempsay.maven:dempsay-parent`; flag missing parent or parent version below **1.0.4** as **must-fix**
- Direct dependency versions should use `${property}` entries in `<properties>` when the project defines them — flag hardcoded duplicates as **nit**
- Flag dependencies without an explicit version when not managed by BOM/import — **must-fix**

## 2. Known minimum versions (direct dependencies)

Flag **must-fix** when a direct dependency version is below the floor:

| Coordinate | Minimum version | Reason |
|------------|-----------------|--------|
| `org.yaml:snakeyaml` | 2.2 | CVE-2022-1471 (unsafe deserialization) |
| `org.apache.logging.log4j:log4j-core` | 2.17.1 | Log4Shell (CVE-2021-44228) |
| `org.apache.logging.log4j:log4j-api` | 2.17.1 | Log4Shell |
| `com.fasterxml.jackson.core:jackson-databind` | 2.15.0 | Multiple historical deserialization CVEs |
| `com.fasterxml.jackson.core:jackson-core` | 2.15.0 | Align with databind |
| `com.fasterxml.jackson.core:jackson-annotations` | 2.15.0 | Align with databind |
| `commons-collections:commons-collections` | — | **must-fix** if present at any version — use `commons-collections4` instead |
| `org.apache.commons:commons-text` | 1.10.0 | CVE-2022-42889 (Text4Shell) |

If a coordinate is not listed, do not invent a CVE — say "not in policy table; run dependency-check".

## 3. Risky patterns

- **must-fix** — `SNAPSHOT` versions on release branches (non-test dependencies)
- **must-fix** — dependencies pinned to versions with known public exploits called out in §2
- **nit** — test-scoped deps (e.g. JUnit) below current patch without security note
- **nit** — properties block missing entries for direct deps that repeat version literals

## 4. Verification reminder

When any dependency version changes, note in findings:

> Confirm with `mvn org.owasp:dependency-check-maven:check` (transitive tree).

Do not claim a CVE unless §2 covers the GAV or the diff explicitly introduces a listed vulnerable range.

## 5. Severity

- **must-fix** — below §2 floor, banned coordinates, missing version, vulnerable Log4j/SnakeYAML ranges, SNAPSHOT on release deps
- **nit** — property hygiene, parent could be newer, verification reminder only

## 6. Response format

- One bullet per finding: `pom.xml:line — [must-fix|nit] — §N — coordinate:version — brief description`
- List **Clean** if no issues
- Do not restate the rules; only report violations
- If version is inherited from parent/BOM and not visible in the diff, say "insufficient context — version managed externally"