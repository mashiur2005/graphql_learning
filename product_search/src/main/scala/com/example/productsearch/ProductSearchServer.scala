package com.example.productsearch

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http

import com.example.productsearch.data.SampleData
import com.example.productsearch.graphql.{ProductSearchContext, ProductSearchRoutes}
import com.example.productsearch.search.{ElasticsearchClientFactory, ProductSearchIndex}

object ProductSearchServer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("product-search")
    import system.dispatcher

    val client = ElasticsearchClientFactory.create()
    ProductSearchIndex.ensureIndex(client)
    ProductSearchIndex.indexAll(client, SampleData.products)

    val context = ProductSearchContext(client)
    val route = ProductSearchRoutes.route(context)

    val bindingFuture = Http().newServerAt("localhost", 8081).bind(route)
    println("Product search GraphQL server running at http://localhost:8081/graphql")

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete { _ =>
          client.close()
          system.terminate()
        }
    }
  }
}
