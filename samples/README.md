# Samples

Curated review reports you can read without running Ollama, plus scripts to regenerate them.

| Sample | Diff | Verdict | Reproduce |
|--------|------|---------|-----------|
| [diff-discipline-small](diff-discipline-small/) | `3bc265e^..3bc265e` (3 Java files) | APPROVE_WITH_NITS | `./samples/diff-discipline-small/reproduce.sh` |

**Requirements to reproduce:** Java 21+, Maven, Ollama with `qwen3-coder-next-256k:latest` (or edit `~/.code-review/config.json`). Working tree must be clean — the script checks out a historical commit and restores your branch when finished.

Reports are deterministic enough for demos but not byte-identical across model versions or temperature tweaks.