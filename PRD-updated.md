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

## 2026-07-07 — Ruleset batch context caps

- Per-ruleset agents batch all matching files in one LLM call; this should remain the
  default.
- Add configurable caps to split oversized batches: `maxAgentDiffKb` (combined diff
  size per agent call) and `maxFilesPerAgent` (optional file-count limit, 0 = unlimited).
- `maxDiffKb` remains the per-file ingest skip threshold; `maxAgentDiffKb` governs how
  many ingested files are packed into a single agent prompt.
- When splitting, CLI should report sub-batch progress (e.g. `java-general batch 2/3`).

## 2026-07-07 — Follow-up chat on review

- After the report, user wants an interactive chat agent to ask follow-up questions.
- Chat context = completed report (agent reviews + summary), not full raw diffs.
- Default on when stdin is a TTY; `--no-chat` for CI/scripting.
- Fits task 16; uses existing `ChatModel` + conversation history (langchain4j).