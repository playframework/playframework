/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.libs

import play.it.tools.HttpBinApplication

import play.api.test._
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.IOException

object WSSpec extends PlaySpecification {

  sequential

  def app = HttpBinApplication.app

  def withServer[T](block: Port => T) = {
    val port = testServerPort
    running(TestServer(port, app)) {
      block(port)
    }
  }

  def withResult[T](result: Result)(block: Port => T) = {
    val port = testServerPort
    running(TestServer(port, FakeApplication(withRoutes = {
      case _ => Action(result)
    }))) {
      block(port)
    }
  }

  "WS@java" should {
    import play.libs.ws.WS
    import play.libs.ws._

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

    "reject invalid query string" in withServer { port =>
      import java.net.MalformedURLException

      WS.url("http://localhost/get?=&foo") must throwA[RuntimeException].like{
        case e: RuntimeException =>
          e.getCause must beAnInstanceOf[MalformedURLException]
      }
    }

    "reject invalid user password string" in withServer { port =>
      import java.net.MalformedURLException

      WS.url("http://@localhost/get") must throwA[RuntimeException].like{
        case e: RuntimeException =>
          e.getCause must beAnInstanceOf[MalformedURLException]
      }
    }

    "accept valid query string" in withServer { port =>
      var empty = WS.url(s"http://localhost:$port/get?foo").get.get(1000)
      var bar = WS.url(s"http://localhost:$port/get?foo=bar").get.get(1000)

      empty.asJson.path("args").path("foo").textValue() must equalTo("")
      bar.asJson.path("args").path("foo").textValue() must equalTo("bar")
    }


  }

  "WS@scala" should {
    import play.api.libs.ws.WS
    import play.api.Play.current

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

    "get a streamed response" in withResult(Results.Ok.chunked(Enumerator("a", "b", "c"))) { port =>
      val res = WS.url(s"http://localhost:$port/get").stream()
      val (_, body) = await(res)
      new String(await(body |>>> Iteratee.consume[Array[Byte]]()), "utf-8") must_== "abc"
    }

    def slow[E](ms: Long): Enumeratee[E, E] = Enumeratee.mapM { i => Promise.timeout(i, ms) }

    "get a streamed response when the server is slow" in withResult(
      Results.Ok.chunked(Enumerator("a", "b", "c") &> slow(50))
    ) { port =>
      val res = WS.url(s"http://localhost:$port/get").stream()
      val (_, body) = await(res)
      new String(await(body |>>> Iteratee.consume[Array[Byte]]()), "utf-8") must_== "abc"
    }

    "get a streamed response when the consumer is slow" in withResult(
      Results.Ok.chunked(Enumerator("a", "b", "c") &> slow(10))
    ) { port =>
      val res = WS.url(s"http://localhost:$port/get").stream()
      val (_, body) = await(res)
      new String(await(body &> slow(50) |>>> Iteratee.consume[Array[Byte]]()), "utf-8") must_== "abc"
    }

    "propogate errors from the stream" in withResult(
      Results.Ok.feed(Enumerator.unfold(0) {
        case i if i < 3 => Some((i + 1, "chunk".getBytes("utf-8")))
        case _ => throw new Exception()
      } &> slow(50)).withHeaders("Content-Length" -> "100000")
    ) { port =>
      val res = WS.url(s"http://localhost:$port/get").stream()
      val (_, body) = await(res)
      await(body |>>> Iteratee.consume[Array[Byte]]()) must throwAn[IOException]
    }

  }
}



