package com.example.graphql

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import io.circe.{Json, JsonObject}
import io.circe.parser
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.circe._
import sangria.parser.{QueryParser, SyntaxError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GraphQLRoutes {

  def route(context: GraphQLContext)(implicit system: ActorSystem, ec: ExecutionContext): Route =
    path("graphql") {
      post {
        entity(as[String]) { body =>
          parser.parse(body) match {
            case Left(parsingError) =>
              complete(StatusCodes.BadRequest -> s"Invalid JSON: ${parsingError.message}")

            case Right(json) =>
              val cursor = json.hcursor
              val query = cursor.downField("query").as[String].getOrElse("")
              val operationName = cursor.downField("operationName").as[String].toOption
              val variables = cursor
                .downField("variables")
                .as[JsonObject]
                .map(Json.fromJsonObject)
                .getOrElse(Json.obj())

              QueryParser.parse(query) match {
                case Success(queryAst) =>
                  val result: Future[Json] = Executor
                    .execute(
                      schema = SchemaDefinition.schema,
                      queryAst = queryAst,
                      userContext = context,
                      operationName = operationName,
                      variables = variables
                    )
                    .recover {
                      case error: QueryAnalysisError => error.resolveError.asInstanceOf[Json]
                      case error: ErrorWithResolver => error.resolveError.asInstanceOf[Json]
                    }

                  onComplete(result) {
                    case Success(json) =>
                      complete(HttpEntity(ContentTypes.`application/json`, json.toString()))
                    case Failure(error) =>
                      complete(StatusCodes.InternalServerError -> error.getMessage)
                  }

                case Failure(error: SyntaxError) =>
                  complete(StatusCodes.BadRequest -> s"Invalid GraphQL query: ${error.getMessage}")
                case Failure(error) =>
                  complete(StatusCodes.InternalServerError -> error.getMessage)
              }
          }
        }
      }
    }
}
