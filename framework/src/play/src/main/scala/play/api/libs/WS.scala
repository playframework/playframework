package play.api.libs

import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.json._

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
 * The value returned is a Promise[Response],
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
  def url(url: String) = WSRequestHolder(url)

  /**
   * A generic class for Request builders.
   * T is the type of request, R is the type of response.
   */
  abstract class WSRequestBase[T <: WSRequestBase[T, R], R](clazz: Class[T], _method: String, _auth:Option[Tuple3[String,String,AuthScheme]], _calc: Option[SignatureCalculator] ) extends RequestBuilderBase[T](clazz, _method) {

    import scala.collection.JavaConversions
    import scala.collection.JavaConversions._

    protected var calculator: Option[SignatureCalculator] = _calc

    protected var headers: Map[String, Seq[String]] = Map()

    protected var _url: String = null

    //this will do a java mutable set hence the {} repsonse
    _auth.map(data => auth(data._1,data._2,data._3)).getOrElse({})

    /**
     * Add http auth headers
     */
    private def auth(username: String, password: String, scheme: AuthScheme) = {
      this.setRealm((new RealmBuilder())
        .setScheme(scheme)
        .setPrincipal(username)
        .setPassword(password)
        .setUsePreemptiveAuth(true)
        .build())
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

    private[libs] def execute: Promise[R] = {
      import com.ning.http.client.AsyncCompletionHandler
      var result = Promise[R]()
      calculator.map(_.sign(this))
      WS.client.executeRequest(this.build(), new AsyncCompletionHandler[AHCResponse]() {
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

    def setHeaders(hdrs: Map[String, Seq[String]]) = {
      headers = hdrs
      hdrs.foreach(header => header._2.foreach (value =>
        super.addHeader(header._1, value)
      ))
      this
    }

    override def setUrl(url: String) = {
      _url = url
      super.setUrl(url)
    }

    protected def wrapResponse(ahcResponse: AHCResponse): R

    private[libs] def executeStream[A](consumer: ResponseHeaders => Iteratee[Array[Byte], A]): Promise[Iteratee[Array[Byte], A]] = {
      import com.ning.http.client.AsyncHandler
      var doneOrError = false
      calculator.map(_.sign(this))

      var statusCode = 0
      var iterateeP: STMPromise[Iteratee[Array[Byte], A]] = null
      var iteratee: Iteratee[Array[Byte], A] = null

      WS.client.executeRequest(this.build(), new AsyncHandler[Unit]() {
        import com.ning.http.client.AsyncHandler.STATE

        override def onStatusReceived(status: HttpResponseStatus) = {
          statusCode = status.getStatusCode()
          STATE.CONTINUE
        }

        override def onHeadersReceived(h: HttpResponseHeaders) = {
          val headers = h.getHeaders()
          iteratee = consumer(ResponseHeaders(statusCode, ningHeadersToMap(headers)))
          STATE.CONTINUE
        }

        override def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
          if (!doneOrError) {
            val nextIteratee = iteratee.pureFlatFold(
              // DONE
              (a, e) => {
                val it = Done(a, e)
                iterateeP.redeem(it)
                it
              },

              // CONTINUE
              k => {
                k(El(bodyPart.getBodyPartBytes()))
              },

              // ERROR
              (e, input) => {
                val it = Error(e, input)
                iterateeP.redeem(it)
                it
              })
            STATE.CONTINUE
          } else {
            iteratee = null
            STATE.ABORT
          }
        }

        override def onCompleted() = {
          Option(iteratee).map(iterateeP.redeem(_))
        }

        override def onThrowable(t: Throwable) = {
          iterateeP.redeem(throw t)
        }
      })
      iterateeP
    }

  }

  /**
   * stores a URL and provides the main API methods for WS
   *
   */
  case class WSRequestHolder(url: String) {

    private var _body: Array[Byte] = null

    private var _calc: Option[SignatureCalculator] = None

    private var _auth: Option[Tuple3[String,String,AuthScheme]] = None

    private var _headers: Map[String, Seq[String]] = Map()

    /**
     * sets the body for the request
     * @param data send as part of the request body
     */
    def body(data:Array[Byte]) = {
      _body = data;
      this 
    }

    def body(data: String) = {
      _body = data.getBytes
      this
    }

    /**
     * sets the signature calculator for the request
     * @param calc
     */
    def sign(calc: SignatureCalculator) = {
      _calc = Some(calc)
      this
    }

    /**
     * sets the authentication realm
     * @param calc
     */
    def auth(username: String, password: String, scheme: AuthScheme) = {
       _auth = Some( (username,password,scheme) )
       this
    }

    /**
     * adds any number of headers
     * @param hdrs
     */
    def headers(hdrs: (String, String)*) = {
        _headers = hdrs.foldLeft(_headers)((m, hdr) =>
            if (m.contains(hdr._1)) m.updated(hdr._1, m(hdr._1) :+ hdr._2)
            else (m + (hdr._1 -> Seq(hdr._2)))
         )
        this
    }

    /**
     * performs a get with supplied body
     */
    def get(): Promise[ws.Response] = new WSRequest("GET", _auth, _calc ).setUrl(url).execute

     /**
     * performs a get with supplied body
     * @param consumer that's handling the response
     */
    def get[A](consumer: ResponseHeaders => Iteratee[Array[Byte], A]): Promise[Iteratee[Array[Byte], A]] = new WSRequest("GET", _auth, _calc).setUrl(url).executeStream(consumer)

    /**
     * Perform a POST on the request asynchronously.
     */
    def post(): Promise[ws.Response] = new WSRequest("POST", _auth, _calc).setUrl(url).setBody(_body).setHeaders(_headers).execute

    /**
     * performs a POST with supplied body
     * @param consumer that's handling the response
     */
    def post[A](consumer: ResponseHeaders => Iteratee[Array[Byte], A]): Promise[Iteratee[Array[Byte], A]] = new WSRequest("POST", _auth, _calc).setUrl(url).setHeaders(_headers).executeStream(consumer)

    /**
     * Perform a PUT on the request asynchronously.
     */
    def put(): Promise[ws.Response] = new WSRequest("PUT", _auth, _calc).setUrl(url).setHeaders(_headers).setBody(_body).execute

     /**
     * performs a PUT with supplied body
     * @param consumer that's handling the response
     */
    def put[A](consumer: ResponseHeaders => Iteratee[Array[Byte], A]): Promise[Iteratee[Array[Byte], A]] = new WSRequest("PUT", _auth, _calc).setUrl(url).setHeaders(_headers).setBody(_body).executeStream(consumer)

    /**
     * Perform a DELETE on the request asynchronously.
     */
    def delete(): Promise[ws.Response] = new WSRequest("DELETE", _auth, _calc).setUrl(url).setHeaders(_headers).execute

    /**
     * Perform a HEAD on the request asynchronously.
     */
    def head(): Promise[ws.Response] = new WSRequest("HEAD", _auth, _calc).setUrl(url).setHeaders(_headers).execute

    /**
     * Perform a OPTIONS on the request asynchronously.
     */
    def options(): Promise[ws.Response] = new WSRequest("OPTIONS", _auth, _calc).setUrl(url).setHeaders(_headers).execute

  }

  class WSRequest(method: String, auth:Option[Tuple3[String,String,AuthScheme]], calc: Option[SignatureCalculator]) extends WS.WSRequestBase[WSRequest, ws.Response](classOf[WSRequest], method, auth, calc) {

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
    import play.api.libs.json._

    lazy val xml = XML.loadString(body)

    /**
     * Return the body as a JsValue.
     */
    lazy val json = Json.parse(body)

  }

  case class ResponseHeaders(status: Int, headers: Map[String, Seq[String]])

  trait SignatureCalculator {
    def sign(request: WS.WSRequestBase[_, _])
  }

}

