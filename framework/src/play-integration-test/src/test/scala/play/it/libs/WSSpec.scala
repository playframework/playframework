/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.libs

import com.ning.http.client.{ RequestBuilderBase, SignatureCalculator }
import play.api.libs.oauth._
import play.it.tools.HttpBinApplication

import play.api.test._
import play.api.mvc._
import play.it._
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.IOException

object NettyWSSpec extends WSSpec with NettyIntegrationSpecification

object AkkaHttpWSSpec extends WSSpec with AkkaHttpIntegrationSpecification

trait WSSpec extends PlaySpecification with ServerIntegrationSpecification with WsTestClient {

  "Web service client" title

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
    import play.libs.ws.{ WS, WSSignatureCalculator }

    "make GET Requests" in withServer { port =>
      val req = WS.url(s"http://localhost:$port/get").get
      val rep = req.get(1000) // AWait result

      rep.getStatus aka "status" must_== 200 and (
        rep.asJson.path("origin").textValue must not beNull)
    }

    "use queryString in url" in withServer { port =>
      val rep = WS.url(s"http://localhost:$port/get?foo=bar").get().get(1000)

      rep.getStatus aka "status" must_== 200 and (
        rep.asJson().path("args").path("foo").textValue() must_== "bar")
    }

    "use user:password in url" in withServer { port =>
      val rep = WS.url(s"http://user:password@localhost:$port/basic-auth/user/password").get().get(1000)

      rep.getStatus aka "status" must_== 200 and (
        rep.asJson().path("authenticated").booleanValue() must beTrue)
    }

    "reject invalid query string" in withServer { port =>
      import java.net.MalformedURLException

      WS.url("http://localhost/get?=&foo").
        aka("invalid request") must throwA[RuntimeException].like {
          case e: RuntimeException =>
            e.getCause must beAnInstanceOf[MalformedURLException]
        }
    }

    "reject invalid user password string" in withServer { port =>
      import java.net.MalformedURLException

      WS.url("http://@localhost/get").
        aka("invalid request") must throwA[RuntimeException].like {
          case e: RuntimeException =>
            e.getCause must beAnInstanceOf[MalformedURLException]
        }
    }

    "consider query string in JSON conversion" in withServer { port =>
      val empty = WS.url(s"http://localhost:$port/get?foo").get.get(1000)
      val bar = WS.url(s"http://localhost:$port/get?foo=bar").get.get(1000)

      empty.asJson.path("args").path("foo").textValue() must_== "" and (
        bar.asJson.path("args").path("foo").textValue() must_== "bar")
    }

    class CustomSigner extends WSSignatureCalculator with com.ning.http.client.SignatureCalculator {
      def calculateAndAddSignature(request: com.ning.http.client.Request, requestBuilder: com.ning.http.client.RequestBuilderBase[_]) = {
        // do nothing
      }
    }

    "not throw an exception while signing requests" in withServer { _ =>
      val key = "12234"
      val secret = "asbcdef"
      val token = "token"
      val tokenSecret = "tokenSecret"
      (ConsumerKey(key, secret), RequestToken(token, tokenSecret))

      val calc: WSSignatureCalculator = new CustomSigner

      WS.url("http://localhost").sign(calc).
        aka("signed request") must not(throwA[Exception])
    }
  }

  "WS@scala" should {
    import play.api.libs.ws.WS
    import play.api.libs.ws.WSSignatureCalculator
    import play.api.Play.current

    "make GET Requests" in withServer { port =>
      val req = WS.url(s"http://localhost:$port/get").get

      Await.result(req, Duration(1, SECONDS)).status aka "status" must_== 200
    }

    "Get 404 errors" in withServer { port =>
      val req = WS.url(s"http://localhost:$port/post").get

      Await.result(req, Duration(1, SECONDS)).status aka "status" must_== 404
    }

    "get a streamed response" in withResult(
      Results.Ok.chunked(Enumerator("a", "b", "c"))) { port =>

        val res = WS.url(s"http://localhost:$port/get").stream()
        val (_, body) = await(res)

        new String(await(body |>>> Iteratee.consume[Array[Byte]]()), "utf-8").
          aka("streamed response") must_== "abc"
      }

    def slow[E](ms: Long): Enumeratee[E, E] =
      Enumeratee.mapM { i => Promise.timeout(i, ms) }

    "get a streamed response when the server is slow" in withResult(
      Results.Ok.chunked(Enumerator("a", "b", "c") &> slow(50))) { port =>

        val res = WS.url(s"http://localhost:$port/get").stream()
        val (_, body) = await(res)

        (new String(await(body |>>> Iteratee.consume[Array[Byte]]()), "utf-8")).
          aka("streamed response") must_== "abc"
      }

    "get a streamed response when the consumer is slow" in withResult(
      Results.Ok.chunked(Enumerator("a", "b", "c") &> slow(10))) { port =>

        val res = WS.url(s"http://localhost:$port/get").stream()
        val (_, body) = await(res)

        new String(await(body &> slow(50) |>>>
          Iteratee.consume[Array[Byte]]()), "utf-8").
          aka("streamed response") must_== "abc"
      }

    "propagate errors from the stream" in withResult(
      Results.Ok.feed(Enumerator.unfold(0) {
        case i if i < 3 => Some((i + 1, "chunk".getBytes("utf-8")))
        case _ => throw new Exception()
      } &> slow(50)).withHeaders("Content-Length" -> "100000")) { port =>
        // Use specially configured test client that doesn't
        // retry so we don't spam the console with errors.
        withClient { ws =>
          val res = ws.url(s"http://localhost:$port/get").stream()
          val (_, body) = await(res)

          await(body |>>> Iteratee.consume[Array[Byte]]()).
            aka("streamed response") must throwAn[IOException]

        }
      }

    class CustomSigner extends WSSignatureCalculator with SignatureCalculator {
      def calculateAndAddSignature(request: com.ning.http.client.Request, requestBuilder: RequestBuilderBase[_]) = {
        // do nothing
      }
    }

    "not throw an exception while signing requests" >> {
      val calc = new CustomSigner

      "without query string" in withServer { port =>
        WS.url("http://localhost:" + port).sign(calc).get().
          aka("signed request") must not(throwA[NullPointerException])
      }

      "with query string" in withServer { port =>
        WS.url("http://localhost:" + port).withQueryString("lorem" -> "ipsum").
          sign(calc) aka "signed request" must not(throwA[Exception])
      }
    }
  }
}
