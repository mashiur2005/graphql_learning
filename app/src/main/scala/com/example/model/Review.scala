package com.example.model

case class Review(
    id: Option[Int],
    rating: Option[Int],
    content: Option[String],
    author_id: Option[Int],
    game_id: Option[Int]
)
