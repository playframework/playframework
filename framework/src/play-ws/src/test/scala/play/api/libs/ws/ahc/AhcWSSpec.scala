/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import akka.util.{ ByteString, Timeout }
import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaders }
import org.asynchttpclient.Realm.AuthScheme
import org.asynchttpclient.cookie.{ Cookie => AHCCookie }
import org.asynchttpclient.{ AsyncHttpClient, DefaultAsyncHttpClientConfig, Param, Response => AHCResponse, Request => AHCRequest }
import org.specs2.mock.Mockito
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.oauth.{ RequestToken, ConsumerKey, OAuthCalculator }

import play.api.mvc._

import java.util
import play.api.libs.ws._
import play.api.test._
import akka.util.ByteString

object AhcWSSpec extends PlaySpecification with Mockito {

  sequential

  "Ahc WS" should {

    object PairMagnet {
      implicit def fromPair(pair: Pair[WSClient, java.net.URL]): WSRequestMagnet =
        new WSRequestMagnet {
          def apply(): WSRequest = {
            val (client, netUrl) = pair
            client.url(netUrl.toString)
          }
        }
    }

    "support Ahc magnet" in new WithApplication {
      import scala.language.implicitConversions
      import PairMagnet._

      val client = WS.client
      val exampleURL = new java.net.URL("http://example.com")
      WS.url(client -> exampleURL) must beAnInstanceOf[WSRequest]
    }

    "support direct client instantiation" in new WithApplication {
      val sslBuilder = new DefaultAsyncHttpClientConfig.Builder().setShutdownQuietPeriod(0).setShutdownTimeout(0)
      implicit val sslClient = new play.api.libs.ws.ahc.AhcWSClient(sslBuilder.build())
      try WS.clientUrl("http://example.com/feed") must beAnInstanceOf[WSRequest]
      finally sslClient.close()
    }

    "AhcWSClient.underlying" in new WithApplication {
      val client = WS.client
      client.underlying[AsyncHttpClient] must beAnInstanceOf[AsyncHttpClient]
    }

    "AhcWSCookie.underlying" in new WithApplication() {
      val mockCookie = mock[AHCCookie]
      val cookie = new AhcWSCookie(mockCookie)
      val thisCookie = cookie.underlying[AHCCookie]
    }

    "support several query string values for a parameter" in new WithApplication {
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withQueryString("foo" -> "foo1", "foo" -> "foo2").asInstanceOf[AhcWSRequest].buildRequest()

      import scala.collection.JavaConverters._
      val paramsList: Seq[Param] = req.getQueryParams.asScala.toSeq
      paramsList.exists(p => (p.getName == "foo") && (p.getValue == "foo1")) must beTrue
      paramsList.exists(p => (p.getName == "foo") && (p.getValue == "foo2")) must beTrue
      paramsList.count(p => p.getName == "foo") must beEqualTo(2)
    }

    /*
    "AhcWSRequest.setHeaders using a builder with direct map" in new WithApplication {
      val request = new AhcWSRequest(mock[AhcWSClient], "GET", None, None, Map.empty, EmptyBody, new RequestBuilder("GET"))
      val headerMap: Map[String, Seq[String]] = Map("key" -> Seq("value"))
      val ahcRequest = request.setHeaders(headerMap).build
      ahcRequest.getHeaders.containsKey("key") must beTrue
    }

    "AhcWSRequest.setQueryString" in new WithApplication {
      val request = new AhcWSRequest(mock[AhcWSClient], "GET", None, None, Map.empty, EmptyBody, new RequestBuilder("GET"))
      val queryString: Map[String, Seq[String]] = Map("key" -> Seq("value"))
      val ahcRequest = request.setQueryString(queryString).build
      ahcRequest.getQueryParams().containsKey("key") must beTrue
    }

    "support several query string values for a parameter" in new WithApplication {
      val req = WS.url("http://playframework.com/")
        .withQueryString("foo" -> "foo1", "foo" -> "foo2").asInstanceOf[AhcWSRequestHolder]
        .prepare().build
      req.getQueryParams.get("foo").contains("foo1") must beTrue
      req.getQueryParams.get("foo").contains("foo2") must beTrue
      req.getQueryParams.get("foo").size must equalTo(2)
    }
    */

    "support http headers" in new WithApplication {
      import scala.collection.JavaConverters._
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withHeaders("key" -> "value1", "key" -> "value2").asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getHeaders.getAll("key").asScala must containTheSameElementsAs(Seq("value1", "value2"))
    }

    "not make Content-Type header if there is Content-Type in headers already" in new WithApplication {
      import scala.collection.JavaConverters._
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withHeaders("Content-Type" -> "fake/contenttype; charset=utf-8").withBody(<aaa>value1</aaa>).asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getHeaders.get("Content-Type") must_== ("fake/contenttype; charset=utf-8")
    }

    "Have form params on POST of content type application/x-www-form-urlencoded" in new WithApplication {
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withBody(Map("param1" -> Seq("value1")))
        .asInstanceOf[AhcWSRequest]
        .buildRequest()
      (new String(req.getByteData, "UTF-8")) must_== ("param1=value1")
    }

    "Have form body on POST of content type text/plain" in new WithApplication {
      val formEncoding = java.net.URLEncoder.encode("param1=value1", "UTF-8")
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withHeaders("Content-Type" -> "text/plain")
        .withBody("HELLO WORLD")
        .asInstanceOf[AhcWSRequest]
        .buildRequest()

      (new String(req.getByteData, "UTF-8")) must be_==("HELLO WORLD")
      val headers = req.getHeaders
      headers.get("Content-Length") must beNull
    }

    "Have form body on POST of content type application/x-www-form-urlencoded explicitly set" in new WithApplication {
      import scala.collection.JavaConverters._
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withHeaders("Content-Type" -> "application/x-www-form-urlencoded") // set content type by hand
        .withBody("HELLO WORLD") // and body is set to string (see #5221)
        .asInstanceOf[AhcWSRequest]
        .buildRequest()
      (new String(req.getByteData, "UTF-8")) must be_==("HELLO WORLD") // should result in byte data.
    }

    "Have form params on POST of content type application/x-www-form-urlencoded when signed" in new WithApplication {
      import scala.collection.JavaConverters._
      val consumerKey = ConsumerKey("key", "secret")
      val requestToken = RequestToken("token", "secret")
      val calc = OAuthCalculator(consumerKey, requestToken)
      val req: AHCRequest = WS.url("http://playframework.com/").withBody(Map("param1" -> Seq("value1")))
        .sign(calc)
        .asInstanceOf[AhcWSRequest]
        .buildRequest()
      // Note we use getFormParams instead of getByteData here.
      req.getFormParams.asScala must containTheSameElementsAs(List(new org.asynchttpclient.Param("param1", "value1")))
      req.getByteData must beNull // should NOT result in byte data.

      val headers = req.getHeaders
      headers.get("Content-Length") must beNull
    }

    "Not remove a user defined content length header" in new WithApplication {
      import scala.collection.JavaConverters._
      val consumerKey = ConsumerKey("key", "secret")
      val requestToken = RequestToken("token", "secret")
      val calc = OAuthCalculator(consumerKey, requestToken)
      val req: AHCRequest = WS.url("http://playframework.com/").withBody(Map("param1" -> Seq("value1")))
        .withHeaders("Content-Length" -> "9001") // add a meaningless content length here...
        .asInstanceOf[AhcWSRequest]
        .buildRequest()

      (new String(req.getByteData, "UTF-8")) must be_==("param1=value1") // should result in byte data.

      val headers = req.getHeaders
      headers.get("Content-Length") must_== ("9001")
    }

    "Remove a user defined content length header if we are parsing body explicitly when signed" in new WithApplication {
      import scala.collection.JavaConverters._
      val consumerKey = ConsumerKey("key", "secret")
      val requestToken = RequestToken("token", "secret")
      val calc = OAuthCalculator(consumerKey, requestToken)
      val req: AHCRequest = WS.url("http://playframework.com/").withBody(Map("param1" -> Seq("value1")))
        .withHeaders("Content-Length" -> "9001") // add a meaningless content length here...
        .sign(calc) // this is signed, so content length is no longer valid per #5221
        .asInstanceOf[AhcWSRequest]
        .buildRequest()

      val headers = req.getHeaders
      req.getByteData must beNull // should NOT result in byte data.
      req.getFormParams.asScala must containTheSameElementsAs(List(new org.asynchttpclient.Param("param1", "value1")))
      headers.get("Content-Length") must beNull // no content length!
    }

    "Verify Content-Type header is passed through correctly" in new WithApplication {
      import scala.collection.JavaConverters._
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withHeaders("Content-Type" -> "text/plain; charset=US-ASCII")
        .withBody("HELLO WORLD")
        .asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getHeaders.get("Content-Type") must_== ("text/plain; charset=US-ASCII")
    }

    "Only send first content type header if two are sent" in new WithApplication {
      import scala.collection.JavaConverters._
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withHeaders("Content-Type" -> "application/json")
        .withHeaders("Content-Type" -> "application/xml") // second content type header is ignored
        .withBody("HELLO WORLD").asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getHeaders.get("Content-Type") must_== ("application/json")
    }

    "POST binary data as is" in new WithApplication {
      val binData = ByteString((0 to 511).map(_.toByte).toArray)
      val req: AHCRequest = WS.url("http://playframework.com/").withHeaders("Content-Type" -> "application/x-custom-bin-data").withBody(binData).asInstanceOf[AhcWSRequest]
        .buildRequest()

      ByteString(req.getByteData) must_== binData
    }

    "support a virtual host" in new WithApplication {
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withVirtualHost("192.168.1.1").asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getVirtualHost must be equalTo "192.168.1.1"
    }

    "support follow redirects" in new WithApplication {
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withFollowRedirects(follow = true).asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getFollowRedirect must beEqualTo(true)
    }

    "support finite timeout" in new WithApplication {
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withRequestTimeout(1000.millis).asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getRequestTimeout must be equalTo 1000
    }

    "support infinite timeout" in new WithApplication {
      val req: AHCRequest = WS.url("http://playframework.com/")
        .withRequestTimeout(Duration.Inf).asInstanceOf[AhcWSRequest]
        .buildRequest()
      req.getRequestTimeout must be equalTo -1
    }

    "not support negative timeout" in new WithApplication {
      WS.url("http://playframework.com/").withRequestTimeout(-1.millis) should throwAn[IllegalArgumentException]
    }

    "not support a timeout greater than Int.MaxValue" in new WithApplication {
      WS.url("http://playframework.com/").withRequestTimeout((Int.MaxValue.toLong + 1).millis) should throwAn[IllegalArgumentException]
    }

    "support a proxy server with basic" in new WithApplication {
      val proxy = DefaultWSProxyServer(protocol = Some("https"), host = "localhost", port = 8080, principal = Some("principal"), password = Some("password"))
      val req: AHCRequest = WS.url("http://playframework.com/").withProxyServer(proxy).asInstanceOf[AhcWSRequest].buildRequest()
      val actual = req.getProxyServer

      actual.getHost must be equalTo "localhost"
      actual.getPort must be equalTo 8080
      actual.getRealm.getPrincipal must be equalTo "principal"
      actual.getRealm.getPassword must be equalTo "password"
      actual.getRealm.getScheme must be equalTo AuthScheme.BASIC
    }

    "support a proxy server with NTLM" in new WithApplication {
      val proxy = DefaultWSProxyServer(protocol = Some("ntlm"), host = "localhost", port = 8080, principal = Some("principal"), password = Some("password"), ntlmDomain = Some("somentlmdomain"))
      val req: AHCRequest = WS.url("http://playframework.com/").withProxyServer(proxy).asInstanceOf[AhcWSRequest].buildRequest()
      val actual = req.getProxyServer

      actual.getHost must be equalTo "localhost"
      actual.getPort must be equalTo 8080
      actual.getRealm.getPrincipal must be equalTo "principal"
      actual.getRealm.getPassword must be equalTo "password"
      actual.getRealm.getNtlmDomain must be equalTo "somentlmdomain"
      actual.getRealm.getScheme must be equalTo AuthScheme.NTLM
    }

    "support a proxy server" in new WithApplication {
      val proxy = DefaultWSProxyServer(host = "localhost", port = 8080)
      val req: AHCRequest = WS.url("http://playframework.com/").withProxyServer(proxy).asInstanceOf[AhcWSRequest].buildRequest()
      val actual = req.getProxyServer

      actual.getHost must be equalTo "localhost"
      actual.getPort must be equalTo 8080
      actual.getRealm must beNull
    }

    val patchFakeApp = GuiceApplicationBuilder().routes {
      case ("PATCH", "/") => Action {
        Results.Ok(play.api.libs.json.Json.parse(
          """{
            |  "data": "body"
            |}
          """.stripMargin))
      }
    }.build()

    "support patch method" in new WithServer(patchFakeApp) {
      // NOTE: if you are using a client proxy like Privoxy or Polipo, your proxy may not support PATCH & return 400.
      val futureResponse = WS.url("http://localhost:" + port + "/").patch("body")

      // This test experiences CI timeouts. Give it more time.
      val reallyLongTimeout = Timeout(defaultAwaitTimeout.duration * 3)
      val rep = await(futureResponse)(reallyLongTimeout)

      rep.status must ===(200)
      (rep.json \ "data").asOpt[String] must beSome("body")
    }

    def gzipFakeApp = {
      import java.io._
      import java.util.zip._
      GuiceApplicationBuilder()
        .configure("play.ws.compressionEnabled" -> true)
        .routes({
          case ("GET", "/") => Action { request =>
            request.headers.get("Accept-Encoding") match {
              case Some(encoding) if encoding.contains("gzip") =>
                val os = new ByteArrayOutputStream
                val gzipOs = new GZIPOutputStream(os)
                gzipOs.write("gziped response".getBytes("utf-8"))
                gzipOs.close()
                Results.Ok(os.toByteArray).as("text/plain").withHeaders("Content-Encoding" -> "gzip")
              case _ =>
                Results.Ok("plain response")
            }
          }
        })
        .build()
    }

    "support gziped encoding" in new WithServer(gzipFakeApp) {

      val req = WS.url("http://localhost:" + port + "/").get()
      val rep = await(req)
      rep.body must ===("gziped response")
    }
  }

