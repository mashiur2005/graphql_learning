package com.example.productsearch.data

import com.example.productsearch.model.Product

object SampleData {

  val products: Seq[Product] = Seq(
    Product(
      id = Some(1),
      name = Some("Classic Oxford Shirt"),
      description = Some("Crisp cotton button-down shirt for everyday wear."),
      price = Some(39.99),
      category = Some("Shirt"),
      gender = Some("men"),
      sizes = Some(Seq("S", "M", "L", "XL")),
      colors = Some(Seq("White", "Light Blue")),
      brand = Some("Northgate"),
      inStock = Some(true)
    ),
    Product(
      id = Some(2),
      name = Some("Slim Fit Chinos"),
      description = Some("Tailored chino trousers with a comfortable stretch."),
      price = Some(49.5),
      category = Some("Trousers"),
      gender = Some("men"),
      sizes = Some(Seq("30", "32", "34", "36")),
      colors = Some(Seq("Khaki", "Navy", "Black")),
      brand = Some("Northgate"),
      inStock = Some(true)
    ),
    Product(
      id = Some(3),
      name = Some("Floral Wrap Dress"),
      description = Some("Lightweight wrap dress with a floral print, perfect for summer."),
      price = Some(59.99),
      category = Some("Dress"),
      gender = Some("women"),
      sizes = Some(Seq("XS", "S", "M", "L")),
      colors = Some(Seq("Floral Print")),
      brand = Some("Amelie & Co"),
      inStock = Some(true)
    ),
    Product(
      id = Some(4),
      name = Some("High-Waist Skinny Jeans"),
      description = Some("Stretch denim jeans with a flattering high-waist fit."),
      price = Some(54.0),
      category = Some("Jeans"),
      gender = Some("women"),
      sizes = Some(Seq("24", "26", "28", "30")),
      colors = Some(Seq("Dark Wash", "Black")),
      brand = Some("Amelie & Co"),
      inStock = Some(false)
    ),
    Product(
      id = Some(5),
      name = Some("Unisex Hooded Sweatshirt"),
      description = Some("Fleece-lined hoodie with a relaxed unisex fit."),
      price = Some(44.99),
      category = Some("Hoodie"),
      gender = Some("unisex"),
      sizes = Some(Seq("S", "M", "L", "XL", "XXL")),
      colors = Some(Seq("Heather Grey", "Black", "Maroon")),
      brand = Some("Streetform"),
      inStock = Some(true)
    ),
    Product(
      id = Some(6),
      name = Some("Merino Wool Sweater"),
      description = Some("Soft merino wool crewneck sweater for cooler days."),
      price = Some(69.0),
      category = Some("Sweater"),
      gender = Some("men"),
      sizes = Some(Seq("M", "L", "XL")),
      colors = Some(Seq("Charcoal", "Forest Green")),
      brand = Some("Northgate"),
      inStock = Some(true)
    ),
    Product(
      id = Some(7),
      name = Some("Pleated Midi Skirt"),
      description = Some("Elegant pleated midi skirt with a satin finish."),
      price = Some(47.25),
      category = Some("Skirt"),
      gender = Some("women"),
      sizes = Some(Seq("XS", "S", "M", "L")),
      colors = Some(Seq("Blush", "Ivory")),
      brand = Some("Amelie & Co"),
      inStock = Some(true)
    )
  )
}
