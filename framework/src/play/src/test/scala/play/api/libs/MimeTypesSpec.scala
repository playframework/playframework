/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import org.specs2.mutable._
import play.api.{ Configuration, Environment, Mode }

class MimeTypesSpec extends Specification {

  "Mime types" should {
    "choose the correct mime type for file with lowercase extension" in {
      val config = Configuration.load(Environment.simple(mode = Mode.Test))
      val mimeTypes = MimeTypes.fromConfiguration(config)
      mimeTypes.forFileName("image.png") must be equalTo Some("image/png")
    }
    "choose the correct mime type for file with uppercase extension" in {
      val config = Configuration.load(Environment.simple(mode = Mode.Test))
      val mimeTypes = MimeTypes.fromConfiguration(config)
      mimeTypes.forFileName("image.PNG") must be equalTo Some("image/png")
    }

    "choose JSON as text" in {
      val config = Configuration.load(Environment.simple(mode = Mode.Test))
      val mimeTypes = MimeTypes.fromConfiguration(config)
      mimeTypes.isText("application/json") must beTrue
    }

    "choose text/* as text" in {
      val config = Configuration.load(Environment.simple(mode = Mode.Test))
      val mimeTypes = MimeTypes.fromConfiguration(config)
      mimeTypes.isText("text/foo") must beTrue
    }

    "use additional types using syntax" in {
      val config = Configuration.load(Environment.simple(mode = Mode.Test)) ++ Configuration("mimetype.foo" -> "image/bar")
      val mimeTypes = MimeTypes.fromConfiguration(config)
      mimeTypes.forFileName("image.foo") must be equalTo Some("image/bar")
    }

  }

}

