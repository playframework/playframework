package play.api

import java.util.concurrent.Executor

import play.api.libs.concurrent._
import com.ning.http.client._

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

    private def execute(method: String): Promise[Response] = {
      play.Logger.error("Oh promises...")
      var result = Promise[Response]()
      WS.client.executeRequest(this.setMethod(method).build(), new AsyncCompletionHandler[Response]() {
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

