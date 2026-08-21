# graphql_learning

A small Scala + GraphQL learning project. It exposes a GraphQL API (built with
[Sangria](https://sangria-graphql.github.io/) and [Apache Pekko HTTP](https://pekko.apache.org/))
for `Game`, `Author`, and `Review` entities, backed by an in-memory data store
seeded with sample data.

## Tech stack

- Scala 2.13.14
- Gradle (via the included wrapper)
- [Sangria](https://sangria-graphql.github.io/) — GraphQL schema & execution
- [Apache Pekko HTTP](https://pekko.apache.org/) — HTTP server
- [Circe](https://circe.github.io/circe/) — JSON marshalling
- ScalaTest — test framework

## Prerequisites

- JDK 17 (a Gradle toolchain will otherwise try to provision one automatically)
- No local Gradle or Scala install needed — the Gradle wrapper (`./gradlew`) downloads everything else

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

See [`curl-commands.sh`](curl-commands.sh) for a full runnable set of example
requests covering every query, mutation, and error case:

```bash
./gradlew run &          # start the server in the background
./curl-commands.sh        # exercise every endpoint
```

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
├── AppSuite.scala
└── graphql/GraphQLRoutesSpec.scala   # route-level tests for every query/mutation
```
