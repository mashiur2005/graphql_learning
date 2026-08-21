#!/usr/bin/env bash
# PreToolUse hook (Bash matcher). Blocks `git push` if README.md looks stale
# relative to source changes made since README.md was last updated.
set -euo pipefail

input="$(cat)"
cmd="$(echo "$input" | jq -r '.tool_input.command // empty')"

case "$cmd" in
  *"git push"*) ;;
  *) exit 0 ;;
esac

readme_commit="$(git log -1 --format=%H -- README.md 2>/dev/null || true)"
if [ -z "$readme_commit" ]; then
  exit 0
fi

changed="$(git log --name-only --pretty=format: "${readme_commit}..HEAD" -- . 2>/dev/null \
  | sed '/^$/d' \
  | grep -v '^README\.md$' \
  | grep -v '^\.claude/' \
  || true)"

staged_readme=""
if git diff --cached --name-only 2>/dev/null | grep -qx 'README.md'; then
  staged_readme="yes"
fi

if [ -z "$changed" ] || [ -n "$staged_readme" ]; then
  exit 0
fi

jq -n --arg reason "README.md looks stale: source files changed since README.md was last updated (commit ${readme_commit}), but README.md was not updated in this push. Run the update-readme skill (.claude/skills/update-readme) to refresh README.md, commit it, then retry the push." \
  '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"deny",permissionDecisionReason:$reason}}'
