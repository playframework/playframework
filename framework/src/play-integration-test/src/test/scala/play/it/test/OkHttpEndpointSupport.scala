/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import okhttp3.{ OkHttpClient, Request, Response }
import org.specs2.execute.AsResult
import org.specs2.specification.core.Fragment
import play.api.mvc.Call

import scala.annotation.implicitNotFound

/**
 * Provides a similar interface to [[play.api.test.WsTestClient]], but
 * uses OkHttp and connects to an integration test's [[ServerEndpoints.ServerEndpoint]]
 * instead of to an arbitrary scheme and port.
 */
trait OkHttpEndpointSupport {
  self: EndpointIntegrationSpecification =>

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

  /**
   * Takes a [[ServerEndpoint]], creates a matching [[OkHttpEndpoint]], calls
   * a block of code on the client and then closes the client afterwards.
   *
   * Most users should use [[OkHttpApplicationFactory.withAllOkHttpEndpoints()]]
   * instead of this method.
   */
  def withOkHttpEndpoint[A](endpoint: ServerEndpoint)(block: OkHttpEndpoint => A): A = {
    val e = endpoint // Avoid a name clash
    val serverClient = new OkHttpEndpoint {
      override val endpoint = e
      override val clientBuilder: OkHttpClient.Builder = {
        val b = new OkHttpClient.Builder()
        endpoint match {
          case e: HttpsEndpoint =>
            b.sslSocketFactory(e.serverSsl.sslContext.getSocketFactory, e.serverSsl.trustManager)
          case _ => b
        }
      }
    }
    block(serverClient)
  }

  /**
   * Implicit class that enhances [[ApplicationFactory]] with the [[withAllOkHttpEndpoints()]] method.
   */
  implicit class OkHttpApplicationFactory(appFactory: ApplicationFactory) {
    /**
     * Helper that creates a specs2 fragment for the server endpoints given in
     * [[allEndpointRecipes]]. Each fragment creates an application, starts a server,
     * starts an [[OkHttpClient]] and runs the given block of code.
     *
     * {{{
     * withResult(Results.Ok("Hello")) withAllOkHttpEndpoints {
     *   okEndpoint: OkHttpEndpoint =>
     *     val response = okEndpoint.makeRequest("/")
     *     response.body.string must_== "Hello"
     * }
     * }}}
     */
    def withAllOkHttpEndpoints[A: AsResult](block: OkHttpEndpoint => A): Fragment =
      appFactory.withAllEndpoints { endpoint: ServerEndpoint => withOkHttpEndpoint(endpoint)(block) }
  }

}
