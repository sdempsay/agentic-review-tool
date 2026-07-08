## Output

- One bullet per finding: `path:line — [must-fix|nit] — brief description`
- **Findings first** — list every violation as a bullet; no preamble or rule restatement
- **Clean** — if there are **no** findings, write `## Clean` only. If there **are** findings, do **not** enumerate clean files; omit Clean or write `Clean: all other files in scope`
- Stay concise — large batches must not list every clean file path
- If the diff lacks context to judge a rule, say "insufficient context" — do not guess