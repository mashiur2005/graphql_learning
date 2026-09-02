# CLAUDE.md — `app`

Module-specific guidance for `app`. See the [root CLAUDE.md](../CLAUDE.md) for
repo-wide commands and conventions shared with `product_search`.

## What this module is

The original GraphQL API in this repo: `Game`, `Author`, and `Review` entities
served over Sangria + Pekko HTTP, backed by an in-memory, mutable, thread-safe
data store (no database). Data resets to the seeded `SampleData` on every
server restart. Runs on `http://localhost:8080/graphql`.

## Commands scoped to this module

```bash
./gradlew :app:build                                            # compile + test + assemble this module only
./gradlew :app:test                                              # run this module's tests
./gradlew test --tests "com.example.graphql.GraphQLRoutesSpec"   # run a single test class
./gradlew test --tests "*GraphQLRoutesSpec*createGame*"          # run a single test case by name pattern
```

`./gradlew run` (from the repo root) starts `app`'s server specifically —
`application.mainClass` in `app/build.gradle` points to `com.example.GraphQLServer`.

## Architecture

Request flow: `GraphQLServer` (binds Pekko HTTP on `:8080`) → `GraphQLRoutes.route`
(parses the JSON body into GraphQL `query`/`operationName`/`variables`, runs
`sangria.execution.Executor.execute` against `SchemaDefinition.schema`,
serializes the result with `sangria-circe`) → `SchemaDefinition` (Sangria
`ObjectType`s and field resolvers) → `GraphQLContext` (the actual data + CRUD logic).

Key design points a future change needs to respect:

- **`GraphQLContext` is the single source of truth and the Sangria `userContext`.**
  It wraps three `mutable.LinkedHashMap[Int, T]` (games/authors/reviews) guarded
  by `synchronized` blocks, with auto-incrementing ids (`max(existing ids) + 1`).
  `GraphQLRoutes.route` and `SchemaDefinition`'s resolvers are pure/stateless —
  all mutation and lookup logic (`createX`/`updateX`/`deleteX`/`findX`) lives on
  `GraphQLContext`, not in the schema or route layer. One `GraphQLContext`
  instance is created in `GraphQLServer.main` and lives for the process
  lifetime, shared across all requests.
- **Update mutations are partial/PATCH-style, not full replacement.** Each
  `updateX` method takes `Option[...]` args and does
  `argValue.orElse(existing.value)` per field — an omitted GraphQL argument
  leaves that field unchanged rather than nulling it out. There's no way to
  explicitly clear a field to `null` via update (an intentional simplification).
- **Every model field is `Option[...]`.** `Game`, `Author`, `Review` (in
  `app/src/main/scala/com/example/model/`) wrap every attribute in `Option`,
  including `Game.platform: Option[Seq[String]]`. Schema field types mirror
  this with `OptionType(...)` / `OptionInputType(...)` wrappers in
  `SchemaDefinition`. New fields should follow the same all-`Option`
  convention rather than mixing in required fields.
- **`Review` fields use snake_case (`author_id`, `game_id`)** in both the case
  class and the GraphQL schema — this is deliberate, not an oversight; don't
  camelCase it without checking whether the client-facing API contract matters.
- **Sangria arguments are shared, standalone `Argument[T]` vals**, not
  per-mutation input object types (e.g. `IdArg`, `TitleArg`, `PlatformArg` in
  `SchemaDefinition` are reused across the `game`/`updateGame`/`deleteGame`
  fields). Follow this pattern for new fields rather than introducing
  `InputObjectType`s.
- **`GraphQLRoutes.route` is a plain function `(GraphQLContext) => Route`**,
  deliberately separated from `GraphQLServer` so it can be exercised with
  `pekko-http-testkit`'s `ScalatestRouteTest` without binding a real socket
  (see `GraphQLRoutesSpec`). Keep new route logic in `GraphQLRoutes`, not
  inlined into `GraphQLServer.main`.
- **Sangria errors are caught and turned into GraphQL error payloads, not HTTP
  error codes.** `QueryAnalysisError`/`ErrorWithResolver` are recovered into a
  Circe `Json` error body with HTTP 200; only a JSON-parse failure or a
  GraphQL syntax error (`SyntaxError` from `QueryParser.parse`) produces an
  HTTP 400.
- **`app/src/main/scala/com/example/App.scala`** is the original
  Gradle-init-generated "Hello, world!" sample and is unused by the running
  application (`application.mainClass` in `app/build.gradle` points to
  `com.example.GraphQLServer`). Its test (`AppSuite.scala`) has been removed;
  the file itself is untested but harmless dead code.
- Tests that assert exact counts against the shared sample data (e.g. "3
  games") use the class-level `context`/`route` in `GraphQLRoutesSpec`;
  mutation tests must use `freshRoute` (a new `GraphQLContext()` per call) to
  avoid mutating shared state and breaking those count assertions.
