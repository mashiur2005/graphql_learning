# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build                                                        # compile + test + assemble every module
./gradlew run                                                          # start app's GraphQL server on http://localhost:8080/graphql (see app/CLAUDE.md)
./gradlew test                                                         # run the full test suite (every module)
./gradlew test --tests "com.example.graphql.GraphQLRoutesSpec"         # run a single test class
./gradlew test --tests "*GraphQLRoutesSpec*createGame*"                # run a single test case by name pattern
./gradlew compileScala                                                 # fast compile-only check, every module
./gradlew scalafmtCheck                                                # fail if any Scala source is unformatted
./gradlew scalafmtApply                                                # reformat all Scala sources with scalafmt
./app/curl-commands.sh                                                 # exercise every query/mutation/error case against a running server
```

Test reports land in `<module>/build/reports/tests/test/index.html`; JUnit-format XML in `<module>/build/test-results/test/`.

For commands scoped to a single module (module-only build/test, module-specific run tasks, Docker setup), see that module's own CLAUDE.md — linked below.

## Architecture

This is a multi-module Gradle build (root `settings.gradle` includes `app` and
`product_search`) using the version catalog at `gradle/libs.versions.toml`
(all dependency coordinates and versions live there, not inline in each
module's `build.gradle`). Each module is a Scala 2.13 GraphQL API built on
Sangria (schema/execution) + Apache Pekko HTTP (server) + Circe (JSON), served
on its own port so both can run at once:

- **[`app`](app/CLAUDE.md)** — `Game`/`Author`/`Review`, backed by an
  in-memory mutable map (`:8080`). The original module in this repo.
- **[`product_search`](product_search/CLAUDE.md)** — a garment product
  catalog, backed by Elasticsearch (`:8081`).

Read the linked module CLAUDE.md before working on that module's code — it
covers that module's request flow and the design invariants specific to it
(e.g. `app`'s mutable in-memory store vs. `product_search`'s Elasticsearch
index). This file covers only what's shared across every module.

### Conventions shared across all modules

- **Every model field is `Option[...]`.** `app`'s `Game`/`Author`/`Review` and
  `product_search`'s `Product` wrap every attribute in `Option`. Schema field
  types mirror this with `OptionType(...)` / `OptionInputType(...)` wrappers.
  New fields in any module should follow the same all-`Option` convention
  rather than mixing in required fields.
- **`<Module>Routes.route` is a plain function `(Context) => Route`**,
  deliberately separated from the module's `*Server` object so it can be
  exercised with `pekko-http-testkit`'s `ScalatestRouteTest` without binding a
  real socket. Keep new route logic in the `*Routes` object, not inlined into
  a `*Server.main`.
- **Sangria errors are caught and turned into GraphQL error payloads, not
  HTTP error codes.** `QueryAnalysisError`/`ErrorWithResolver` are recovered
  into a Circe `Json` error body with HTTP 200; only a JSON-parse failure or a
  GraphQL syntax error (`SyntaxError` from `QueryParser.parse`) produces an
  HTTP 400. This is identical across modules — keep it that way if you add a
  new one.
- **`scalafmtCheck`/`scalafmtApply` (defined per-module in each
  `build.gradle`) shell out to the `scalafmt` CLI directly** rather than using
  a Gradle scalafmt plugin — the obvious plugin
  (`cz.alenkacz.gradle.scalafmt`) uses APIs removed in Gradle 9 and fails to
  apply. Requires `scalafmt` on `PATH`.
- **`.githooks/pre-push` auto-formats before every push** (enabled per-clone
  via `git config core.hooksPath .githooks`, not a Claude Code hook). Because
  git fixes the commit(s) to push before invoking the hook, a reformat can't
  be folded into the push already in flight — the hook commits the fix and
  aborts (exit 1) instead; the next `git push` then succeeds immediately since
  nothing is left to format. Don't try to make it "succeed on the first push"
  — that's not achievable with a pre-push hook.

## Keeping the CLAUDE.md files current

Each module has its own CLAUDE.md (`app/CLAUDE.md`, `product_search/CLAUDE.md`)
for module-specific architecture/commands, and this root file holds only
repo-wide/shared conventions. Before every `git push`, run the
`update-module-claude-md` skill (`.claude/skills/update-module-claude-md`) to
refresh whichever module CLAUDE.md files are stale relative to code changes
in that module, then update this root file if the change affects something
shared across modules (a new module, a repo-wide command, a convention that
moved from one module into the shared list above, or vice versa). A
PreToolUse hook (`.claude/hooks/check-module-claude-md-before-push.sh`) blocks
`git push` if a module's CLAUDE.md looks stale and wasn't updated in the
same push.
