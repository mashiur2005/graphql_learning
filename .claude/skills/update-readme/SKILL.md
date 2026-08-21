---
name: update-readme
description: Review the current diff/commits against README.md and update the README so it accurately reflects the codebase. Use before every git push, and whenever asked to update the README.
---

Run this before every `git push` in this repo, and any time the user asks to update the README.

1. Check what's changed since the README was last updated:
   ```bash
   git log -1 --format=%H -- README.md
   git diff <that-commit>..HEAD --stat
   git status
   ```
2. Read the current `README.md` and compare it against the real state of the repo:
   - New/renamed/removed source files, scripts, or endpoints not reflected in "Project structure"
   - New or changed commands (build/run/test), dependencies, or ports
   - New GraphQL queries/mutations not documented under "Using the API"
   - Anything the README claims that is no longer true
3. Edit `README.md` to close those gaps. Keep the existing structure/tone; don't rewrite sections that are still accurate. Don't pad it with generic filler.
4. Stage the change: `git add README.md`. If nothing needed updating, say so explicitly rather than making a no-op edit.
5. Continue with the commit/push the user asked for.
