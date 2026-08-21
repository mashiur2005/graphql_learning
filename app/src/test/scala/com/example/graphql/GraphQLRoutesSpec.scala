package com.example.graphql

import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import io.circe.Json
import io.circe.parser

@RunWith(classOf[JUnitRunner])
class GraphQLRoutesSpec extends AnyFunSuite with ScalatestRouteTest {

  private val context = GraphQLContext()
  private val route = GraphQLRoutes.route(context)

  private def graphqlRequest(query: String): HttpRequest =
    HttpRequest(
      method = HttpMethods.POST,
      uri = "/graphql",
      entity = HttpEntity(ContentTypes.`application/json`, Json.obj("query" -> Json.fromString(query)).noSpaces)
    )

  // Mutation tests get their own fresh context/route so they don't disturb the shared
  // `context`/`route` counts (3 games, 3 authors, 4 reviews) used by the query tests above.
  private def freshRoute: Route = GraphQLRoutes.route(new GraphQLContext())

  test("games query returns the sample games") {
    graphqlRequest("{ games { id title platform } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val games = json.hcursor.downField("data").downField("games").values.getOrElse(fail("no games field"))
      assert(games.size === 3)
      val titles = games.flatMap(_.hcursor.downField("title").as[String].toOption)
      assert(titles.exists(_ == "The Legend of Zelda: Breath of the Wild"))
    }
  }

  test("authors query returns the sample authors") {
    graphqlRequest("{ authors { id name verified } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val authors = json.hcursor.downField("data").downField("authors").values.getOrElse(fail("no authors field"))
      assert(authors.size === 3)
      val names = authors.flatMap(_.hcursor.downField("name").as[String].toOption)
      assert(names.exists(_ == "Alice Johnson"))
    }
  }

  test("reviews query returns the sample reviews") {
    graphqlRequest("{ reviews { id rating content author_id game_id } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val reviews = json.hcursor.downField("data").downField("reviews").values.getOrElse(fail("no reviews field"))
      assert(reviews.size === 4)
      val ratings = reviews.flatMap(_.hcursor.downField("rating").as[Int].toOption)
      assert(ratings.forall(r => r >= 1 && r <= 5))
    }
  }

  test("a query can request games, authors and reviews together") {
    graphqlRequest("{ games { id } authors { id } reviews { id } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val data = json.hcursor.downField("data")
      assert(data.downField("games").values.exists(_.size == 3))
      assert(data.downField("authors").values.exists(_.size == 3))
      assert(data.downField("reviews").values.exists(_.size == 4))
    }
  }

  test("game(id) returns the matching game") {
    graphqlRequest("{ game(id: 2) { id title platform } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val game = json.hcursor.downField("data").downField("game")
      assert(game.downField("id").as[Int].toOption.contains(2))
      assert(game.downField("title").as[String].toOption.contains("God of War"))
    }
  }

  test("game(id) returns null for an unknown id") {
    graphqlRequest("{ game(id: 999) { id } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("game").focus.contains(Json.Null))
    }
  }

  test("author(id) returns the matching author") {
    graphqlRequest("{ author(id: 1) { id name verified } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val author = json.hcursor.downField("data").downField("author")
      assert(author.downField("id").as[Int].toOption.contains(1))
      assert(author.downField("name").as[String].toOption.contains("Alice Johnson"))
    }
  }

  test("author(id) returns null for an unknown id") {
    graphqlRequest("{ author(id: 999) { id } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("author").focus.contains(Json.Null))
    }
  }

  test("review(id) returns the matching review") {
    graphqlRequest("{ review(id: 3) { id rating content author_id game_id } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val review = json.hcursor.downField("data").downField("review")
      assert(review.downField("id").as[Int].toOption.contains(3))
      assert(review.downField("game_id").as[Int].toOption.contains(3))
    }
  }

  test("review(id) returns null for an unknown id") {
    graphqlRequest("{ review(id: 999) { id } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("review").focus.contains(Json.Null))
    }
  }

  test("createGame adds a new game with an auto-generated id") {
    val r = freshRoute
    graphqlRequest(
      """mutation { createGame(title: "Elden Ring", platform: ["PC", "PS5"]) { id title platform } }"""
    ) ~> r ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val created = json.hcursor.downField("data").downField("createGame")
      assert(created.downField("id").as[Int].toOption.contains(4))
      assert(created.downField("title").as[String].toOption.contains("Elden Ring"))
    }

