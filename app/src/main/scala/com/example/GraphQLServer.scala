package com.example

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http

import com.example.graphql.{GraphQLContext, GraphQLRoutes}

object GraphQLServer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("graphql-learning")
    import system.dispatcher

    val context = GraphQLContext()
    val route = GraphQLRoutes.route(context)

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println("GraphQL server running at http://localhost:8080/graphql")

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
    }
  }
}
