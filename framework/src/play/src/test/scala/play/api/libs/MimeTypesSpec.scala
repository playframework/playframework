/*
 * Copyright (C) 2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import org.specs2.mutable._

object MimeTypesSpec extends Specification {

  "Mime types" should {
    "choose the correct mime type for file with lowercase extension" in {
      MimeTypes.forFileName("image.png") must be equalTo Some("image/png")
    }
    "choose the correct mime type for file with uppercase extension" in {
      MimeTypes.forFileName("image.PNG") must be equalTo Some("image/png")
    }
  }

}

