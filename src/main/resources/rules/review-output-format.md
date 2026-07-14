## Diff review discipline

Applies when reviewing unified diffs (lines below each file marked as a diff).

- Lines starting with `+` are **added** in the resulting code; `-` are **removed**; context lines (leading space) are unchanged.
- Report a finding **only** if the violation is on a **`+` line** (or you can tie it unambiguously to new code introduced in the hunk).
- **Never** cite removed `-` lines as current violations — they are not in the code after the change.
- Each finding must identify a **`+` line** (path:line and the added content or a faithful paraphrase). If you cannot point to a `+` line, omit the finding.
- Do not flag patterns the rules explicitly allow (e.g. JDK I/O inside `ExceptionalSupport.supply`, `ExceptionalResource`, or the `ExceptionalSupport.fail()` capture helper).

## Verification (mandatory before each bullet)

- Quote the exact `+` line (or the final added line of the file in the hunk) that proves the violation.
- If you cannot quote that line verbatim from the prompt, **omit** the bullet.
- **One pass only** — do not emit a bullet, then retract, re-evaluate, or say "false positive" in the same response.
- **must-fix** only when the matched ruleset's Severity section explicitly allows must-fix for that violation class.
- Do not infer imports, visibility (`public`/`private`/`protected`), or Javadoc from memory — read the lines shown in the hunk.
- Do not report regressions for intentional removals visible only on `-` lines or intentional default-behavior changes on `+` lines (e.g. replacing a TTY check with `return false`).

## Output

- One bullet per finding: `path:line — [must-fix|nit] — brief description`
- **Findings first** — list every violation as a bullet; no preamble or rule restatement
- **Clean** — if there are **no** findings, write `## Clean` only. If there **are** findings, do **not** enumerate clean files; omit Clean or write `Clean: all other files in scope`
- Stay concise — large batches must not list every clean file path
- If the diff lacks context to judge a rule, omit the finding — do not guess or emit "insufficient context" bullets