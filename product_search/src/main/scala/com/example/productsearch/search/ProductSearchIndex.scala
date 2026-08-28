package com.example.productsearch.search

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import com.example.productsearch.model.Product

import scala.jdk.CollectionConverters._

object ProductSearchIndex {

  val indexName: String = "products"

  def ensureIndex(client: ElasticsearchClient): Unit = {
    val exists = client.indices().exists(ExistsRequest.of(e => e.index(indexName))).value()
    if (!exists) {
      client
        .indices()
        .create(c =>
          c.index(indexName)
            .mappings(m =>
              m.properties("id", p => p.integer(i => i))
                .properties("name", p => p.text(t => t))
                .properties("description", p => p.text(t => t))
                .properties("price", p => p.double_(d => d))
                .properties("category", p => p.keyword(k => k))
                .properties("gender", p => p.keyword(k => k))
                .properties("sizes", p => p.keyword(k => k))
                .properties("colors", p => p.keyword(k => k))
                .properties("brand", p => p.keyword(k => k))
                .properties("inStock", p => p.boolean_(b => b))
            )
        )
    }
  }

  def indexAll(client: ElasticsearchClient, products: Seq[Product]): Unit = {
    if (products.nonEmpty) {
      client.bulk(b => {
        products.foreach { product =>
          val operation = IndexOperation.of[Product](idx =>
            idx.index(indexName).id(product.id.map(_.toString).getOrElse("")).document(product)
          )
          b.operations(op => op.index(operation))
        }
        b
      })
      client.indices().refresh(r => r.index(indexName))
    }
  }

  def search(client: ElasticsearchClient, queryText: String): Seq[Product] = {
    val response = client.search(
      s =>
        s.index(indexName)
          .query(q => q.multiMatch(m => m.query(queryText).fields("name", "description", "category", "brand"))),
      classOf[Product]
    )
    response.hits().hits().asScala.flatMap(hit => Option(hit.source())).toSeq
  }
}
