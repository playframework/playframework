package play.api

import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._

import com.ning.http.client.{
  AsyncHttpClient,
  RequestBuilderBase,
  FluentCaseInsensitiveStringsMap,
  HttpResponseBodyPart,
  HttpResponseHeaders,
  HttpResponseStatus,
  Response => AHCResponse
}

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
  import com.ning.http.client.Realm.{ AuthScheme, RealmBuilder }

  lazy val client = new AsyncHttpClient()

  /**
   * Prepare a new request. You can then construct it by chaining calls.
   * @param url the URL to request
   */
  def url(url: String) = new WSRequest().setUrl(url)

  /**
   * A generic class for Request builders.
   * T is the type of request, R is the type of response.
   */
  abstract class WSRequestBase[T <: WSRequestBase[T, R], R](clazz: Class[T]) extends RequestBuilderBase[T](clazz, "GET") {

    import scala.collection.JavaConversions
    import scala.collection.JavaConversions._

    protected var calculator: Option[SignatureCalculator] = None
    protected var headers: Map[String, Seq[String]] = Map()
    protected var _url: String = null
    protected var _method = "GET"

    /**
     * Perform a GET on the request asynchronously.
     */
    def get(): Promise[R] = execute("GET")

    def getStream(): Promise[StreamedResponse] = executeStream("GET")

    /**
     * Perform a POST on the request asynchronously.
     */
    def post(): Promise[R] = execute("POST")

    def postStream(): Promise[StreamedResponse] = executeStream("POST")

    /**
     * Perform a PUT on the request asynchronously.
     */
    def put(): Promise[R] = execute("PUT")

    def putStream(): Promise[StreamedResponse] = executeStream("PUT")

    /**
     * Perform a DELETE on the request asynchronously.
     */
    def delete(): Promise[R] = execute("DELETE")

    /**
     * Perform a HEAD on the request asynchronously.
     */
    def head(): Promise[R] = execute("HEAD")

    /**
     * Perform a OPTIONS on the request asynchronously.
     */
    def options(): Promise[R] = execute("OPTION")

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

    override def setHeader(name: String, value: String) = {
      headers = headers + (name -> List(value))
      super.setHeader(name, value)
    }

    override def addHeader(name: String, value: String) = {
      headers = headers + (name -> (headers.get(name).getOrElse(List()) :+ value))
      super.addHeader(name, value)
    }

    override def setHeaders(hdrs: FluentCaseInsensitiveStringsMap) = {
      headers = ningHeadersToMap(hdrs)
      super.setHeaders(hdrs)
    }

    override def setHeaders(hdrs: java.util.Map[String, java.util.Collection[String]]) = {
      headers = ningHeadersToMap(hdrs)
      super.setHeaders(hdrs)
    }

    override def setUrl(url: String) = {
      _url = url
      super.setUrl(url)
    }

    override def setMethod(method: String) = {
      _method = method
      super.setMethod(method)
    }

    /**
     * Return the current headers of the request being constructed
     */
    def allHeaders: Map[String, Seq[String]] =
      JavaConversions.mapAsScalaMap(request.getHeaders()).map { entry => (entry._1, entry._2.toSeq) }.toMap

    def header(name: String): Option[String] = headers.get(name).flatMap(_.headOption)

    def method: String = _method

    def url: String = _url

    private def ningHeadersToMap(headers: java.util.Map[String, java.util.Collection[String]]) =
      JavaConversions.mapAsScalaMap(headers).map { entry => (entry._1, entry._2.toSeq) }.toMap

    private def ningHeadersToMap(headers: FluentCaseInsensitiveStringsMap) =
      JavaConversions.mapAsScalaMap(headers).map { entry => (entry._1, entry._2.toSeq) }.toMap

    protected def execute(method: String): Promise[R] = {
      import com.ning.http.client.AsyncCompletionHandler
      var result = Promise[R]()
      var request = this.setMethod(method).build()
      calculator.map(_.sign(this))
      WS.client.executeRequest(request, new AsyncCompletionHandler[AHCResponse]() {
        override def onCompleted(response: AHCResponse) = {
          result.redeem(wrapResponse(response))
          response
        }
        override def onThrowable(t: Throwable) = {
          result.redeem(throw t)
        }
      })
      result
    }

    protected def wrapResponse(ahcResponse: AHCResponse): R

    private def executeStream(method: String): Promise[StreamedResponse] = {
      import com.ning.http.client.AsyncHandler
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
          if (iteratee != null) {
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
          } else {
            // The Iteratee has not been plugged yet - ignore the chunk and wait for the Iteratee
            STATE.CONTINUE
          }
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

  class WSRequest extends WS.WSRequestBase[WSRequest, ws.Response](classOf[WSRequest]) {

    override def wrapResponse(ahcResponse: AHCResponse) = new ws.Response(ahcResponse)

  }

}

package ws {

  class WSResponse(ahcResponse: AHCResponse) {

    def getAHCResponse = ahcResponse

    def status = ahcResponse.getStatusCode();

    def header(key: String) = ahcResponse.getHeader(key)

    lazy val body: String = ahcResponse.getResponseBody()

  }

  class Response(ahcResponse: AHCResponse) extends WSResponse(ahcResponse) {
    import scala.xml._

    lazy val xml = XML.loadString(body)

    /**
     * Return the body as a JValue.
     */
    lazy val json = play.api.Json.parse(body)

  }

  case class StreamedResponse(status: Int, headers: Map[String, Seq[String]], chunks: Enumerator[Array[Byte]])

  trait SignatureCalculator {
    def sign(request: WS.WSRequestBase[_, _])
  }

}

