package play.api

import java.util.concurrent.Executor

import play.api.libs.concurrent._
import com.ning.http.client._

object WS {

  private lazy val client = new AsyncHttpClient()

  def url(url: String) = new WSRequest().setUrl(url)

  class WSRequest extends RequestBuilderBase[WSRequest](classOf[WSRequest], "GET") {

    def get(): HttpPromise[Response] = promise(WS.client.executeRequest(this.setMethod("GET").build()))
    def post(): HttpPromise[Response] = promise(WS.client.executeRequest(this.setMethod("POST").build()))
    def put(): HttpPromise[Response] = promise(WS.client.executeRequest(this.setMethod("PUT").build()))
    def delete(): HttpPromise[Response] = promise(WS.client.executeRequest(this.setMethod("DELETE").build()))
    def head(): HttpPromise[Response] = promise(WS.client.executeRequest(this.setMethod("HEAD").build()))
    def options(): HttpPromise[Response] = promise(WS.client.executeRequest(this.setMethod("OPTIONS").build()))

    def promise(listenable: ListenableFuture[Response]) = new HttpPromise[Response](listenable)
  }

  class HttpPromise[A](listenable: ListenableFuture[A]) extends Promise[A] {

    def apply: A = this.value match {
      case Redeemed(a) => a
      case Thrown(e) => throw e
    }

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
