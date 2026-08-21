#!/usr/bin/env bash
# Sample curl commands for manually exercising the GraphQL endpoint.
# Start the server first: ./gradlew run
# Endpoint: POST http://localhost:8080/graphql

echo "== List all games =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ games { id title platform } }"}'
echo -e "\n"

echo "== List all authors =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ authors { id name verified } }"}'
echo -e "\n"

echo "== List all reviews =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ reviews { id rating content author_id game_id } }"}'
echo -e "\n"

echo "== Find game by id =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ game(id: 2) { id title platform } }"}'
echo -e "\n"

echo "== Find author by id =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ author(id: 1) { id name verified } }"}'
echo -e "\n"

echo "== Find review by id =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ review(id: 3) { id rating content author_id game_id } }"}'
echo -e "\n"

echo "== Find game by unknown id (expect null) =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ game(id: 999) { id title } }"}'
echo -e "\n"

echo "== Create a game =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createGame(title: \"Elden Ring\", platform: [\"PC\", \"PS5\"]) { id title platform } }"}'
echo -e "\n"

echo "== Update a game (only provided fields change) =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { updateGame(id: 1, title: \"Zelda: BOTW Remastered\") { id title platform } }"}'
echo -e "\n"

echo "== Delete a game =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { deleteGame(id: 1) }"}'
echo -e "\n"

echo "== Create an author =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createAuthor(name: \"Dana Lee\", verified: true) { id name verified } }"}'
echo -e "\n"

echo "== Update an author =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { updateAuthor(id: 2, verified: true) { id name verified } }"}'
echo -e "\n"

echo "== Delete an author =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { deleteAuthor(id: 2) }"}'
echo -e "\n"

echo "== Create a review =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { createReview(rating: 5, content: \"Loved it\", author_id: 1, game_id: 2) { id rating content author_id game_id } }"}'
echo -e "\n"

echo "== Update a review =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { updateReview(id: 1, rating: 1) { id rating content } }"}'
echo -e "\n"

echo "== Delete a review =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"mutation { deleteReview(id: 1) }"}'
echo -e "\n"

echo "== All three in one query =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ games { id title } authors { id name } reviews { id rating } }"}'
echo -e "\n"

echo "== Error case: unknown field =="
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ unknownField }"}'
echo -e "\n"

echo "== Error case: malformed GraphQL syntax (expect 400) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ games { "}'

echo "== Error case: malformed JSON body (expect 400) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d 'not-json'
