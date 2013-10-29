/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.libs

import play.it.tools.HttpBinApplication

import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration._



object WSSpec extends PlaySpecification {

  val app = HttpBinApplication.app

  def withServer[T](block: Port => T) = {
    val port = testServerPort
    running(TestServer(port, app)) {
      block(port)
    }
  }

  "WS@java" should {
    import play.libs.WS

    "make GET Requests" in withServer { port =>
      val req = WS.url(s"http://localhost:$port/get").get
      val rep = req.get(1000) // AWait result

      rep.getStatus must be equalTo(200)
      rep.asJson.path("origin").textValue must not beNull
    }

    "use queryString in url" in withServer { port =>
      val rep = WS.url(s"http://localhost:$port/get?foo=bar").get().get(1000)

      rep.getStatus() must be equalTo(200)
      rep.asJson().path("args").path("foo").textValue() must be equalTo("bar")
    }

    "use user:password in url" in withServer { port =>
      val rep = WS.url(s"http://user:password@localhost:$port/basic-auth/user/password").get().get(1000)

      rep.getStatus() must be equalTo(200)
      rep.asJson().path("authenticated").booleanValue() must beTrue
    }

    "reject invalid query string" in {
      import java.net.MalformedURLException

      WS.url("http://localhost/get?=&foo") must throwA[RuntimeException].like{
        case e: RuntimeException => e.getCause must beAnInstanceOf[MalformedURLException]
      }
    }

    "reject invalid user password string" in {
      import java.net.MalformedURLException

      WS.url("http://@localhost/get") must throwA[RuntimeException].like{
        case e: RuntimeException =>
          e.getCause must beAnInstanceOf[MalformedURLException]
      }
    }

    "accept valid query string" in {
      import java.net.MalformedURLException

      WS.url("http://localhost/get?foo") must beAnInstanceOf[WS.WSRequestHolder]
      WS.url("http://localhost/get?foo=bar") must beAnInstanceOf[WS.WSRequestHolder]
    }


  }

  "WS@scala" should {
    import play.api.libs.ws.WS

    "make GET Requests" in withServer { port =>
      val req = WS.url(s"http://localhost:$port/get").get

      val rep = Await.result(req, Duration(1, SECONDS))

      rep.status must be equalTo(200)
    }

    "Get 404 errors" in withServer { port =>
      val req = WS.url(s"http://localhost:$port/post").get

      val rep = Await.result(req, Duration(1, SECONDS))

      rep.status must be equalTo(404)
    }

  }
}



