/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws.ahc

import java.util

import akka.stream.Materializer
import akka.util.{ ByteString, Timeout }
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.oauth.{ ConsumerKey, OAuthCalculator, RequestToken }
import play.api.libs.ws._
import play.api.mvc._
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits, Helpers, WithServer }
import play.shaded.ahc.io.netty.handler.codec.http.DefaultHttpHeaders
import play.shaded.ahc.org.asynchttpclient.Realm.AuthScheme
import play.shaded.ahc.io.netty.handler.codec.http.cookie.{ Cookie => NettyCookie, DefaultCookie => NettyDefaultCookie }
import play.shaded.ahc.org.asynchttpclient.{ Param, Request => AHCRequest, Response => AHCResponse }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.implicitConversions

class AhcWSSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with FutureMatchers with FutureAwaits with DefaultAwaitTimeout {

  sequential

  "Ahc WSClient" should {

    "support several query string values for a parameter" in {
      val client = mock[StandaloneAhcWSClient]
      val r: AhcWSRequest = makeAhcRequest("http://playframework.com/").withQueryStringParameters("foo" -> "foo1", "foo" -> "foo2").asInstanceOf[AhcWSRequest]
      val req: AHCRequest = r.underlying.buildRequest()

      import scala.collection.JavaConverters._
      val paramsList: Seq[Param] = req.getQueryParams.asScala
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

    "support http headers" in {
      val client = mock[StandaloneAhcWSClient]
      import scala.collection.JavaConverters._
      val req: AHCRequest = makeAhcRequest("http://playframework.com/")
        .addHttpHeaders("key" -> "value1", "key" -> "value2").asInstanceOf[AhcWSRequest]
        .underlying.buildRequest()
      req.getHeaders.getAll("key").asScala must containTheSameElementsAs(Seq("value1", "value2"))
    }
  }

  def makeAhcRequest(url: String): AhcWSRequest = {
    implicit val materializer = mock[Materializer]

    val client = mock[StandaloneAhcWSClient]
    val standalone = new StandaloneAhcWSRequest(client, "http://playframework.com/")
    AhcWSRequest(standalone)
  }

  "not make Content-Type header if there is Content-Type in headers already" in {
    import scala.collection.JavaConverters._
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .addHttpHeaders("content-type" -> "fake/contenttype; charset=utf-8")
      .withBody(<aaa>value1</aaa>)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getHeaders.getAll("Content-Type").asScala must_== Seq("fake/contenttype; charset=utf-8")
  }

  "Have form params on POST of content type application/x-www-form-urlencoded" in {
    val client = mock[StandaloneAhcWSClient]
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .withBody(Map("param1" -> Seq("value1")))
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    (new String(req.getByteData, "UTF-8")) must_== ("param1=value1")

  }

  "Have form body on POST of content type text/plain" in {
    val client = mock[StandaloneAhcWSClient]
    val formEncoding = java.net.URLEncoder.encode("param1=value1", "UTF-8")
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .addHttpHeaders("Content-Type" -> "text/plain")
      .withBody("HELLO WORLD")
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()

    (new String(req.getByteData, "UTF-8")) must be_==("HELLO WORLD")
    val headers = req.getHeaders
    headers.get("Content-Length") must beNull
  }

  "Have form body on POST of content type application/x-www-form-urlencoded explicitly set" in {
    val client = mock[StandaloneAhcWSClient]
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded") // set content type by hand
      .withBody("HELLO WORLD") // and body is set to string (see #5221)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    (new String(req.getByteData, "UTF-8")) must be_==("HELLO WORLD") // should result in byte data.
  }

  "support a custom signature calculator" in {
    val client = mock[StandaloneAhcWSClient]
    var called = false
    val calc = new play.shaded.ahc.org.asynchttpclient.SignatureCalculator with WSSignatureCalculator {
      override def calculateAndAddSignature(
        request: play.shaded.ahc.org.asynchttpclient.Request,
        requestBuilder: play.shaded.ahc.org.asynchttpclient.RequestBuilderBase[_]): Unit = {
        called = true
      }
    }

    val req = makeAhcRequest("http://playframework.com/").sign(calc)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    called must beTrue
  }

  "Have form params on POST of content type application/x-www-form-urlencoded when signed" in {
    val client = mock[StandaloneAhcWSClient]
    import scala.collection.JavaConverters._
    val consumerKey = ConsumerKey("key", "secret")
    val requestToken = RequestToken("token", "secret")
    val calc = OAuthCalculator(consumerKey, requestToken)
    val req: AHCRequest = makeAhcRequest("http://playframework.com/").withBody(Map("param1" -> Seq("value1")))
      .sign(calc)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    // Note we use getFormParams instead of getByteData here.
    req.getFormParams.asScala must containTheSameElementsAs(List(new play.shaded.ahc.org.asynchttpclient.Param("param1", "value1")))
    req.getByteData must beNull // should NOT result in byte data.

    val headers = req.getHeaders
    headers.get("Content-Length") must beNull

  }

  "Not remove a user defined content length header" in {
    val client = mock[StandaloneAhcWSClient]
    val consumerKey = ConsumerKey("key", "secret")
    val requestToken = RequestToken("token", "secret")
    val calc = OAuthCalculator(consumerKey, requestToken)
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .withBody(Map("param1" -> Seq("value1")))
      .addHttpHeaders("Content-Length" -> "9001") // add a meaningless content length here...
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()

    (new String(req.getByteData, "UTF-8")) must be_==("param1=value1") // should result in byte data.

    val headers = req.getHeaders
    headers.get("Content-Length") must_== ("9001")

  }

  "Remove a user defined content length header if we are parsing body explicitly when signed" in {
    val client = mock[StandaloneAhcWSClient]
    import scala.collection.JavaConverters._
    val consumerKey = ConsumerKey("key", "secret")
    val requestToken = RequestToken("token", "secret")
    val calc = OAuthCalculator(consumerKey, requestToken)
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .withBody(Map("param1" -> Seq("value1")))
      .addHttpHeaders("Content-Length" -> "9001") // add a meaningless content length here...
      .sign(calc) // this is signed, so content length is no longer valid per #5221
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()

    val headers = req.getHeaders
    req.getByteData must beNull // should NOT result in byte data.
    req.getFormParams.asScala must containTheSameElementsAs(List(new play.shaded.ahc.org.asynchttpclient.Param("param1", "value1")))
    headers.get("Content-Length") must beNull // no content length!
  }

  "Verify Content-Type header is passed through correctly" in {
    import scala.collection.JavaConverters._
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .addHttpHeaders("Content-Type" -> "text/plain; charset=US-ASCII")
      .withBody("HELLO WORLD")
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getHeaders.getAll("Content-Type").asScala must_== Seq("text/plain; charset=US-ASCII")
  }

  "POST binary data as is" in {
    val binData = ByteString((0 to 511).map(_.toByte).toArray)
    val req: AHCRequest = makeAhcRequest("http://playframework.com/").addHttpHeaders("Content-Type" -> "application/x-custom-bin-data").withBody(binData)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()

    ByteString(req.getByteData) must_== binData
  }

  "support a virtual host" in {
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .withVirtualHost("192.168.1.1")
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getVirtualHost must be equalTo "192.168.1.1"
  }

  "support follow redirects" in {
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .withFollowRedirects(follow = true)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getFollowRedirect must beEqualTo(true)
  }

  "support finite timeout" in {
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .withRequestTimeout(1000.millis)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getRequestTimeout must be equalTo 1000
  }

  "support infinite timeout" in {
    val req: AHCRequest = makeAhcRequest("http://playframework.com/")
      .withRequestTimeout(Duration.Inf)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getRequestTimeout must be equalTo -1
  }

  "not support negative timeout" in {
    makeAhcRequest("http://playframework.com/").withRequestTimeout(-1.millis) should throwAn[IllegalArgumentException]
  }

  "not support a timeout greater than Int.MaxValue" in {
    makeAhcRequest("http://playframework.com/").withRequestTimeout((Int.MaxValue.toLong + 1).millis) should throwAn[IllegalArgumentException]
  }

  "support a proxy server with basic" in {
    val proxy = DefaultWSProxyServer(protocol = Some("https"), host = "localhost", port = 8080, principal = Some("principal"), password = Some("password"))
    val req: AHCRequest = makeAhcRequest("http://playframework.com/").withProxyServer(proxy)
      .asInstanceOf[AhcWSRequest].underlying.buildRequest()
    val actual = req.getProxyServer

    actual.getHost must be equalTo "localhost"
    actual.getPort must be equalTo 8080
    actual.getRealm.getPrincipal must be equalTo "principal"
    actual.getRealm.getPassword must be equalTo "password"
    actual.getRealm.getScheme must be equalTo AuthScheme.BASIC
  }

  "support a proxy server with NTLM" in {
    val proxy = DefaultWSProxyServer(protocol = Some("ntlm"), host = "localhost", port = 8080, principal = Some("principal"), password = Some("password"), ntlmDomain = Some("somentlmdomain"))
    val req: AHCRequest = makeAhcRequest("http://playframework.com/").withProxyServer(proxy).asInstanceOf[AhcWSRequest].underlying.buildRequest()
    val actual = req.getProxyServer

    actual.getHost must be equalTo "localhost"
    actual.getPort must be equalTo 8080
    actual.getRealm.getPrincipal must be equalTo "principal"
    actual.getRealm.getPassword must be equalTo "password"
    actual.getRealm.getNtlmDomain must be equalTo "somentlmdomain"
    actual.getRealm.getScheme must be equalTo AuthScheme.NTLM
  }

  "Set Realm.UsePreemptiveAuth to false when WSAuthScheme.DIGEST being used" in {
    val req = makeAhcRequest("http://playframework.com/")
      .withAuth("usr", "pwd", WSAuthScheme.DIGEST)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getRealm.isUsePreemptiveAuth must beFalse
  }

  "Set Realm.UsePreemptiveAuth to true when WSAuthScheme.DIGEST not being used" in {
    val req = makeAhcRequest("http://playframework.com/")
      .withAuth("usr", "pwd", WSAuthScheme.BASIC)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    req.getRealm.isUsePreemptiveAuth must beTrue
  }

  "support a proxy server" in {
    val proxy = DefaultWSProxyServer(host = "localhost", port = 8080)
    val req: AHCRequest = makeAhcRequest("http://playframework.com/").withProxyServer(proxy)
      .asInstanceOf[AhcWSRequest].underlying
      .buildRequest()
    val actual = req.getProxyServer

    actual.getHost must be equalTo "localhost"
    actual.getPort must be equalTo 8080
    actual.getRealm must beNull
  }

  def patchFakeApp = {
    val routes: (Application) => PartialFunction[(String, String), Handler] = { app: Application =>
      {
        case ("PATCH", "/") =>
          val action = app.injector.instanceOf(classOf[DefaultActionBuilder])
          action {
            Results.Ok(play.api.libs.json.Json.parse(
              """{
                |  "data": "body"
                |}
              """.stripMargin))
          }
      }
    }

    GuiceApplicationBuilder().appRoutes(routes).build()
  }

  "support patch method" in new WithServer(patchFakeApp) {
    // NOTE: if you are using a client proxy like Privoxy or Polipo, your proxy may not support PATCH & return 400.
    {
      val wsClient = app.injector.instanceOf(classOf[play.api.libs.ws.WSClient])
      val futureResponse = wsClient.url(s"http://localhost:${Helpers.testServerPort}/").patch("body")

      // This test experiences CI timeouts. Give it more time.
      val reallyLongTimeout = Timeout(defaultAwaitTimeout.duration * 3)
      val rep = await(futureResponse)(reallyLongTimeout)

      rep.status must ===(200)
      (rep.json \ "data").asOpt[String] must beSome("body")
    }
  }

  def gzipFakeApp = {
    import java.io._
    import java.util.zip._

    lazy val Action = ActionBuilder.ignoringBody

    val routes: Application => PartialFunction[(String, String), Handler] = {
      app =>
        {
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
        }
    }

    GuiceApplicationBuilder()
      .configure("play.ws.compressionEnabled" -> true)
      .appRoutes(routes)
      .build()
  }

  "support gziped encoding" in new WithServer(gzipFakeApp) {
    val client = app.injector.instanceOf[WSClient]
    val req = client.url("http://localhost:" + port + "/").get()
    val rep = Await.result(req, 1.second)
    rep.body must ===("gziped response")
  }

  "Ahc WS Response" should {
    "get cookies from an AHC response" in {

      val ahcResponse: AHCResponse = mock[AHCResponse]
      val (name, value, wrap, domain, path, maxAge, secure, httpOnly) =
        ("someName", "someValue", true, "example.com", "/", 1000L, false, false)

      val ahcCookie = createCookie(name, value, wrap, domain, path, maxAge, secure, httpOnly)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = makeAhcResponse(ahcResponse)

      val cookies: Seq[WSCookie] = response.cookies
      val cookie = cookies.head

      cookie.name must ===(name)
      cookie.value must ===(value)
      cookie.domain must beSome(domain)
      cookie.path must beSome(path)
      cookie.maxAge must beSome(maxAge)
      cookie.secure must beFalse
    }

    "get a single cookie from an AHC response" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val (name, value, wrap, domain, path, maxAge, secure, httpOnly) =
        ("someName", "someValue", true, "example.com", "/", 1000L, false, false)

      val ahcCookie = createCookie(name, value, wrap, domain, path, maxAge, secure, httpOnly)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = makeAhcResponse(ahcResponse)

      val optionCookie = response.cookie("someName")
      optionCookie must beSome[WSCookie].which {
        cookie =>
          cookie.name must ===(name)
          cookie.value must ===(value)
          cookie.domain must beSome(domain)
          cookie.path must beSome(path)
          cookie.maxAge must beSome(maxAge)
          cookie.secure must beFalse
      }
    }

    "return -1 values of expires and maxAge as None" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]

