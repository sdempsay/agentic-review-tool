#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
SAMPLE_COMMIT=3bc265e
BASE="${SAMPLE_COMMIT}^"
OUTPUT="${SCRIPT_DIR}/report.md"

cd "${REPO_ROOT}"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree must be clean. Stash or commit first." >&2
  exit 1
fi

CURRENT="$(git rev-parse HEAD)"
restore() {
  git checkout "${CURRENT}" -q
}
trap restore EXIT

echo "Checking out ${SAMPLE_COMMIT} (diff against ${BASE})..."
git checkout "${SAMPLE_COMMIT}" -q

mvn package -DskipTests -q
java -jar target/code-review.jar doctor
java -jar target/code-review.jar diff --base "${BASE}" --no-chat --output "${OUTPUT}"

echo "Wrote ${OUTPUT}"