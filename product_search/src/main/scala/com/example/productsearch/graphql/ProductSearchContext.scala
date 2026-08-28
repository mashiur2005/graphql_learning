package com.example.productsearch.graphql

import co.elastic.clients.elasticsearch.ElasticsearchClient

import com.example.productsearch.model.Product
import com.example.productsearch.search.ProductSearchIndex

case class ProductSearchContext(client: ElasticsearchClient) {

  def allProducts(): Seq[Product] = ProductSearchIndex.all(client)

  def findProduct(id: Int): Option[Product] = ProductSearchIndex.findById(client, id)

  def searchProducts(queryText: String): Seq[Product] = ProductSearchIndex.search(client, queryText)
}
