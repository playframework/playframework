/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import okhttp3.RequestBody
import okio.ByteString
import org.apache.pekko.http.scaladsl.model.HttpMethod
import org.apache.pekko.http.scaladsl.settings.ParserSettings
import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.mvc.RequestHeader
import play.api.mvc.Results
import play.api.routing.Router
import play.api.test.ApplicationFactories
import play.api.test.ApplicationFactory
import play.api.test.PlaySpecification
import play.api.test.ServerEndpointRecipe
import play.core.server.PekkoHttpServer
import play.core.server.Server
import play.core.server.ServerProvider
import play.it.test._

class PekkoHttpCustomServerProviderSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with OkHttpEndpointSupport
    with ApplicationFactories {
  final val emptyRequest = RequestBody.create(null, ByteString.EMPTY)

  val appFactory: ApplicationFactory = withRouter { components =>
    import play.api.routing.sird.{ GET => SirdGet, _ }
    object SirdFoo {
      def unapply(rh: RequestHeader): Option[RequestHeader] =
        if (rh.method.equalsIgnoreCase("foo")) Some(rh) else None
    }
    Router.from {
      case SirdGet(p"/") => components.defaultActionBuilder(Results.Ok("get"))
      case SirdFoo(p"/") => components.defaultActionBuilder(Results.Ok("foo"))
    }
  }

  def requestWithMethod[A: AsResult](endpointRecipe: ServerEndpointRecipe, method: String, body: RequestBody)(
      f: Either[Int, String] => A
  ): Fragment =
    appFactory.withOkHttpEndpoints(Seq(endpointRecipe)) { (okEndpoint: OkHttpEndpoint) =>
      val response                   = okEndpoint.configuredCall("/")(_.method(method, body))
      val param: Either[Int, String] = if (response.code == 200) Right(response.body.string) else Left(response.code)
      f(param)
    }

  import PekkoHttpServerEndpointRecipes.PekkoHttp11Plaintext

  "an PekkoHttpServer with standard settings" should {
    "serve a routed GET request" in requestWithMethod(PekkoHttp11Plaintext, "GET", null)(_ must beRight("get"))
    "not find an unrouted POST request" in requestWithMethod(PekkoHttp11Plaintext, "POST", emptyRequest)(
      _ must beLeft(404)
    )
    "reject a routed FOO request" in requestWithMethod(PekkoHttp11Plaintext, "FOO", null)(_ must beLeft(501))
    "reject an unrouted BAR request" in requestWithMethod(PekkoHttp11Plaintext, "BAR", emptyRequest)(
      _ must beLeft(501)
    )
    "reject a long header value" in appFactory.withOkHttpEndpoints(Seq(PekkoHttp11Plaintext)) {
      (okEndpoint: OkHttpEndpoint) =>
        val response = okEndpoint.configuredCall("/")(
          _.addHeader("X-ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", "abc")
        )
        response.code must_== 431
    }
  }

  "an PekkoHttpServer with a custom FOO method" should {
    val customPekkoHttpEndpoint: ServerEndpointRecipe = PekkoHttp11Plaintext
      .withDescription("Pekko HTTP HTTP/1.1 (plaintext, supports FOO)")
      .withServerProvider(new ServerProvider {
        def createServer(context: ServerProvider.Context): Server =
          new PekkoHttpServer(PekkoHttpServer.Context.fromServerProviderContext(context)) {
            protected override def createParserSettings(): ParserSettings = {
              super.createParserSettings().withCustomMethods(HttpMethod.custom("FOO"))
            }
          }
      })

    "serve a routed GET request" in requestWithMethod(customPekkoHttpEndpoint, "GET", null)(_ must beRight("get"))
    "not find an unrouted POST request" in requestWithMethod(customPekkoHttpEndpoint, "POST", emptyRequest)(
      _ must beLeft(404)
    )
    "serve a routed FOO request" in requestWithMethod(customPekkoHttpEndpoint, "FOO", null)(_ must beRight("foo"))
    "reject an unrouted BAR request" in requestWithMethod(customPekkoHttpEndpoint, "BAR", emptyRequest)(
      _ must beLeft(501)
    )
  }

  "an PekkoHttpServer with a config to support long headers" should {
    val customPekkoHttpEndpoint: ServerEndpointRecipe = PekkoHttp11Plaintext
      .withDescription("Pekko HTTP HTTP/1.1 (plaintext, long headers)")
      .withServerProvider(new ServerProvider {
        def createServer(context: ServerProvider.Context): Server =
          new PekkoHttpServer(PekkoHttpServer.Context.fromServerProviderContext(context)) {
            protected override def createParserSettings(): ParserSettings = {
              super.createParserSettings().withMaxHeaderNameLength(100)
            }
          }
      })

    "accept a long header value" in appFactory.withOkHttpEndpoints(Seq(customPekkoHttpEndpoint)) {
      (okEndpoint: OkHttpEndpoint) =>
        val response = okEndpoint.configuredCall("/")(
          _.addHeader("X-ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", "abc")
        )
        response.code must_== 200
    }
  }
}
