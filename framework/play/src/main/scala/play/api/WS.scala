package play.api

import play.api.libs.concurrent._
import play.api.libs.iteratee._
import com.ning.http.client._
import com.ning.http.client.Realm.{ AuthScheme, RealmBuilder }

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

    def getStream(): Promise[StreamedResponse] = executeStream("GET")

    /**
     * Perform a POST on the request asynchronously.
     */
    def post(): Promise[Response] = execute("POST")

    def postStream(): Promise[StreamedResponse] = executeStream("POST")

    /**
     * Perform a PUT on the request asynchronously.
     */
    def put(): Promise[Response] = execute("PUT")

    def putStream(): Promise[StreamedResponse] = executeStream("PUT")

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
     * Add http auth headers
     */
    def auth(username: String, password: String, scheme: AuthScheme) = {
      this.setRealm((new RealmBuilder())
        .setScheme(scheme)
        .setPrincipal(username)
        .setPassword(password)
        .setUsePreemptiveAuth(true)
        .build())
      this
    }

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

    private def ningHeadersToMap(headers: FluentCaseInsensitiveStringsMap) =
      JavaConversions.mapAsScalaMap(headers).map { entry => (entry._1, entry._2.toSeq) }.toMap

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

    private def executeStream(method: String): Promise[StreamedResponse] = {
      var result = Promise[StreamedResponse]()
      var request = this.setMethod(method).build()
      calculator.map(_.sign(this))

      var statusCode = 0
      var iterateeP: STMPromise[Iteratee[Array[Byte], _]] = null
      var iteratee: Iteratee[Array[Byte], _] = null
      val enumerator = new Enumerator[Array[Byte]] {
        def apply[A, EE >: Array[Byte]](it: Iteratee[EE, A]): Promise[Iteratee[EE, A]] = {
          iteratee = it.asInstanceOf[Iteratee[Array[Byte], _]]
          val p = new STMPromise[Iteratee[EE, A]]()
          iterateeP = p.asInstanceOf[STMPromise[Iteratee[Array[Byte], _]]]
          p
        }
      }

      WS.client.executeRequest(request, new AsyncHandler[Unit]() {
        import com.ning.http.client.AsyncHandler.STATE

        override def onStatusReceived(status: HttpResponseStatus) = {
          statusCode = status.getStatusCode()
          STATE.CONTINUE
        }

        override def onHeadersReceived(h: HttpResponseHeaders) = {
          val headers = h.getHeaders()
          result.redeem(StreamedResponse(statusCode, ningHeadersToMap(headers), enumerator))
          STATE.CONTINUE
        }

        override def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
          iteratee.fold(
            // DONE
            (a, e) => {
              iterateeP.redeem(iteratee)
              iteratee = null
              Promise.pure(STATE.ABORT)
            },

            // CONTINUE
            k => {
              k(El(bodyPart.getBodyPartBytes()))
              Promise.pure(STATE.CONTINUE)
            },

            // ERROR
            (e, input) => {
              iterateeP.redeem(iteratee)
              iteratee = null
              Promise.pure(STATE.ABORT)
            }).value.get
        }

        override def onCompleted() = {
          Option(iteratee).map(iterateeP.redeem(_))
        }

        override def onThrowable(t: Throwable) = {
          iterateeP.redeem(throw t)
        }
      })
      result
    }

  }

}

package ws {

  case class StreamedResponse(status: Integer, headers: Map[String, Seq[String]], chunks: Enumerator[Array[Byte]])

  trait SignatureCalculator {
    def sign(request: WS.WSRequest)
  }

}

