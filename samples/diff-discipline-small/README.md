# diff-discipline-small

A small, dogfood diff from this repo: the commit that added unified-diff discipline to review prompts (`3bc265e`).

- **Base:** `3bc265e^` (`0459f0b`)
- **Head:** `3bc265e`
- **Files:** `ReviewPromptBuilder.java` and two test classes (`.md` rule files are skipped by ingest)
- **Model:** Ollama `qwen3-coder-next-256k:latest`, `reviewMaxTokens` 4096

## Read the report

[report.md](report.md) — full Markdown export with ingest, classification, per-agent findings, summary, and token usage.

## Regenerate

From a clean working tree at `master`:

```bash
./samples/diff-discipline-small/reproduce.sh
```

Or manually:

```bash
git checkout 3bc265e
mvn package -DskipTests
java -jar target/code-review.jar diff --base 3bc265e^ --no-chat --output samples/diff-discipline-small/report.md
git checkout master
```

`java-exceptional` and `java-javadoc` came back clean; `java-formatting` filed brace-placement nits with line citations and the summarizer rolled them into top actions.