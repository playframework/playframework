/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws

import org.specs2.mutable.Specification
import play.api.test.WithApplication

class WSRequestSpec extends Specification {

  "WSRequestHolder" should {

    "give the full URL with the query string" in new WithApplication() {

      val ws = app.injector.instanceOf[WSClient]

      ws.url("http://foo.com").uri.toString must equalTo("http://foo.com")

      ws.url("http://foo.com").withQueryString("bar" -> "baz").uri.toString must equalTo("http://foo.com?bar=baz")

      ws.url("http://foo.com").withQueryString("bar" -> "baz", "bar" -> "bah").uri.toString must equalTo("http://foo.com?bar=bah&bar=baz")

    }

    "correctly URL-encode the query string part" in new WithApplication() {

      val ws = app.injector.instanceOf[WSClient]

      ws.url("http://foo.com").withQueryString("&" -> "=").uri.toString must equalTo("http://foo.com?%26=%3D")

    }

  }

}
