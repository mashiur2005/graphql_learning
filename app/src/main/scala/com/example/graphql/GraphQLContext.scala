package com.example.graphql

import com.example.data.SampleData
import com.example.model.{Author, Game, Review}

import scala.collection.mutable

class GraphQLContext(
    initialGames: Seq[Game] = SampleData.games,
    initialAuthors: Seq[Author] = SampleData.authors,
    initialReviews: Seq[Review] = SampleData.reviews
) {

  private val gamesById = mutable.LinkedHashMap(initialGames.flatMap(g => g.id.map(_ -> g)): _*)
  private val authorsById = mutable.LinkedHashMap(initialAuthors.flatMap(a => a.id.map(_ -> a)): _*)
  private val reviewsById = mutable.LinkedHashMap(initialReviews.flatMap(r => r.id.map(_ -> r)): _*)

  def games: Seq[Game] = synchronized(gamesById.values.toSeq)
  def authors: Seq[Author] = synchronized(authorsById.values.toSeq)
  def reviews: Seq[Review] = synchronized(reviewsById.values.toSeq)

  def findGame(id: Int): Option[Game] = synchronized(gamesById.get(id))
  def findAuthor(id: Int): Option[Author] = synchronized(authorsById.get(id))
  def findReview(id: Int): Option[Review] = synchronized(reviewsById.get(id))

  def createGame(title: Option[String], platform: Option[Seq[String]]): Game = synchronized {
    val id = nextId(gamesById.keys)
    val game = Game(Some(id), title, platform)
    gamesById += id -> game
    game
  }

  def updateGame(id: Int, title: Option[String], platform: Option[Seq[String]]): Option[Game] = synchronized {
    gamesById.get(id).map { existing =>
      val updated = existing.copy(title = title.orElse(existing.title), platform = platform.orElse(existing.platform))
      gamesById += id -> updated
      updated
    }
  }

  def deleteGame(id: Int): Boolean = synchronized(gamesById.remove(id).isDefined)

  def createAuthor(name: Option[String], verified: Option[Boolean]): Author = synchronized {
    val id = nextId(authorsById.keys)
    val author = Author(Some(id), name, verified)
    authorsById += id -> author
    author
  }

  def updateAuthor(id: Int, name: Option[String], verified: Option[Boolean]): Option[Author] = synchronized {
    authorsById.get(id).map { existing =>
      val updated = existing.copy(name = name.orElse(existing.name), verified = verified.orElse(existing.verified))
      authorsById += id -> updated
      updated
    }
  }

  def deleteAuthor(id: Int): Boolean = synchronized(authorsById.remove(id).isDefined)

  def createReview(
      rating: Option[Int],
      content: Option[String],
      authorId: Option[Int],
      gameId: Option[Int]
  ): Review = synchronized {
    val id = nextId(reviewsById.keys)
    val review = Review(Some(id), rating, content, authorId, gameId)
    reviewsById += id -> review
    review
  }

  def updateReview(
      id: Int,
      rating: Option[Int],
      content: Option[String],
      authorId: Option[Int],
      gameId: Option[Int]
  ): Option[Review] = synchronized {
    reviewsById.get(id).map { existing =>
      val updated = existing.copy(
        rating = rating.orElse(existing.rating),
        content = content.orElse(existing.content),
        author_id = authorId.orElse(existing.author_id),
        game_id = gameId.orElse(existing.game_id)
      )
      reviewsById += id -> updated
      updated
    }
  }

  def deleteReview(id: Int): Boolean = synchronized(reviewsById.remove(id).isDefined)

  private def nextId(ids: Iterable[Int]): Int = if (ids.isEmpty) 1 else ids.max + 1
}

object GraphQLContext {
  def apply(): GraphQLContext = new GraphQLContext()
}
