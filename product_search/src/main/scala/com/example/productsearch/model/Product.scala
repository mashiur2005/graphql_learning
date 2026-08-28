package com.example.productsearch.model

case class Product(
    id: Option[Int],
    name: Option[String],
    description: Option[String],
    price: Option[Double],
    category: Option[String],
    gender: Option[String],
    sizes: Option[Seq[String]],
    colors: Option[Seq[String]],
    brand: Option[String],
    inStock: Option[Boolean]
)
