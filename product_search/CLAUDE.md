# CLAUDE.md — `product_search`

Module-specific guidance for `product_search`. See the [root CLAUDE.md](../CLAUDE.md)
for repo-wide commands and conventions shared with `app`.

## What this module is

A garment product catalog (men's/women's/unisex) exposed over the same
Sangria + Pekko HTTP GraphQL stack as `app`, but backed by Elasticsearch
instead of an in-memory map. Read-only: there are `products`, `product(id)`,
and `searchProducts(query)` queries, no mutations. Runs on
`http://localhost:8081/graphql` (not 8080 — this lets both modules' servers
run side by side).

## Commands scoped to this module

```bash
docker compose -f product_search/docker-compose.yml up -d   # start local Elasticsearch (localhost:9200)
docker compose -f product_search/docker-compose.yml down    # stop it
./gradlew :product_search:build                              # compile + test + assemble this module only
./gradlew :product_search:test                                # run this module's tests (needs Docker — see below)
./gradlew run                                                 # NOTE: runs `app`'s server, not this one — see below
```

There is no root-level task that starts `ProductSearchServer` — the
`application` plugin's `run` task is only wired for `app` in this repo. To run
this module's server directly:

```bash
./gradlew :product_search:run
```

## Architecture

Request flow mirrors `app`'s: `ProductSearchServer` (binds Pekko HTTP on
`:8081`) → `ProductSearchRoutes.route` (same JSON-body-to-GraphQL-execution
shape as `app`'s `GraphQLRoutes.route`) → `ProductSchemaDefinition` (Sangria
`ObjectType`s) → `ProductSearchContext` (thin wrapper around an
`ElasticsearchClient`) → `ProductSearchIndex` (the actual Elasticsearch
queries).

Key design points a future change needs to respect:

- **This module has no in-memory data store.** `ProductSearchContext` just
  holds an `ElasticsearchClient` and delegates every read
  (`allProducts`/`findProduct`/`searchProducts`) to `ProductSearchIndex`,
  which issues real Elasticsearch requests (`search`, `get`, `bulk`). There is
  no `synchronized` mutable map like `app/GraphQLContext` — Elasticsearch is
  the store.
- **`ProductSearchServer.main` indexes `SampleData.products` into
  Elasticsearch on every startup** via `ProductSearchIndex.ensureIndex` (create
  the `products` index with an explicit mapping if it doesn't exist) followed
  by `ProductSearchIndex.indexAll` (bulk-index + refresh). There's no
  persistence layer beyond whatever the Elasticsearch container/volume
  retains — restarting the app re-indexes the same sample data, matching
  `app`'s "resets on restart" behavior even though the storage engine differs.
- **Every model field is `Option[...]`**, same convention as `app`'s models —
  `Product` (in `product_search/src/main/scala/com/example/productsearch/model/`)
  wraps every attribute, and `ProductSchemaDefinition` mirrors that with
  `OptionType(...)` wrappers. Follow the same convention for new fields.
- **`ElasticsearchClientFactory.create()` reads `ELASTICSEARCH_HOST` /
  `ELASTICSEARCH_PORT` env vars** (defaulting to `localhost:9200`), and also
  takes an explicit `(host, port)` overload used by tests to point at a
  Testcontainers-managed instance instead. Don't hardcode connection details
  elsewhere — go through this factory.
- **No mutations are defined** in `ProductSchemaDefinition` — only `products`,
  `product(id)`, and `searchProducts(query)` queries. If mutations are added
  later, follow `app`'s pattern of shared standalone `Argument[T]` vals rather
  than introducing `InputObjectType`s, to stay consistent across modules.
- **Tests are end-to-end against a real Elasticsearch, not mocks.**
  `ProductSearchRoutesSpec` starts a real Elasticsearch container via
  Testcontainers in `beforeAll`/`afterAll` (not the plain `context`/`route`
  vals pattern `app`'s `GraphQLRoutesSpec` uses), indexes `SampleData.products`
  into it, and exercises the route against that live index. Requires Docker;
  expect these tests to be slower than `app`'s in-memory ones. This is
  deliberate — it catches real Elasticsearch mapping/query issues that a
  mocked client would hide.
- **`product_search/docker-compose.yml`** is for local manual runs (`./gradlew
  :product_search:run` against a real Elasticsearch on `:9200`) — it is
  independent of the test suite, which spins up its own ephemeral container
  via Testcontainers and doesn't touch this compose file.
