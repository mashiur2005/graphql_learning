package com.example.productsearch.graphql

import co.elastic.clients.elasticsearch.ElasticsearchClient
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import io.circe.Json
import io.circe.parser

import com.example.productsearch.data.SampleData
import com.example.productsearch.search.{ElasticsearchClientFactory, ProductSearchIndex}

import java.time.Duration

// End-to-end: runs against a real Elasticsearch instance in a Testcontainers-managed
// container (requires Docker), not an in-memory fake, so setup is a beforeAll/afterAll
// rather than the plain vals app's GraphQLRoutesSpec uses.
@RunWith(classOf[JUnitRunner])
class ProductSearchRoutesSpec extends AnyFunSuite with ScalatestRouteTest with BeforeAndAfterAll {

  private val esContainer =
    new GenericContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.2"))
  esContainer.withExposedPorts(9200)
  esContainer.withEnv("discovery.type", "single-node")
  esContainer.withEnv("xpack.security.enabled", "false")
  esContainer.withEnv("xpack.security.http.ssl.enabled", "false")
  esContainer.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
  esContainer.waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)))

  private var client: ElasticsearchClient = _
  private var route: Route = _

  override def beforeAll(): Unit = {
    esContainer.start()
    client = ElasticsearchClientFactory.create(esContainer.getHost, esContainer.getMappedPort(9200))
    ProductSearchIndex.ensureIndex(client)
    ProductSearchIndex.indexAll(client, SampleData.products)
    route = ProductSearchRoutes.route(ProductSearchContext(client))
  }

  override def afterAll(): Unit = {
    if (client != null) client.close()
    esContainer.stop()
  }

  private def graphqlRequest(query: String): HttpRequest =
    HttpRequest(
      method = HttpMethods.POST,
      uri = "/graphql",
      entity = HttpEntity(ContentTypes.`application/json`, Json.obj("query" -> Json.fromString(query)).noSpaces)
    )

  test("products query returns all sample products") {
    graphqlRequest("{ products { id name category gender } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val products = json.hcursor.downField("data").downField("products").values.getOrElse(fail("no products field"))
      assert(products.size === 7)
      val names = products.flatMap(_.hcursor.downField("name").as[String].toOption)
      assert(names.exists(_ == "Floral Wrap Dress"))
    }
  }

  test("product(id) returns the matching product") {
    graphqlRequest("{ product(id: 4) { id name category gender price } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val product = json.hcursor.downField("data").downField("product")
      assert(product.downField("id").as[Int].toOption.contains(4))
      assert(product.downField("name").as[String].toOption.contains("High-Waist Skinny Jeans"))
      assert(product.downField("gender").as[String].toOption.contains("women"))
    }
  }

  test("product(id) returns null for an unknown id") {
    graphqlRequest("{ product(id: 999) { id } }") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("product").focus.contains(Json.Null))
    }
  }

  test("searchProducts finds products matching a free-text query") {
    graphqlRequest("""{ searchProducts(query: "jeans") { id name category } }""") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val results =
        json.hcursor.downField("data").downField("searchProducts").values.getOrElse(fail("no searchProducts field"))
      assert(results.size === 1)
      assert(results.head.hcursor.downField("name").as[String].toOption.contains("High-Waist Skinny Jeans"))
    }
  }

  test("searchProducts finds all products from a matching brand") {
    graphqlRequest("""{ searchProducts(query: "Northgate") { id name } }""") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      val results =
        json.hcursor.downField("data").downField("searchProducts").values.getOrElse(fail("no searchProducts field"))
      assert(results.size === 3)
    }
  }

  test("searchProducts returns an empty list when nothing matches") {
    graphqlRequest("""{ searchProducts(query: "nonexistent-garment-xyz") { id } }""") ~> route ~> check {
      assert(status === StatusCodes.OK)
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("data").downField("searchProducts").values.exists(_.isEmpty))
    }
  }

  test("an unknown field returns a GraphQL error instead of throwing") {
    graphqlRequest("{ unknownField }") ~> route ~> check {
      val json = parser.parse(responseAs[String]).getOrElse(fail("response was not valid JSON"))
      assert(json.hcursor.downField("errors").succeeded)
    }
  }

  test("malformed GraphQL syntax returns 400 Bad Request") {
    graphqlRequest("{ products { ") ~> route ~> check {
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