  "Ahc WS Response" should {
    "get cookies from an AHC response" in {

      val ahcResponse: AHCResponse = mock[AHCResponse]
      val (name, value, wrap, domain, path, maxAge, secure, httpOnly) =
        ("someName", "someValue", true, "example.com", "/", 1000L, false, false)

      val ahcCookie: AHCCookie = new AHCCookie(name, value, wrap, domain, path, maxAge, secure, httpOnly)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = AhcWSResponse(ahcResponse)

      val cookies: Seq[WSCookie] = response.cookies
      val cookie = cookies.head

      cookie.domain must ===(domain)
      cookie.name must beSome(name)
      cookie.value must beSome(value)
      cookie.path must ===(path)
      cookie.maxAge must beSome(maxAge)
      cookie.secure must beFalse
    }

    "get a single cookie from an AHC response" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val (name, value, wrap, domain, path, maxAge, secure, httpOnly) =
        ("someName", "someValue", true, "example.com", "/", 1000L, false, false)

      val ahcCookie: AHCCookie = new AHCCookie(name, value, wrap, domain, path, maxAge, secure, httpOnly)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = AhcWSResponse(ahcResponse)

      val optionCookie = response.cookie("someName")
      optionCookie must beSome[WSCookie].which {
        cookie =>
          cookie.name must beSome(name)
          cookie.value must beSome(value)
          cookie.domain must ===(domain)
          cookie.path must ===(path)
          cookie.maxAge must beSome(maxAge)
          cookie.secure must beFalse
      }
    }

