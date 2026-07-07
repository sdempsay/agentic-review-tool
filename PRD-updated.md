# PRD-updated.md

Requirements learned during implementation that extend or refine `PRD.md`.

## 2026-07-07 — Rules directory

- Default `rulesDir` is `~/.grok/rules`, not `~/.code-review/rules`.
- Instructional docs in the rules directory (e.g. `maven.md` without YAML `paths`
  frontmatter) must be skipped — only path-gloved `*.md` files are review rules.

## 2026-07-07 — Enhanced CLI experience

- Multi-agent reviews can run for several minutes with no intermediate output; the CLI
  needs live progress reporting (see PRD §5 "Enhanced CLI Experience").
- Progress should cover pipeline stages, per-ruleset agents, and per-file scope.
- LLM thinking/reasoning should stream to the terminal when the provider supports it.
- Verbosity flags (`--quiet`, `--verbose`) should control how much is shown.