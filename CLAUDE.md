# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build                                                        # compile + test + assemble
./gradlew run                                                          # start the GraphQL server on http://localhost:8080/graphql
./gradlew test                                                         # run the full test suite
./gradlew test --tests "com.example.graphql.GraphQLRoutesSpec"         # run a single test class
./gradlew test --tests "*GraphQLRoutesSpec*createGame*"                # run a single test case by name pattern
./gradlew compileScala                                                 # fast compile-only check
./gradlew scalafmtCheck                                                # fail if any Scala source is unformatted
./gradlew scalafmtApply                                                # reformat all Scala sources with scalafmt
./curl-commands.sh                                                     # exercise every query/mutation/error case against a running server
```

Test reports land in `app/build/reports/tests/test/index.html`; JUnit-format XML in `app/build/test-results/test/`.

There is a single Gradle module, `app`, using the version catalog at `gradle/libs.versions.toml` (all dependency coordinates and versions live there, not inline in `app/build.gradle`).

## Architecture

This is a Scala 2.13 GraphQL API built on Sangria (schema/execution) + Apache Pekko HTTP (server) + Circe (JSON), with an in-memory, mutable, thread-safe data store — there is no database or persistence layer. Data resets to the seeded `SampleData` on every server restart.

Request flow: `GraphQLServer` (binds Pekko HTTP on `:8080`) → `GraphQLRoutes.route` (parses the JSON body into GraphQL `query`/`operationName`/`variables`, runs `sangria.execution.Executor.execute` against `SchemaDefinition.schema`, serializes the result with `sangria-circe`) → `SchemaDefinition` (Sangria `ObjectType`s and field resolvers) → `GraphQLContext` (the actual data + CRUD logic).

Key design points a future change needs to respect:

- **`GraphQLContext` is the single source of truth and the Sangria `userContext`.** It wraps three `mutable.LinkedHashMap[Int, T]` (games/authors/reviews) guarded by `synchronized` blocks, with auto-incrementing ids (`max(existing ids) + 1`). `GraphQLRoutes.route` and `SchemaDefinition`'s resolvers are pure/stateless — all mutation and lookup logic (`createX`/`updateX`/`deleteX`/`findX`) lives on `GraphQLContext`, not in the schema or route layer. One `GraphQLContext` instance is created in `GraphQLServer.main` and lives for the process lifetime, shared across all requests.
- **Update mutations are partial/PATCH-style, not full replacement.** Each `updateX` method takes `Option[...]` args and does `argValue.orElse(existing.value)` per field — an omitted GraphQL argument leaves that field unchanged rather than nulling it out. There's no way to explicitly clear a field to `null` via update (an intentional simplification).
- **Every model field is `Option[...]`.** `Game`, `Author`, `Review` (in `app/src/main/scala/com/example/model/`) wrap every attribute in `Option`, including `Game.platform: Option[Seq[String]]`. Schema field types mirror this with `OptionType(...)` / `OptionInputType(...)` wrappers in `SchemaDefinition`. New fields should follow the same all-`Option` convention rather than mixing in required fields.
- **`Review` fields use snake_case (`author_id`, `game_id`)** in both the case class and the GraphQL schema — this is deliberate, not an oversight; don't camelCase it without checking whether the client-facing API contract matters.
- **Sangria arguments are shared, standalone `Argument[T]` vals**, not per-mutation input object types (e.g. `IdArg`, `TitleArg`, `PlatformArg` in `SchemaDefinition` are reused across the `game`/`updateGame`/`deleteGame` fields). Follow this pattern for new fields rather than introducing `InputObjectType`s.
- **`GraphQLRoutes.route` is a plain function `(GraphQLContext) => Route`**, deliberately separated from `GraphQLServer` so it can be exercised with `pekko-http-testkit`'s `ScalatestRouteTest` without binding a real socket (see `GraphQLRoutesSpec`). Keep new route logic in `GraphQLRoutes`, not inlined into `GraphQLServer.main`.
- **Sangria errors are caught and turned into GraphQL error payloads, not HTTP error codes.** `QueryAnalysisError`/`ErrorWithResolver` are recovered into a Circe `Json` error body with HTTP 200; only a JSON-parse failure or a GraphQL syntax error (`SyntaxError` from `QueryParser.parse`) produces an HTTP 400.
- **`app/src/main/scala/com/example/App.scala`** is the original Gradle-init-generated "Hello, world!" sample and is unused by the running application (`application.mainClass` in `app/build.gradle` points to `com.example.GraphQLServer`). Its test (`AppSuite.scala`) has been removed; the file itself is untested but harmless dead code.
- Tests that assert exact counts against the shared sample data (e.g. "3 games") use the class-level `context`/`route` in `GraphQLRoutesSpec`; mutation tests must use `freshRoute` (a new `GraphQLContext()` per call) to avoid mutating shared state and breaking those count assertions.
- **`scalafmtCheck`/`scalafmtApply` (in `app/build.gradle`) shell out to the `scalafmt` CLI directly** rather than using a Gradle scalafmt plugin — the obvious plugin (`cz.alenkacz.gradle.scalafmt`) uses APIs removed in Gradle 9 and fails to apply. Requires `scalafmt` on `PATH`.
- **`.githooks/pre-push` auto-formats before every push** (enabled per-clone via `git config core.hooksPath .githooks`, not a Claude Code hook). Because git fixes the commit(s) to push before invoking the hook, a reformat can't be folded into the push already in flight — the hook commits the fix and aborts (exit 1) instead; the next `git push` then succeeds immediately since nothing is left to format. Don't try to make it "succeed on the first push" — that's not achievable with a pre-push hook.
