#!/bin/bash
# PostToolUse hook: after a .java edit, compile offline and report errors back to Claude (exit 2).
f=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty')
case "$f" in
  */lumoo-project/src/*.java) ;;
  *) exit 0 ;;
esac
cd "$(dirname "$0")/../.." || exit 0
out=$(./mvnw -q -o compile 2>&1)
if [ $? -ne 0 ]; then
  echo "Compile failed after editing $f:" >&2
  echo "$out" | grep -E "ERROR|error:" | grep -v "^\[ERROR\] *$\|Help 1\|re-run Maven\|full stack\|For more information" | head -20 >&2
  exit 2
fi
exit 0
