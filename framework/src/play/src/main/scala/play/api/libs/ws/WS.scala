package play.api.libs.ws

import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Input._
import play.api.libs.json._
import play.api.http.{ Writeable, ContentTypeOf }

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
 * Asynchronous API to to query web services, as an http client.
 *
 * Usage example:
 * {{{
 * WS.url("http://example.com/feed").get()
 * WS.url("http://example.com/item").post("content")
 * }}}
 *
 * The value returned is a Promise[Response],
 * and you should use Play's asynchronous mechanisms to use this response.
 *
 */
object WS {

  import com.ning.http.client.Realm.{ AuthScheme, RealmBuilder }

  /**
   * The underlying HTTP client.
   */
  lazy val client = new AsyncHttpClient()

  /**
   * Prepare a new request. You can then construct it by chaining calls.
   *
   * @param url the URL to request
   */
  def url(url: String): WSRequestHolder = WSRequestHolder(url, Map(), Map(), None, None)

  /**
   * A WS Request.
   */
  class WSRequest(_method: String, _auth: Option[Tuple3[String, String, AuthScheme]], _calc: Option[SignatureCalculator]) extends RequestBuilderBase[WSRequest](classOf[WSRequest], _method, false) {

    import scala.collection.JavaConverters._

    protected var calculator: Option[SignatureCalculator] = _calc

    protected var headers: Map[String, Seq[String]] = Map()

    protected var _url: String = null

    //this will do a java mutable set hence the {} response
    _auth.map(data => auth(data._1, data._2, data._3)).getOrElse({})

