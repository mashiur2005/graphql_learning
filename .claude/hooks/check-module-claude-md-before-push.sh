#!/usr/bin/env bash
# PreToolUse hook (Bash matcher). Blocks `git push` if a module's CLAUDE.md
# looks stale relative to source changes made under that module since its
# CLAUDE.md was last updated.
set -euo pipefail

input="$(cat)"
cmd="$(echo "$input" | jq -r '.tool_input.command // empty')"

case "$cmd" in
  *"git push"*) ;;
  *) exit 0 ;;
esac

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

staged_files="$(git diff --cached --name-only 2>/dev/null || true)"
stale=""

while IFS= read -r claude_md; do
  [ -z "$claude_md" ] && continue
  module_dir="$(dirname "$claude_md")"

  claude_md_commit="$(git log -1 --format=%H -- "$claude_md" 2>/dev/null || true)"
  [ -z "$claude_md_commit" ] && continue

  if echo "$staged_files" | grep -qx "$claude_md"; then
    continue
  fi

  changed="$(git log --name-only --pretty=format: "${claude_md_commit}..HEAD" -- "$module_dir" 2>/dev/null \
    | sed '/^$/d' \
    | grep -vx "$claude_md" \
    || true)"

  if [ -n "$changed" ]; then
    stale="${stale}- ${claude_md} (stale since ${claude_md_commit})\n"
  fi
done <<MODULES
$(git ls-files -- '*/CLAUDE.md' 2>/dev/null)
MODULES

if [ -z "$stale" ]; then
  exit 0
fi

jq -n --arg reason "Module CLAUDE.md file(s) look stale: source files changed under the module since it was last updated, but it wasn't refreshed in this push:
$(printf '%b' "$stale")
Run the update-module-claude-md skill (.claude/skills/update-module-claude-md) to refresh them (and the root CLAUDE.md if needed), commit, then retry the push." \
  '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"deny",permissionDecisionReason:$reason}}'
