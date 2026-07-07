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
- Chat is an orchestrator, not just a summarizer: when follow-ups need deeper analysis,
  re-invoke the matching ruleset agent with the file diff + question + prior finding.
- Routing reuses classification from the original run; ingest data retained for the session.

## 2026-07-07 — Full repository review (Phase 2, task 20)

Extends the pipeline from diff-only to whole-repo review. Reuses Classify → Review →
Summarize; adds a new ingest path and `code-review repo` subcommand.

### CLI

```
code-review repo [options]
```

Shares flags with `diff` where applicable: `--config`, `--dry-run`, `--output`,
`--quiet`, `--verbose`, `--chat` / `--no-chat`.

Additional flags:

| Flag | Purpose |
|------|---------|
| `--path <glob>` | Limit review to paths matching glob (repeatable) |
| `--include-ext <ext>` | Only these extensions (overrides default excludes when set) |
| `--exclude-ext <ext>` | Extra extensions to skip (repeatable) |

### File discovery (tracked + untracked, gitignore-safe)

Repo ingest must include **both tracked and untracked** files while **never** picking up
gitignored paths. Use the same git semantics as uncommitted diff ingest:

1. **Tracked** — `git ls-files` (files in the index / committed tree)
2. **Untracked** — `git ls-files --others --exclude-standard` (working-tree files not
   in the index, excluding anything matched by `.gitignore` or global excludes)

Union the two lists (dedupe by path). Only regular files are read; symlinks and
directories are skipped.

`--exclude-standard` is required: without it, `git ls-files --others` would include
`target/`, `node_modules/`, build artifacts, etc.

### Default file-type exclusions

Repo review targets reviewable source, not docs/config noise. **By default, skip files
whose extension is in the built-in deny list:**

- `.md` — documentation and rule prompt files
- `.json` — config manifests, lockfiles metadata (not primary review targets)

Users can widen scope with `--path` or `--include-ext`. Additional skips via
`--exclude-ext`.

`repoExcludeExtensions` in `config.json` extends the default deny list (`.md`, `.json`)
without CLI flags. Values merge with `--exclude-ext` when no `--include-ext` is set.

## 2026-07-07 — OpenRouter provider + token usage (task 19)

- `provider: "openrouter"` uses `langchain4j-open-ai` against `https://openrouter.ai/api/v1`
- API key via `model.apiKey` or `OPENROUTER_API_KEY` env var (`${OPENROUTER_API_KEY}` supported)
- `doctor` validates OpenRouter `/models` and configured model id
- `LlmTokenLedger` records input/output/total tokens per LLM call (Ollama + OpenRouter)
- Review output appends `--- Token Usage ---` with per-call breakdown and totals for comparison

### Ingest behaviour

- Read full file content (not a git diff); represent as `ChangedFile` with
  `changeType = EXISTING` (or equivalent) and content in the diff/content field.
- Reuse `maxDiffKb` as per-file read cap (skip with reason when exceeded).
- Skip binary files (same detection approach as diff ingest).
- Only send files to review that have at least one matching ruleset, **or** to the
  general fallback agent when no rule matches (same as diff pipeline).

### Review / batching

Reuse `RulesClassifier`, `RulesetReviewPlanner`, `RulesetBatchSplitter`,
`AgentBatchLimits`, and Ollama `num_ctx` soft/hard caps. Repo runs produce many more
agent batches than diff runs — progress reporting is critical.

`ReviewPromptBuilder` gains a **full-file** mode (path + content sections) instead of
diff hunks.

### Summarize

Repo summary adds codebase-level framing: overall health, hotspot areas, cross-cutting
findings, and recommendation. May use a two-pass rollup (per-agent → repo summary).

### Non-goals (task 20; defer to task 23)

- Vector RAG / embeddings
- Call-graph or cross-repo analysis
- Incremental “since last repo run” caching

### Implementation slices (TODO 20a–20e)

| Slice | Deliverable |
|-------|-------------|
| 20a | `RepoIngestService`: tracked + untracked listing, default ext filter, `--dry-run` |
| 20b | Full-file prompt builder + `LlmReviewService` wiring |
| 20c | `RepoCommand` end-to-end with report export |
| 20d | Repo-specific summarize prompt + fixture tests |
| 20e | `repoExcludeExtensions` in config; PRD §3 update |

### Success criteria

- `code-review repo --dry-run` lists files with classification; gitignored paths absent
- Untracked, non-ignored files (e.g. new `.java` not yet `git add`) appear in the list
- `.md` and `.json` files excluded by default
- `code-review repo --path 'src/**'` runs review with batching and summary
- `mvn verify` passes