/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationWriteSpec extends Specification {

  //#write-first-ex
  import play.api.data.mapping._
  def floatToString: Write[Float, String] = ???
  //#write-first-ex

  //#write-custom
  val currency = Write[Double, String]{ money =>
    import java.text.NumberFormat
    import java.util.Locale
    val f = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    f.format(money)
  }
  //#write-custom

  "Scala Validation Write" should {

    "test default Writes" in {
      //#write-first-defaults
      import play.api.data.mapping.Writes

      Writes.anyval.writes(12.8F) === "12.8"
      Writes.anyval.writes(12F) === "12.0"
      //#write-first-defaults
    }

    "define custom Write" in {
      //#write-custom-test
      currency.writes(9.99) === "9,99 €"
      //#write-custom-test
    }

    "compose Write" in {
      //#write-product
      case class Product(name: String, price: Double)
      //#write-product

      //#write-product-price
      val productPrice = Write[Product, Double]{ _.price }
      //#write-product-price

      //#write-product-asprice
      val productAsPrice: Write[Product,String] = productPrice compose currency
      //#write-product-asprice

      //#write-product-asprice-test
      productAsPrice.writes(Product("Awesome product", 9.99)) === "9,99 €"
      //#write-product-asprice-test
    }

  }
}
