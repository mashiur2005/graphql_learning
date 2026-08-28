package com.example.productsearch.search

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.net.URI

object ElasticsearchClientFactory {

  private val host: String = sys.env.getOrElse("ELASTICSEARCH_HOST", "localhost")
  private val port: Int = sys.env.get("ELASTICSEARCH_PORT").map(_.toInt).getOrElse(9200)

  def create(): ElasticsearchClient = {
    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)

    val restClient = Rest5Client.builder(URI.create(s"http://$host:$port")).build()
    val transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper(objectMapper))
    new ElasticsearchClient(transport)
  }
}
