package play.api

import play.api.libs.concurrent._
import com.ning.http.client.{ AsyncHttpClient, AsyncCompletionHandler, RequestBuilderBase, Response }

import _root_.oauth.signpost.{ OAuthConsumer, AbstractOAuthConsumer }
import play.api.oauth.{ ConsumerKey, RequestToken }

/**
 * Asynchronous API to to query web services, as an http client
 *
 * Usage example:
 * WS.url("http://example.com/feed").get()
 *
 * The value returned is a Promise of com.ning.http.client.Response,
 * and you should use Play's asynchronous mechanisms to use this response.
 *
 */
object WS {

  private lazy val client = new AsyncHttpClient()

  /**
   * Prepare a new request. You can then construct it by chaining calls.
   * @param url the URL to request
   */
  def url(url: String) = new WSRequest().setUrl(url)

  class WSRequest extends RequestBuilderBase[WSRequest](classOf[WSRequest], "GET") {

    import scala.collection.JavaConversions
    import scala.collection.JavaConversions._

    private var oauthConsumer: Option[OAuthConsumer] = None

    /**
     * Sign the request for OAuth
     */
    def oauth(key: ConsumerKey, token: RequestToken) = {
      val c = new WSOAuthConsumer(key)
      c.setTokenWithSecret(token.token, token.secret)
      oauthConsumer = Some(c)
      this
    }

    /**
     * Perform a GET on the request asynchronously.
     */
    def get(): Promise[Response] = execute("GET")

    /**
     * Perform a POST on the request asynchronously.
     */
    def post(): Promise[Response] = execute("POST")

    /**
     * Perform a PUT on the request asynchronously.
     */
    def put(): Promise[Response] = execute("PUT")

    /**
     * Perform a DELETE on the request asynchronously.
     */
    def delete(): Promise[Response] = execute("DELETE")

    /**
     * Perform a HEAD on the request asynchronously.
     */
    def head(): Promise[Response] = execute("HEAD")

    /**
     * Perform a OPTIONS on the request asynchronously.
     */
    def options(): Promise[Response] = execute("OPTION")

    /**
     * Return the current headers of the request being constructed
     */
    def headers: Map[String, Seq[String]] =
      JavaConversions.mapAsScalaMap(request.getHeaders()).map { entry => (entry._1, entry._2.toSeq) }.toMap

    def header(name: String): Option[String] = headers.get(name).flatMap(_.headOption)

    def method: String = request.getMethod()

    def url: String = request.getUrl()

    private def execute(method: String): Promise[Response] = {
      var result = Promise[Response]()
      var request = this.setMethod(method).build()
      oauthConsumer.map(_.sign(request))
      WS.client.executeRequest(request, new AsyncCompletionHandler[Response]() {
        override def onCompleted(response: Response) = {
          result.redeem(response)
          response
        }
        override def onThrowable(t: Throwable) = {
          result.redeem(throw t)
        }
      })
      result
    }

  }

}

class WSOAuthConsumer(consumerKey: ConsumerKey) extends AbstractOAuthConsumer(consumerKey.key, consumerKey.secret) {

  import _root_.oauth.signpost.http.HttpRequest
  import WS.WSRequest

  override protected def wrap(request: Any) = request match {
    case r: WSRequest => new WSRequestAdapter(r)
    case _ => throw new IllegalArgumentException("WSOAuthConsumer expects requests of type play.api.WS.WSRequest")
  }

  class WSRequestAdapter(request: WSRequest) extends HttpRequest {

    import scala.collection.JavaConversions._

    override def unwrap() = request

    override def getAllHeaders(): java.util.Map[String, String] =
      request.headers.map { entry => (entry._1, entry._2.headOption) }
        .filter { entry => entry._2.isDefined }
        .map { entry => (entry._1, entry._2.get) }

    override def getHeader(name: String): String = request.header(name).getOrElse("")

    override def getContentType(): String = getHeader("Content-Type")

    override def getMessagePayload() = null

    override def getMethod(): String = this.request.method

    override def setHeader(name: String, value: String) {
      request.setHeader(name, value)
    }

    override def getRequestUrl() = request.url

    override def setRequestUrl(url: String) {
      request.setUrl(url)
    }

  }

}

