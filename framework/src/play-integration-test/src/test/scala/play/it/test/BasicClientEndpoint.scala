package play.it.test

import play.it.http.{ BasicHttpClient, BasicRequest, BasicResponse }

/** Describes a [[BasicHttpClient] that is bound to a particular [[ServerEndpoint]]. */
trait BasicClientEndpoint {
  /** The endpoint to connect to. */
  def endpoint: ServerEndpoint

  /** The client to connect with. */
  def client: BasicHttpClient

  def request(method: String, path: String, version: String, headers: Map[String, String] = Map.empty, body: String = ""): BasicResponse = {
    val request: BasicRequest = BasicRequest(method, path, version, headers, body)
    val responses: Seq[BasicResponse] = client.sendRequest(request, "BasicClientEndpoint.request")
    assert(responses.length == 1)
    responses(0)
  }
}

object BasicClientEndpoint {
  /**
   * Takes a [[ServerEndpoint]], creates a matching [[BasicClientEndpoint]], calls
   * a block of code on the client and then closes the client afterwards.
   *
   * Most users should use `useBasicClient` instead of using this method.
   */
  def withBasicClientEndpoint[A](endpoint: ServerEndpoint)(block: BasicClientEndpoint => A): A = {
    val bce = getBasicClientEndpoint(endpoint)
    block(bce)
  }

  def getBasicClientEndpoint[A](endpoint: ServerEndpoint): BasicClientEndpoint = {
    val e = endpoint // Avoid a name clash
    new BasicClientEndpoint {
      override val endpoint = e
      override val client = new BasicHttpClient(endpoint.port, secure = endpoint.recipe.scheme == "https")
    }
  }
}

/**
 * Provides helpers to connect to [[ServerEndpoint]]s with a [[BasicHttpClient]].
 */
trait BasicClientEndpointSupport {
  self: EndpointIntegrationSpecification =>

  import BasicClientEndpoint._

  /**
   * Implicit class that enhances [[ApplicationFactory]] with methods to connect with BasicClient.
   */
  implicit final class BasicClientApplicationFactoryOps(appFactory: ApplicationFactory) {
    def useBasicClient: BasicClientEndpointOps = {
      val ops = new PlainServerEndpointOps(appFactory).skipped(_.supportsHttp2) // Don't test HTTP/2 with BasicClient
      new BasicClientEndpointOps(ops)
    }
  }

  /**
   * Provides a [[BasicClientEndpoint]] for each underlying endpoint.
   *
   * Also provides the `request` method to conveniently make a request.
   */
  final class BasicClientEndpointOps(delegate: ServerEndpointOps[ServerEndpoint]) extends AdaptingServerEndpointOps[BasicClientEndpoint, ServerEndpoint](delegate) {
    override protected def adaptBlock[A](block: BasicClientEndpoint => A): ServerEndpoint => A = withBasicClientEndpoint(_)(block)
    override protected def withDelegate(newDelegate: ServerEndpointOps[ServerEndpoint]): BasicClientEndpointOps = new BasicClientEndpointOps(newDelegate)

    /**
     * Serves a request from the given path.
     */
    def request(path: String): ServerEndpointOps[BasicResponse] = new BasicResponseEndpointOps(path, this)
  }

  /**
   * Makes a request and provides the `BasicResponse` for each underlying endpoint.
   */
  final class BasicResponseEndpointOps(path: String, delegate: ServerEndpointOps[BasicClientEndpoint]) extends AdaptingServerEndpointOps[BasicResponse, BasicClientEndpoint](delegate) {
    override protected def adaptBlock[A](block: BasicResponse => A): BasicClientEndpoint => A = { basicEndpoint: BasicClientEndpoint =>
      val response: BasicResponse = basicEndpoint.request("GET", path, "HTTP/1.1")
      block(response)
    }
    override protected def withDelegate(newDelegate: ServerEndpointOps[BasicClientEndpoint]): BasicResponseEndpointOps = new BasicResponseEndpointOps(path, newDelegate)
  }

}
