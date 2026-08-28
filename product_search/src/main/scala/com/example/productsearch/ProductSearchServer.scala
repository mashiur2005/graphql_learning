package com.example.productsearch

import com.example.productsearch.data.SampleData
import com.example.productsearch.search.{ElasticsearchClientFactory, ProductSearchIndex}

object ProductSearchServer {
  def main(args: Array[String]): Unit = {
    val client = ElasticsearchClientFactory.create()
    try {
      ProductSearchIndex.ensureIndex(client)
      ProductSearchIndex.indexAll(client, SampleData.products)

      val queryText = args.headOption.getOrElse("dress")
      val results = ProductSearchIndex.search(client, queryText)

      println(s"Search results for '$queryText':")
      results.foreach(product =>
        println(
          s"  - ${product.name.getOrElse("<unnamed>")} (${product.category.getOrElse("?")}, ${product.gender.getOrElse("?")})"
        )
      )
    } finally {
      client.close()
    }
  }
}
