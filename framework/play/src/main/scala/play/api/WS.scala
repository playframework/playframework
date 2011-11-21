package play.api

import play.api.libs.concurrent._
import com.ning.http.client.{ AsyncHttpClient, AsyncCompletionHandler, RequestBuilderBase, Response }

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

  import ws._

  private lazy val client = new AsyncHttpClient()

  /**
   * Prepare a new request. You can then construct it by chaining calls.
   * @param url the URL to request
   */
  def url(url: String) = new WSRequest().setUrl(url)

  class WSRequest extends RequestBuilderBase[WSRequest](classOf[WSRequest], "GET") {

    import scala.collection.JavaConversions
    import scala.collection.JavaConversions._

    private var calculator: Option[SignatureCalculator] = None

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
     * Set a signature calculator for the request. This is usually used for authentication,
     * for example for OAuth.
     */
    def sign(calculator: SignatureCalculator) = {
      this.calculator = Some(calculator)
      this
    }

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
      calculator.map(_.sign(this))
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

package ws {

  trait SignatureCalculator {
    def sign(request: WS.WSRequest)
  }

}

