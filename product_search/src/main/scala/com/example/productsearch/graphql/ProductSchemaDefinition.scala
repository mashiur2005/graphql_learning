package com.example.productsearch.graphql

import sangria.schema._

import com.example.productsearch.model.Product

object ProductSchemaDefinition {

  val ProductType: ObjectType[Unit, Product] = ObjectType(
    "Product",
    "A garment product for men, women, or unisex",
    fields[Unit, Product](
      Field("id", OptionType(IntType), resolve = _.value.id),
      Field("name", OptionType(StringType), resolve = _.value.name),
      Field("description", OptionType(StringType), resolve = _.value.description),
      Field("price", OptionType(FloatType), resolve = _.value.price),
      Field("category", OptionType(StringType), resolve = _.value.category),
      Field("gender", OptionType(StringType), resolve = _.value.gender),
      Field("sizes", OptionType(ListType(StringType)), resolve = _.value.sizes),
      Field("colors", OptionType(ListType(StringType)), resolve = _.value.colors),
      Field("brand", OptionType(StringType), resolve = _.value.brand),
      Field("inStock", OptionType(BooleanType), resolve = _.value.inStock)
    )
  )

  val IdArg: Argument[Int] = Argument("id", IntType, description = "The product id to look up")
  val QueryArg: Argument[String] = Argument("query", StringType, description = "Free-text search query")

  val QueryType: ObjectType[ProductSearchContext, Unit] = ObjectType(
    "Query",
    fields[ProductSearchContext, Unit](
      Field("products", ListType(ProductType), resolve = _.ctx.allProducts()),
      Field(
        "product",
        OptionType(ProductType),
        arguments = IdArg :: Nil,
        resolve = c => c.ctx.findProduct(c.arg(IdArg))
      ),
      Field(
        "searchProducts",
        ListType(ProductType),
        arguments = QueryArg :: Nil,
        resolve = c => c.ctx.searchProducts(c.arg(QueryArg))
      )
    )
  )

  val schema: Schema[ProductSearchContext, Unit] = Schema(QueryType)
}
