---
name: update-module-claude-md
description: Review the current diff/commits against each module's CLAUDE.md (app/CLAUDE.md, product_search/CLAUDE.md) and the root CLAUDE.md, and update whichever are stale. Use before every git push, and whenever asked to update a CLAUDE.md file.
---

Run this before every `git push` in this repo, and any time the user asks to
update a CLAUDE.md file. Update the per-module files first, then the root
file, in that order — the root file's "necessary changes" are often just a
consequence of what changed at the module level.

## 1. Update each module's CLAUDE.md

For every module CLAUDE.md tracked in the repo (currently `app/CLAUDE.md` and
`product_search/CLAUDE.md`; discover them with `git ls-files -- '*/CLAUDE.md'`
rather than hardcoding, in case a new module is added):

1. Find what changed under that module's directory since its CLAUDE.md was
   last touched:
   ```bash
   git log -1 --format=%H -- <module>/CLAUDE.md
   git diff <that-commit>..HEAD --stat -- <module>/
   git status
   ```
2. Read the current `<module>/CLAUDE.md` and compare it against the real
   state of that module's code:
   - New/renamed/removed source files, packages, or GraphQL fields not
     reflected in the architecture notes
   - New or changed module-scoped commands, dependencies, or ports
   - New "key design points" — a deliberate pattern, invariant, or gotcha a
     future change needs to respect — that isn't documented yet
   - Anything the file claims that is no longer true
3. Edit `<module>/CLAUDE.md` to close those gaps. Keep the existing
   structure/tone; don't rewrite sections that are still accurate. Don't pad
   it with generic filler, and don't duplicate content that belongs in the
   root CLAUDE.md (repo-wide commands, cross-module conventions) — link to it
   instead.
4. Stage the change: `git add <module>/CLAUDE.md`. If nothing needed
   updating, say so explicitly rather than making a no-op edit.

## 2. Update the root CLAUDE.md

Once the module files are current, check whether the root `CLAUDE.md` needs
changes as a result:

- Does its module list / architecture summary still match reality (new
  module added or removed, module renamed)?
- Do the repo-wide `Commands` still work, and do they still cover every
  module (e.g. a new module needing its own build/test invocation)?
- Did a convention move from "module-specific" to "shared across modules" (or
  vice versa) — if every module now follows a pattern that used to live in
  one module's CLAUDE.md, promote it to the root file and trim it from the
  module file(s); if a root-level convention no longer applies to a new
  module, scope it down instead.

Edit the root `CLAUDE.md` to close those gaps, stage it (`git add CLAUDE.md`),
and continue with the commit/push the user asked for.