    /**
     * Add http auth headers
     */
    private def auth(username: String, password: String, scheme: AuthScheme): WSRequest = {
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
    def allHeaders: Map[String, Seq[String]] = {
      mapAsScalaMapConverter(request.getHeaders()).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
    }

    /**
     * Retrieve an HTTP header.
     */
    def header(name: String): Option[String] = headers.get(name).flatMap(_.headOption)

    /**
     * The HTTP method.
     */
    def method: String = _method

    /**
     * The URL
     */
    def url: String = _url

    private def ningHeadersToMap(headers: java.util.Map[String, java.util.Collection[String]]) =
      mapAsScalaMapConverter(headers).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap

    private def ningHeadersToMap(headers: FluentCaseInsensitiveStringsMap) =
      mapAsScalaMapConverter(headers).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap

    private[libs] def execute: Promise[Response] = {
      import com.ning.http.client.AsyncCompletionHandler
      var result = Promise[Response]()
      calculator.map(_.sign(this))
      WS.client.executeRequest(this.build(), new AsyncCompletionHandler[AHCResponse]() {
        override def onCompleted(response: AHCResponse) = {
          result.redeem(Response(response))
          response
        }
        override def onThrowable(t: Throwable) = {
          result.redeem(throw t)
        }
      })
      result
    }

    /**
     * Set an HTTP header.
     */
    override def setHeader(name: String, value: String) = {
      headers = headers + (name -> List(value))
      super.setHeader(name, value)
    }

    /**
     * Add an HTTP header (used for headers with mutiple values).
     */
    override def addHeader(name: String, value: String) = {
      headers = headers + (name -> (headers.get(name).getOrElse(List()) :+ value))
      super.addHeader(name, value)
    }

    /**
     * Defines the request headers.
     */
    override def setHeaders(hdrs: FluentCaseInsensitiveStringsMap) = {
      headers = ningHeadersToMap(hdrs)
      super.setHeaders(hdrs)
    }

    /**
     * Defines the request headers.
     */
    override def setHeaders(hdrs: java.util.Map[String, java.util.Collection[String]]) = {
      headers = ningHeadersToMap(hdrs)
      super.setHeaders(hdrs)
    }

    /**
     * Defines the request headers.
     */
    def setHeaders(hdrs: Map[String, Seq[String]]) = {
      headers = hdrs
      hdrs.foreach(header => header._2.foreach(value =>
        super.addHeader(header._1, value)
      ))
      this
    }

    /**
     * Defines the query string.
     */
    def setQueryString(queryString: Map[String, String]) = {
      queryString.foreach { param: (String, String) => this.addQueryParameter(param._1, param._2) }
      this
    }

    /**
     * Defines the URL.
     */
    override def setUrl(url: String) = {
      _url = url
      super.setUrl(url)
    }

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
   * A WS Request builder.
   */
  case class WSRequestHolder(url: String,
      headers: Map[String, Seq[String]],
      queryString: Map[String, String],
      calc: Option[SignatureCalculator],
      auth: Option[Tuple3[String, String, AuthScheme]]) {

    /**
     * sets the signature calculator for the request
     * @param calc
     */
    def sign(calc: SignatureCalculator): WSRequestHolder = this.copy(calc = Some(calc))

    /**
     * sets the authentication realm
     * @param calc
     */
    def withAuth(username: String, password: String, scheme: AuthScheme): WSRequestHolder =
      this.copy(auth = Some((username, password, scheme)))

    /**
     * adds any number of HTTP headers
     * @param hdrs
     */
    def withHeaders(hdrs: (String, String)*): WSRequestHolder = {
      val headers = hdrs.foldLeft(this.headers)((m, hdr) =>
        if (m.contains(hdr._1)) m.updated(hdr._1, m(hdr._1) :+ hdr._2)
        else (m + (hdr._1 -> Seq(hdr._2)))
      )
      this.copy(headers = headers)
    }

    /**
     * adds any number of query string parameters to the
     */
    def withQueryString(parameters: (String, String)*): WSRequestHolder =
      this.copy(queryString = parameters.foldLeft(queryString)((m, param) => m + param))

    /**
     * performs a get with supplied body
     */

    def get(): Promise[Response] = prepare("GET").execute

    /**
     * performs a get with supplied body
     * @param consumer that's handling the response
     */
    def get[A](consumer: ResponseHeaders => Iteratee[Array[Byte], A]): Promise[Iteratee[Array[Byte], A]] =
      prepare("GET").executeStream(consumer)

    /**
     * Perform a POST on the request asynchronously.
     */
    def post[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Response] = prepare("POST", body).execute

    /**
     * performs a POST with supplied body
     * @param consumer that's handling the response
     */
    def postAndRetrieveStream[A, T](body: T)(consumer: ResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Iteratee[Array[Byte], A]] = prepare("POST", body).executeStream(consumer)

    /**
     * Perform a PUT on the request asynchronously.
     */
    def put[T](body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Response] = prepare("PUT", body).execute

    /**
     * performs a PUT with supplied body
     * @param consumer that's handling the response
     */
    def putAndRetrieveStream[A, T](body: T)(consumer: ResponseHeaders => Iteratee[Array[Byte], A])(implicit wrt: Writeable[T], ct: ContentTypeOf[T]): Promise[Iteratee[Array[Byte], A]] = prepare("PUT", body).executeStream(consumer)

    /**
     * Perform a DELETE on the request asynchronously.
     */
    def delete(): Promise[Response] = prepare("DELETE").execute

    /**
     * Perform a HEAD on the request asynchronously.
     */
    def head(): Promise[Response] = prepare("HEAD").execute

    /**
     * Perform a OPTIONS on the request asynchronously.
     */
    def options(): Promise[Response] = prepare("OPTIONS").execute

    private def prepare(method: String) =
      new WSRequest(method, auth, calc).setUrl(url)
        .setHeaders(headers)
        .setQueryString(queryString)

    private def prepare[T](method: String, body: T)(implicit wrt: Writeable[T], ct: ContentTypeOf[T]) =
      new WSRequest(method, auth, calc).setUrl(url)
        .setHeaders(Map("Content-Type" -> Seq(ct.mimeType.getOrElse("text/plain"))) ++ headers)
        .setQueryString(queryString)
        .setBody(wrt.transform(body))

  }
}

/**
 * A WS HTTP response.
 */
case class Response(ahcResponse: AHCResponse) {

  import scala.xml._
  import play.api.libs.json._

  /**
   * Get the underlying response object.
   */
  def getAHCResponse = ahcResponse

  /**
   * The response status code.
   */
  def status: Int = ahcResponse.getStatusCode();

  /**
   * Get a response header.
   */
  def header(key: String): Option[String] = Option(ahcResponse.getHeader(key))

  /**
   * The response body as String.
   */
  lazy val body: String = ahcResponse.getResponseBody()

  /**
   * The response body as Xml.
   */
  lazy val xml: Elem = XML.loadString(body)

  /**
   * The response body as Json.
   */
  lazy val json: JsValue = Json.parse(body)

}

/**
 * An HTTP response header (the body has not been retrieved yet)
 */
case class ResponseHeaders(status: Int, headers: Map[String, Seq[String]])

/**
 * Sign a WS call.
 */
trait SignatureCalculator {

  /**
   * Sign it.
   */
  def sign(request: WS.WSRequest)

}

