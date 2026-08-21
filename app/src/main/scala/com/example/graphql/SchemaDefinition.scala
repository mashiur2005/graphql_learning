package com.example.graphql

import sangria.schema._
import com.example.model.{Author, Game, Review}

object SchemaDefinition {

  val GameType: ObjectType[Unit, Game] = ObjectType(
    "Game",
    "A video game",
    fields[Unit, Game](
      Field("id", OptionType(IntType), resolve = _.value.id),
      Field("title", OptionType(StringType), resolve = _.value.title),
      Field("platform", OptionType(ListType(StringType)), resolve = _.value.platform)
    )
  )

  val AuthorType: ObjectType[Unit, Author] = ObjectType(
    "Author",
    "An author who writes game reviews",
    fields[Unit, Author](
      Field("id", OptionType(IntType), resolve = _.value.id),
      Field("name", OptionType(StringType), resolve = _.value.name),
      Field("verified", OptionType(BooleanType), resolve = _.value.verified)
    )
  )

  val ReviewType: ObjectType[Unit, Review] = ObjectType(
    "Review",
    "A review of a game written by an author",
    fields[Unit, Review](
      Field("id", OptionType(IntType), resolve = _.value.id),
      Field("rating", OptionType(IntType), resolve = _.value.rating),
      Field("content", OptionType(StringType), resolve = _.value.content),
      Field("author_id", OptionType(IntType), resolve = _.value.author_id),
      Field("game_id", OptionType(IntType), resolve = _.value.game_id)
    )
  )

  val IdArg: Argument[Int] = Argument("id", IntType, description = "The id to search for")

  val TitleArg: Argument[Option[String]] = Argument("title", OptionInputType(StringType))
  val PlatformArg: Argument[Option[Seq[String]]] =
    Argument("platform", OptionInputType(ListInputType(StringType)))

  val NameArg: Argument[Option[String]] = Argument("name", OptionInputType(StringType))
  val VerifiedArg: Argument[Option[Boolean]] = Argument("verified", OptionInputType(BooleanType))

  val RatingArg: Argument[Option[Int]] = Argument("rating", OptionInputType(IntType))
  val ContentArg: Argument[Option[String]] = Argument("content", OptionInputType(StringType))
  val AuthorIdArg: Argument[Option[Int]] = Argument("author_id", OptionInputType(IntType))
  val GameIdArg: Argument[Option[Int]] = Argument("game_id", OptionInputType(IntType))

  val QueryType: ObjectType[GraphQLContext, Unit] = ObjectType(
    "Query",
    fields[GraphQLContext, Unit](
      Field("games", ListType(GameType), resolve = _.ctx.games),
      Field("authors", ListType(AuthorType), resolve = _.ctx.authors),
      Field("reviews", ListType(ReviewType), resolve = _.ctx.reviews),
      Field(
        "game",
        OptionType(GameType),
        arguments = IdArg :: Nil,
        resolve = c => c.ctx.findGame(c.arg(IdArg))
      ),
      Field(
        "author",
        OptionType(AuthorType),
        arguments = IdArg :: Nil,
        resolve = c => c.ctx.findAuthor(c.arg(IdArg))
      ),
      Field(
        "review",
        OptionType(ReviewType),
        arguments = IdArg :: Nil,
        resolve = c => c.ctx.findReview(c.arg(IdArg))
      )
    )
  )

  val MutationType: ObjectType[GraphQLContext, Unit] = ObjectType(
    "Mutation",
    fields[GraphQLContext, Unit](
      Field(
        "createGame",
        GameType,
        arguments = TitleArg :: PlatformArg :: Nil,
        resolve = c => c.ctx.createGame(c.arg(TitleArg), c.arg(PlatformArg))
      ),
      Field(
        "updateGame",
        OptionType(GameType),
        arguments = IdArg :: TitleArg :: PlatformArg :: Nil,
        resolve = c => c.ctx.updateGame(c.arg(IdArg), c.arg(TitleArg), c.arg(PlatformArg))
      ),
      Field(
        "deleteGame",
        BooleanType,
        arguments = IdArg :: Nil,
        resolve = c => c.ctx.deleteGame(c.arg(IdArg))
      ),
      Field(
        "createAuthor",
        AuthorType,
        arguments = NameArg :: VerifiedArg :: Nil,
        resolve = c => c.ctx.createAuthor(c.arg(NameArg), c.arg(VerifiedArg))
      ),
      Field(
        "updateAuthor",
        OptionType(AuthorType),
        arguments = IdArg :: NameArg :: VerifiedArg :: Nil,
        resolve = c => c.ctx.updateAuthor(c.arg(IdArg), c.arg(NameArg), c.arg(VerifiedArg))
      ),
      Field(
        "deleteAuthor",
        BooleanType,
        arguments = IdArg :: Nil,
        resolve = c => c.ctx.deleteAuthor(c.arg(IdArg))
      ),
      Field(
        "createReview",
        ReviewType,
        arguments = RatingArg :: ContentArg :: AuthorIdArg :: GameIdArg :: Nil,
        resolve = c => c.ctx.createReview(c.arg(RatingArg), c.arg(ContentArg), c.arg(AuthorIdArg), c.arg(GameIdArg))
      ),
      Field(
        "updateReview",
        OptionType(ReviewType),
        arguments = IdArg :: RatingArg :: ContentArg :: AuthorIdArg :: GameIdArg :: Nil,
        resolve = c =>
          c.ctx.updateReview(c.arg(IdArg), c.arg(RatingArg), c.arg(ContentArg), c.arg(AuthorIdArg), c.arg(GameIdArg))
      ),
      Field(
        "deleteReview",
        BooleanType,
        arguments = IdArg :: Nil,
        resolve = c => c.ctx.deleteReview(c.arg(IdArg))
      )
    )
  )

  val schema: Schema[GraphQLContext, Unit] = Schema(QueryType, Some(MutationType))
}
