/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs

import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

import java.util.concurrent.TimeUnit;

object WSSpec extends Specification {
  "WS" should {
    "use queryString in url" in {
      val rep = WS.url("http://httpbin.org/get?foo=bar").get().get(2L, TimeUnit.SECONDS);

      rep.getStatus() must be equalTo(200)
      rep.asJson().path("args").path("foo").textValue() must be equalTo("bar")
    }
    "use user:password in url" in {
      val rep = WS.url("http://user:password@httpbin.org/basic-auth/user/password").get().get(2L, TimeUnit.SECONDS);

      rep.getStatus() must be equalTo(200)
      rep.asJson().path("authenticated").booleanValue() must beTrue
    }
  }
}