      val ahcCookie = createCookie("someName", "value", true, "domain", "path", -1L, false, false)
      ahcResponse.getCookies returns util.Arrays.asList(ahcCookie)

      val response = makeAhcResponse(ahcResponse)

      val optionCookie = response.cookie("someName")
      optionCookie must beSome[WSCookie].which { cookie =>
        cookie.maxAge must beNone
      }
    }

    "get the body as bytes from the AHC response" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val bytes = ByteString(-87, -72, 96, -63, -32, 46, -117, -40, -128, -7, 61, 109, 80, 45, 44, 30)
      ahcResponse.getResponseBodyAsBytes returns bytes.toArray
      val response = makeAhcResponse(ahcResponse)
      response.bodyAsBytes must_== bytes
    }

    "get headers from an AHC response in a case insensitive map" in {
      val ahcResponse: AHCResponse = mock[AHCResponse]
      val ahcHeaders = new DefaultHttpHeaders(true)
      ahcHeaders.add("Foo", "bar")
      ahcHeaders.add("Foo", "baz")
      ahcHeaders.add("Bar", "baz")
      ahcResponse.getHeaders returns ahcHeaders
      val response = makeAhcResponse(ahcResponse)
      val headers = response.headers
      headers must beEqualTo(Map("Foo" -> Seq("bar", "baz"), "Bar" -> Seq("baz")))
      headers.contains("foo") must beTrue
      headers.contains("Foo") must beTrue
      headers.contains("BAR") must beTrue
      headers.contains("Bar") must beTrue
    }
  }

  def createCookie(name: String, value: String, wrap: Boolean, domain: String, path: String, maxAge: Long, secure: Boolean, httpOnly: Boolean): NettyCookie = {
    val ahcCookie = new NettyDefaultCookie(name, value)
    ahcCookie.setWrap(wrap)
    ahcCookie.setDomain(domain)
    ahcCookie.setPath(path)
    ahcCookie.setMaxAge(maxAge)
    ahcCookie.setSecure(secure)
    ahcCookie.setHttpOnly(httpOnly)

    ahcCookie
  }

  def makeAhcResponse(ahcResponse: AHCResponse): AhcWSResponse = {
    AhcWSResponse(StandaloneAhcWSResponse(ahcResponse))
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

