/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http

import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.settings.ParserSettings
import okhttp3.RequestBody
import okhttp3.internal.{ Util => OkUtil }
import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.mvc.{ RequestHeader, Results }
import play.api.routing.Router
import play.api.test.PlaySpecification
import play.core.server.{ AkkaHttpServer, ServerProvider }
import play.it.test._

class AkkaHttpCustomServerProviderSpec extends PlaySpecification
  with EndpointIntegrationSpecification with OkHttpEndpointSupport with ApplicationFactories {

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

  def requestWithMethod[A: AsResult](endpointRecipe: ServerEndpointRecipe, method: String, body: RequestBody)(f: Either[Int, String] => A): Fragment =
    appFactory.withOkHttpEndpoints(Seq(endpointRecipe)) { okEndpoint: OkHttpEndpoint =>
      val response = okEndpoint.configuredCall("/")(_.method(method, body))
      val param: Either[Int, String] = if (response.code == 200) Right(response.body.string) else Left(response.code)
      f(param)
    }

  import ServerEndpointRecipe.AkkaHttp11Plaintext

  "an AkkaHttpServer with standard settings" should {
    "serve a routed GET request" in requestWithMethod(AkkaHttp11Plaintext, "GET", null)(_ must_== Right("get"))
    "not find an unrouted POST request" in requestWithMethod(AkkaHttp11Plaintext, "POST", OkUtil.EMPTY_REQUEST)(_ must_== Left(404))
    "reject a routed FOO request" in requestWithMethod(AkkaHttp11Plaintext, "FOO", null)(_ must_== Left(501))
    "reject an unrouted BAR request" in requestWithMethod (AkkaHttp11Plaintext, "BAR", OkUtil.EMPTY_REQUEST)(_ must_== Left(501))
    "reject a long header value" in appFactory.withOkHttpEndpoints(Seq(AkkaHttp11Plaintext)) { okEndpoint: OkHttpEndpoint =>
      val response = okEndpoint.configuredCall("/")(_.addHeader("X-ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", "abc"))
      response.code must_== 431
    }
  }

  "an AkkaHttpServer with a custom FOO method" should {

    val customAkkaHttpEndpoint: ServerEndpointRecipe = AkkaHttp11Plaintext
      .withDescription("Akka HTTP HTTP/1.1 (plaintext, supports FOO)")
      .withServerProvider(new ServerProvider {
        def createServer(context: ServerProvider.Context) =
          new AkkaHttpServer(context) {
            override protected def createParserSettings(): ParserSettings = {
              super.createParserSettings.withCustomMethods(HttpMethod.custom("FOO"))
            }
          }
      })

    "serve a routed GET request" in requestWithMethod(customAkkaHttpEndpoint, "GET", null)(_ must_== Right("get"))
    "not find an unrouted POST request" in requestWithMethod(customAkkaHttpEndpoint, "POST", OkUtil.EMPTY_REQUEST)(_ must_== Left(404))
    "serve a routed FOO request" in requestWithMethod(customAkkaHttpEndpoint, "FOO", null)(_ must_== Right("foo"))
    "reject an unrouted BAR request" in requestWithMethod (customAkkaHttpEndpoint, "BAR", OkUtil.EMPTY_REQUEST)(_ must_== Left(501))
  }

  "an AkkaHttpServer with a config to support long headers" should {

    val customAkkaHttpEndpoint: ServerEndpointRecipe = AkkaHttp11Plaintext
      .withDescription("Akka HTTP HTTP/1.1 (plaintext, long headers)")
      .withServerProvider(new ServerProvider {
        def createServer(context: ServerProvider.Context) =
          new AkkaHttpServer(context) {
            override protected def createParserSettings(): ParserSettings = {
              super.createParserSettings.withMaxHeaderNameLength(100)
            }
          }
      })

    "accept a long header value" in appFactory.withOkHttpEndpoints(Seq(customAkkaHttpEndpoint)) { okEndpoint: OkHttpEndpoint =>
      val response = okEndpoint.configuredCall("/")(_.addHeader("X-ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", "abc"))
      response.code must_== 200
    }
  }

}
