/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import org.specs2.mutable._
import play.api.http.{ DefaultFileMimeTypesProvider, FileMimeTypesConfiguration }

class FileMimeTypesSpec extends Specification {

  "Mime types" should {
    "choose the correct mime type for file with lowercase extension" in {
      val mimeTypes = new DefaultFileMimeTypesProvider(FileMimeTypesConfiguration(Map("png" -> "image/png"))).get
      mimeTypes.forFileName("image.png") must be equalTo Some("image/png")
    }
    "choose the correct mime type for file with uppercase extension" in {
      val mimeTypes = new DefaultFileMimeTypesProvider(FileMimeTypesConfiguration(Map("png" -> "image/png"))).get
      mimeTypes.forFileName("image.PNG") must be equalTo Some("image/png")
    }
  }

}

