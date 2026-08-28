# graphql_learning

A small Scala + GraphQL learning project. It's a multi-module Gradle build:

- **`app`** — a GraphQL API (built with [Sangria](https://sangria-graphql.github.io/)
  and [Apache Pekko HTTP](https://pekko.apache.org/)) for `Game`, `Author`, and
  `Review` entities, backed by an in-memory data store seeded with sample data.
- **`product_search`** — a garment product catalog (men's/women's/unisex) indexed
  into Elasticsearch for full-text search.

## Tech stack

- Scala 2.13.14
- Gradle (via the included wrapper)
- [Sangria](https://sangria-graphql.github.io/) — GraphQL schema & execution
- [Apache Pekko HTTP](https://pekko.apache.org/) — HTTP server
- [Circe](https://circe.github.io/circe/) — JSON marshalling
- [Elasticsearch](https://www.elastic.co/elasticsearch) (official Java client) — product search, in `product_search`
- ScalaTest — test framework

## Prerequisites

- JDK 17 (a Gradle toolchain will otherwise try to provision one automatically)
- No local Gradle or Scala install needed — the Gradle wrapper (`./gradlew`) downloads everything else
- Docker (to run Elasticsearch locally via `docker-compose.yml`, needed for `product_search`)

## Setup

```bash
git clone https://github.com/mashiur2005/graphql_learning.git
cd graphql_learning
```

## Build

```bash
./gradlew build
```

## Run the server

```bash
./gradlew run
```

This starts the GraphQL server at:

```
http://localhost:8080/graphql
```

Stop it with `Ctrl+C`.

> The data store is in-memory and resets to the sample data every time the server restarts.

## Run the tests

```bash
./gradlew test
```

Test reports are written to `app/build/reports/tests/test/index.html`.

## Using the API

Send a `POST` request to `/graphql` with a JSON body containing a `query` field
(and optionally `variables` / `operationName`).

### Queries

List all games, authors, or reviews:

```bash
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ games { id title platform } }"}'

curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ authors { id name verified } }"}'

curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ reviews { id rating content author_id game_id } }"}'
```

Look up a single record by id (returns `null` if not found):

```bash
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ game(id: 1) { id title platform } }"}'

curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ author(id: 1) { id name verified } }"}'

curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ review(id: 1) { id rating content author_id game_id } }"}'
```

### Mutations

Create, update, and delete are available for all three entities. Updates only
change the fields you pass — anything omitted keeps its current value.

```bash
# Create
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createGame(title: \"Elden Ring\", platform: [\"PC\", \"PS5\"]) { id title platform } }"}'

# Update (only title changes; platform is left as-is)
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { updateGame(id: 1, title: \"Zelda: BOTW Remastered\") { id title platform } }"}'

# Delete (returns true/false)
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { deleteGame(id: 1) }"}'
```

The equivalent mutations exist for authors (`createAuthor`, `updateAuthor`,
`deleteAuthor`) and reviews (`createReview`, `updateReview`, `deleteReview`).

See [`app/curl-commands.sh`](app/curl-commands.sh) for a full runnable set of example
requests covering every query, mutation, and error case:

```bash
./gradlew run &                # start the server in the background
./app/curl-commands.sh         # exercise every endpoint
```

## Product search (Elasticsearch)

The `product_search` module indexes garment products (men's/women's/unisex)
into Elasticsearch and searches them with a `multi_match` query over
`name`/`description`/`category`/`brand`.

Start a local single-node Elasticsearch cluster:

```bash
docker compose up -d      # Elasticsearch 9.5.2 on http://localhost:9200, security disabled (dev only)
```

Then index the sample products and run a search (the first CLI arg is the
search text, defaulting to `"dress"`):

```bash
./gradlew :product_search:run --args="jeans"
```

`ELASTICSEARCH_HOST` / `ELASTICSEARCH_PORT` env vars override the default
`localhost:9200` target.

## Project structure

```
app/src/main/scala/com/example/
├── App.scala                     # generated "Hello, world!" sample entry point
├── GraphQLServer.scala           # binds the Pekko HTTP server on :8080
├── data/SampleData.scala         # seed data for games, authors, reviews
├── graphql/
│   ├── GraphQLContext.scala      # in-memory, thread-safe data store + CRUD ops
│   ├── SchemaDefinition.scala    # Sangria schema: Query and Mutation types
│   └── GraphQLRoutes.scala       # HTTP route: parses requests, executes queries
└── model/
    ├── Game.scala
    ├── Author.scala
    └── Review.scala

app/src/test/scala/com/example/
└── graphql/GraphQLRoutesSpec.scala   # route-level tests for every query/mutation

product_search/src/main/scala/com/example/productsearch/
├── ProductSearchServer.scala         # indexes sample data, runs a demo search
├── data/SampleData.scala             # seed data for garment products
├── model/Product.scala
└── search/
    ├── ElasticsearchClientFactory.scala   # builds the ElasticsearchClient
    └── ProductSearchIndex.scala           # index creation, bulk indexing, search
```

## Contributing

See [`CLAUDE.md`](CLAUDE.md) for build/test commands and the architecture
notes a future change should respect.

`git push` in this repo is gated by a `.claude/hooks` check that blocks the
push if source files changed since README.md was last updated and README.md
isn't part of the current push. If you hit that, update this README (or ask
Claude to run the `update-readme` skill) and push again.

### Code formatting

Scala sources are formatted with [scalafmt](https://scalameta.org/scalafmt/)
(config in `.scalafmt.conf`). Requires the `scalafmt` CLI on your `PATH`
(`cs install scalafmt` via [Coursier](https://get-coursier.io/)).

```bash
./gradlew scalafmtCheck   # fails if anything is unformatted
./gradlew scalafmtApply   # reformats in place
```

A `pre-push` git hook auto-formats before every push. Since git fixes the
commit(s) to be pushed before the hook runs, a reformat can't be folded into
that same push — if scalafmt finds anything to fix, the hook commits the fix
and aborts; just run `git push` again and it will go through. Enable it once
per clone:

```bash
git config core.hooksPath .githooks
```
