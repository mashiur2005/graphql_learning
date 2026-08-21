package com.example.data

import com.example.model.{Author, Game, Review}

object SampleData {

  val games: Seq[Game] = Seq(
    Game(
      id = Some(1),
      title = Some("The Legend of Zelda: Breath of the Wild"),
      platform = Some(Seq("Switch", "Wii U"))
    ),
    Game(
      id = Some(2),
      title = Some("God of War"),
      platform = Some(Seq("PS4", "PS5", "PC"))
    ),
    Game(
      id = Some(3),
      title = Some("Hollow Knight"),
      platform = Some(Seq("PC", "Switch", "PS4", "Xbox One"))
    )
  )

  val authors: Seq[Author] = Seq(
    Author(id = Some(1), name = Some("Alice Johnson"), verified = Some(true)),
    Author(id = Some(2), name = Some("Bob Smith"), verified = Some(false)),
    Author(id = Some(3), name = Some("Carol Diaz"), verified = Some(true))
  )

  val reviews: Seq[Review] = Seq(
    Review(
      id = Some(1),
      rating = Some(5),
      content = Some("A masterpiece of open-world design."),
      author_id = Some(1),
      game_id = Some(1)
    ),
    Review(
      id = Some(2),
      rating = Some(4),
      content = Some("Great combat, emotional story."),
      author_id = Some(2),
      game_id = Some(2)
    ),
    Review(
      id = Some(3),
      rating = Some(5),
      content = Some("Tight platforming and beautiful art style."),
      author_id = Some(3),
      game_id = Some(3)
    ),
    Review(
      id = Some(4),
      rating = Some(3),
      content = Some("Good but repetitive shrines."),
      author_id = Some(2),
      game_id = Some(1)
    )
  )
}