    "return -1 values of expires and maxAge as None" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]

      val ahcCookie: AHCCookie = new AHCCookie("someName", "value", true, "domain", "path", -1L, false, false)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = AhcWSResponse(ahcResponse)

      val optionCookie = response.cookie("someName")
      optionCookie must beSome[WSCookie].which { cookie =>
        cookie.maxAge must beNone
      }
    }

    "get the body as bytes from the AHC response" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val bytes = ByteString(-87, -72, 96, -63, -32, 46, -117, -40, -128, -7, 61, 109, 80, 45, 44, 30)
      ahcResponse.getResponseBodyAsBytes returns bytes.toArray
      val response = AhcWSResponse(ahcResponse)
      response.bodyAsBytes must_== bytes
    }

    "get headers from an AHC response in a case insensitive map" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val ahcHeaders = new DefaultHttpHeaders(true)
      ahcHeaders.add("Foo", "bar")
      ahcHeaders.add("Foo", "baz")
      ahcHeaders.add("Bar", "baz")
      ahcResponse.getHeaders returns ahcHeaders
      val response = AhcWSResponse(ahcResponse)
      val headers = response.allHeaders
      headers must beEqualTo(Map("Foo" -> Seq("bar", "baz"), "Bar" -> Seq("baz")))
      headers.contains("foo") must beTrue
      headers.contains("Foo") must beTrue
      headers.contains("BAR") must beTrue
      headers.contains("Bar") must beTrue
    }
  }

  "withRequestFilter" should {

    class CallbackRequestFilter(callList: scala.collection.mutable.Buffer[Int], value: Int) extends WSRequestFilter {
      override def apply(executor: WSRequestExecutor): WSRequestExecutor = {
        callList.append(value)
        executor
      }
    }

    "work with one request filter" in new WithServer() {
      val client = app.injector.instanceOf(classOf[WSClient])
      val callList = scala.collection.mutable.ArrayBuffer[Int]()
      val responseFuture = client.url(s"http://example.com:$testServerPort")
        .withRequestFilter(new CallbackRequestFilter(callList, 1))
        .get()
      callList must contain(1)
    }

    "work with three request filter" in new WithServer() {
      val client = app.injector.instanceOf(classOf[WSClient])
      val callList = scala.collection.mutable.ArrayBuffer[Int]()
      val responseFuture = client.url(s"http://localhost:${testServerPort}")
        .withRequestFilter(new CallbackRequestFilter(callList, 1))
        .withRequestFilter(new CallbackRequestFilter(callList, 2))
        .withRequestFilter(new CallbackRequestFilter(callList, 3))
        .get()
      callList must containTheSameElementsAs(Seq(1, 2, 3))
    }
  }

  "Ahc WS Config" should {
    "support overriding secure default values" in {
      val ahcConfig = new AhcConfigBuilder().modifyUnderlying { builder =>
        builder.setCompressionEnforced(false)
        builder.setFollowRedirect(false)
      }.build()
      ahcConfig.isCompressionEnforced must beFalse
      ahcConfig.isFollowRedirect must beFalse
      ahcConfig.getConnectTimeout must_== 120000
      ahcConfig.getRequestTimeout must_== 120000
      ahcConfig.getReadTimeout must_== 120000
    }
  }

}

