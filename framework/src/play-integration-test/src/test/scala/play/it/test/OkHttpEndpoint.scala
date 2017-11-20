/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import okhttp3.{ OkHttpClient, Request, Response }
import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.mvc.Call

import scala.annotation.implicitNotFound

/** Describes an [[OkHttpClient] that is bound to a particular [[ServerEndpoint]]. */
trait OkHttpEndpoint {
  /** The endpoint to connect to. */
  def endpoint: ServerEndpoint

  /** A partly-built client to connect with. */
  def clientBuilder: OkHttpClient.Builder

  /** The client to connect with. */
  def client: OkHttpClient = clientBuilder.build()

  /** Make a request to the endpoint using the given path. */
  def requestBuilder(path: String): Request.Builder =
    new Request.Builder().url(endpoint.pathUrl(path))

  /** Create a request that can be called to connect to the endpoint. */
  def request(path: String): Request = requestBuilder(path).build()

  /** Make a request to the endpoint using the given path. */
  def call(path: String): Response = client.newCall(request(path)).execute()
}

object OkHttpEndpoint {

  /**
   * Takes a [[ServerEndpoint]], creates a matching [[OkHttpEndpoint]], calls
   * a block of code on the client and then closes the client afterwards.
   *
   * Most users should use `useOkHttp` instead of this method.
   */
  def withOkHttpEndpoint[A](endpoint: ServerEndpoint)(block: OkHttpEndpoint => A): A = {
    val oke = getOkHttpEndpoint(endpoint)
    block(oke)
  }

  def getOkHttpEndpoint[A](endpoint: ServerEndpoint): OkHttpEndpoint = {
    val e = endpoint // Avoid a name clash
    new OkHttpEndpoint {
      override val endpoint = e
      override val clientBuilder: OkHttpClient.Builder = {
        val ssl = endpoint.recipe.serverSsl
        new OkHttpClient.Builder().sslSocketFactory(ssl.sslContext.getSocketFactory, ssl.trustManager)
      }
    }
  }

}

/**
 * Provides helpers to connect to [[ServerEndpoint]]s with OkHttp.
 */
trait OkHttpEndpointSupport {
  self: EndpointIntegrationSpecification =>

  import OkHttpEndpoint._

  /**
   * Implicit class that enhances [[ApplicationFactory]] with methods to connect with OkHttp.
   */
  implicit final class OkHttpApplicationFactoryOps(appFactory: ApplicationFactory) {
    def useOkHttp: OkHttpEndpointOps = new OkHttpEndpointOps(new PlainServerEndpointOps(appFactory))
  }

  /**
   * Provides a [[OkHttpEndpointOps]] for each underlying endpoint.
   *
   * Also provides the `request` method to conveniently make a request.
   */
  final class OkHttpEndpointOps(delegate: ServerEndpointOps[ServerEndpoint]) extends AdaptingServerEndpointOps[OkHttpEndpoint, ServerEndpoint](delegate) {
    override protected def adaptBlock[A](block: OkHttpEndpoint => A): ServerEndpoint => A = withOkHttpEndpoint(_)(block)
    def request(path: String): ServerEndpointOps[Response] = new OkResponseEndpointOps(path, this)
    override protected def withDelegate(newDelegate: ServerEndpointOps[ServerEndpoint]): OkHttpEndpointOps = new OkHttpEndpointOps(newDelegate)
  }

  /**
   * Makes a request and provides the `okhttp3.Response` for each underlying endpoint.
   */
  final class OkResponseEndpointOps(path: String, delegate: ServerEndpointOps[OkHttpEndpoint]) extends AdaptingServerEndpointOps[Response, OkHttpEndpoint](delegate) {
    override protected def adaptBlock[A](block: Response => A): OkHttpEndpoint => A = { okEndpoint: OkHttpEndpoint =>
      val response: Response = okEndpoint.call(path)
      block(response)
    }
    override protected def withDelegate(newDelegate: ServerEndpointOps[OkHttpEndpoint]): OkResponseEndpointOps = new OkResponseEndpointOps(path, newDelegate)
  }

}
