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
    def get(): HttpPromise[Response] = execute("GET")

    /**
     * Perform a POST on the request asynchronously.
     */
    def post(): HttpPromise[Response] = execute("POST")

    /**
     * Perform a PUT on the request asynchronously.
     */
    def put(): HttpPromise[Response] = execute("PUT")

    /**
     * Perform a DELETE on the request asynchronously.
     */
    def delete(): HttpPromise[Response] = execute("DELETE")

    /**
     * Perform a HEAD on the request asynchronously.
     */
    def head(): HttpPromise[Response] = execute("HEAD")

    /**
     * Perform a OPTIONS on the request asynchronously.
     */
    def options(): HttpPromise[Response] = execute("OPTION")

    private def execute(method: String) = new HttpPromise[Response](WS.client.executeRequest(this.setMethod("HEAD").build()))
  }

  class HttpPromise[A](listenable: ListenableFuture[A]) extends Promise[A] {

    override def onRedeem(k: A => Unit) = {
      listenable.addListener(new Runnable() {
        def run() = k(listenable.get())
      }, new Executor {
        def execute(r: Runnable) = r.run()
      })
    }

    override def extend[B](k: Function1[Promise[A], B]): Promise[B] = {
      val result = new STMPromise[B]()
      onRedeem(_ => result.redeem(k(this)))
      result
    }

    override def filter(p: A => Boolean): Promise[A] = {
      val result = new STMPromise[A]()
      onRedeem(a => if (p(a)) result.redeem(a))
      result
    }

    override def value: NotWaiting[A] = try {
      Redeemed(listenable.get())
    } catch { case e => Thrown(e) }

    override def map[B](f: A => B): Promise[B] = {
      val result = new STMPromise[B]()
      this.onRedeem(a => result.redeem(f(a)))
      result
    }

    override def flatMap[B](f: A => Promise[B]) = {
      throw new RuntimeException("Not implemented")
      val result = new STMPromise[B]()
      this.onRedeem(a => {
        f(a).onRedeem(b => {
          result.redeem(b)
        })
      })
      result
    }
  }

}