    graphqlRequest("{ games { id } }") ~> r ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("games").values.exists(_.size == 4))
    }
  }

  test("updateGame changes only the provided fields") {
    val r = freshRoute
    graphqlRequest("""mutation { updateGame(id: 1, title: "Zelda: BOTW Remastered") { id title platform } }""") ~> r ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val updated = json.hcursor.downField("data").downField("updateGame")
      assert(updated.downField("title").as[String].toOption.contains("Zelda: BOTW Remastered"))
      assert(updated.downField("platform").values.exists(_.size == 2))
    }
  }

  test("updateGame returns null for an unknown id") {
    graphqlRequest("mutation { updateGame(id: 999, title: \"Nope\") { id } }") ~> freshRoute ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("updateGame").focus.contains(Json.Null))
    }
  }

  test("deleteGame removes the game and returns true") {
    val r = freshRoute
    graphqlRequest("mutation { deleteGame(id: 1) }") ~> r ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("deleteGame").as[Boolean].contains(true))
    }

    graphqlRequest("{ game(id: 1) { id } games { id } }") ~> r ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("game").focus.contains(Json.Null))
      assert(json.hcursor.downField("data").downField("games").values.exists(_.size == 2))
    }
  }

  test("deleteGame returns false for an unknown id") {
    graphqlRequest("mutation { deleteGame(id: 999) }") ~> freshRoute ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("deleteGame").as[Boolean].contains(false))
    }
  }

  test("createAuthor adds a new author with an auto-generated id") {
    val r = freshRoute
    graphqlRequest("""mutation { createAuthor(name: "Dana Lee", verified: true) { id name verified } }""") ~> r ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val created = json.hcursor.downField("data").downField("createAuthor")
      assert(created.downField("id").as[Int].toOption.contains(4))
      assert(created.downField("name").as[String].toOption.contains("Dana Lee"))
    }
  }

  test("updateAuthor changes only the provided fields") {
    graphqlRequest("mutation { updateAuthor(id: 2, verified: true) { id name verified } }") ~> freshRoute ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val updated = json.hcursor.downField("data").downField("updateAuthor")
      assert(updated.downField("name").as[String].toOption.contains("Bob Smith"))
      assert(updated.downField("verified").as[Boolean].contains(true))
    }
  }

  test("deleteAuthor removes the author and returns true") {
    val r = freshRoute
    graphqlRequest("mutation { deleteAuthor(id: 3) }") ~> r ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("deleteAuthor").as[Boolean].contains(true))
    }

    graphqlRequest("{ author(id: 3) { id } }") ~> r ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("author").focus.contains(Json.Null))
    }
  }

  test("createReview adds a new review with an auto-generated id") {
    graphqlRequest(
      """mutation { createReview(rating: 5, content: "Loved it", author_id: 1, game_id: 2) { id rating content author_id game_id } }"""
    ) ~> freshRoute ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val created = json.hcursor.downField("data").downField("createReview")
      assert(created.downField("id").as[Int].toOption.contains(5))
      assert(created.downField("content").as[String].toOption.contains("Loved it"))
    }
  }

  test("updateReview changes only the provided fields") {
    graphqlRequest("mutation { updateReview(id: 1, rating: 1) { id rating content } }") ~> freshRoute ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val updated = json.hcursor.downField("data").downField("updateReview")
      assert(updated.downField("rating").as[Int].contains(1))
      assert(updated.downField("content").as[String].toOption.contains("A masterpiece of open-world design."))
    }
  }

  test("deleteReview removes the review and returns true") {
    val r = freshRoute
    graphqlRequest("mutation { deleteReview(id: 4) }") ~> r ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("deleteReview").as[Boolean].contains(true))
    }

    graphqlRequest("{ reviews { id } }") ~> r ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("reviews").values.exists(_.size == 3))
    }
  }

  test("an unknown field returns a GraphQL error instead of throwing") {
    graphqlRequest("{ unknownField }") ~> route ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("errors").succeeded)
    }
  }

  test("malformed GraphQL syntax returns 400 Bad Request") {
    graphqlRequest("{ games { ") ~> route ~> check {
      assert(status === StatusCodes.BadRequest)
    }
  }

  test("malformed JSON body returns 400 Bad Request") {
    HttpRequest(
      method = HttpMethods.POST,
      uri = "/graphql",
      entity = HttpEntity(ContentTypes.`application/json`, "not-json")
    ) ~> route ~> check {
      assert(status === StatusCodes.BadRequest)
    }
  }
}
