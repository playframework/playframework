package play.api.mvc

import play.api.libs.iteratee._
import play.api.http._
import play.api.http.HeaderNames._
import play.api.http.HttpProtocol._
import play.api.{ Application, Play }
import play.api.i18n.Lang

import scala.concurrent.{ Future, ExecutionContext, Promise }

import play.core.Execution.Implicits._
import play.api.libs.concurrent.Execution.defaultContext

/**
 * A simple HTTP response header, used for standard responses.
 *
 * @param status the response status, e.g. ‘200 OK’
 * @param headers the HTTP headers
 */
case class ResponseHeader(status: Int, headers: Map[String, String] = Map.empty) {

  override def toString = {
    status + ", " + headers
  }

}

/**
 * Any Action result.
 */
sealed trait Result extends NotNull with WithHeaders[Result]

sealed trait WithHeaders[+A <: Result] {
  /**
   * Adds HTTP headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers the headers to add to this result.
   * @return the new result
   */
  def withHeaders(headers: (String, String)*): A

  /**
   * Adds cookies to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withCookies(Cookie("theme", "blue"))
   * }}}
   *
   * @param cookies the cookies to add to this result
   * @return the new result
   */
  def withCookies(cookies: Cookie*): A

  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingCookies("theme")
   * }}}
   *
   * @param names the names of the cookies to discard along to this result
   * @return the new result
   */
  @deprecated("This method can only discard cookies on the / path with no domain and without secure set.  Use discardingCookies(DiscardingCookie*) instead.", "2.1")
  def discardingCookies(name: String, names: String*): A = discardingCookies((name :: names.toList).map(n => DiscardingCookie(n)): _*)

  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingCookies(DiscardingCookie("theme"))
   * }}}
   *
   * @param cookies the cookies to discard along to this result
   * @return the new result
   */
  def discardingCookies(cookies: DiscardingCookie*): A

  /**
   * Sets a new session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession(session + ("saidHello" -> "true"))
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: Session): A

  /**
   * Sets a new session for this result, discarding the existing session.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession("saidHello" -> "yes")
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: (String, String)*): A

  /**
   * Discards the existing session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withNewSession
   * }}}
   *
   * @return the new result
   */
  def withNewSession: A

  /**
   * Sets the user's language permanently for future requests by storing it in a cookie.
   *
   * For example:
   * {{{
   * implicit val lang = Lang("fr-FR")
   * Ok(Messages("hello.world")).withLang(lang)
   * }}}
   *
   * @param lang the language to store for the user
   * @return the new result
   */
  def withLang(lang: Lang)(implicit app: Application): A = withCookies(Cookie(Play.langCookieName, lang.code))

  /**
   * Clears the user's language by discarding the language cookie set by withLang
   *
   * For example:
   * {{{
   * Ok(Messages("hello.world")).clearingLang
   * }}}
   *
   * @return the new result
   */
  def clearingLang(implicit app: Application): A = discardingCookies(DiscardingCookie(Play.langCookieName))

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing(flash + ("success" -> "Done!"))
   * }}}
   *
   * @param flash the flash scope to set with this result
   * @return the new result
   */
  def flashing(flash: Flash): A

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing("success" -> "Done!")
   * }}}
   *
   * @param values the flash values to set with this result
   * @return the new result
   */
  def flashing(values: (String, String)*): A

  /**
   * Changes the result content type.
   *
   * For example:
   * {{{
   * Ok("<text>Hello world</text>").as("application/xml")
   * }}}
   *
   * @param contentType the new content type.
   * @return the new result
   */
  def as(contentType: String): A
}

/**
 * Helper utilities for Result values.
 */
@deprecated("In Play 2.3, SimpleResult will be the only type of result", "2.2.0")
object PlainResult {

  /**
   * Extractor:
   *
   * {{{
   * case Result(status, headers) => ...
   * }}}
   */
  def unapply(result: Result): Option[(Int, Map[String, String])] = result match {
    case r: PlainResult => Some((r.header.status, r.header.headers))
    case _ => None
  }

}

/**
 * A plain HTTP result.
 */
@deprecated("In Play 2.3, SimpleResult will be the only type of result", "2.2.0")
sealed trait PlainResult extends Result with WithHeaders[PlainResult] {

  /**
   * The response header
   */
  val header: ResponseHeader

}

/**
 * The connection semantics for the result.
 */
object HttpConnection extends Enumeration {
  type Connection = Value

  /**
   * Prefer to keep the connection alive.
   *
   * If no `Content-Length` header is present, and no `Transfer-Encoding` header is present, then the body will be
   * buffered for a maximum of one chunk from the enumerator, in an attempt to calculate the content length.  If the
   * enumerator contains more than one chunk, then the body will be sent chunked if the client is using HTTP 1.1,
   * or the body will be sent as is, but the connection will be closed after the body is sent.
   *
   * There are cases where the connection won't be kept alive.  These are as follows:
   *
   * - The protocol the client is using is HTTP 1.0 and the client hasn't sent a `Connection: keep-alive` header.
   * - The client has sent a `Connection: close` header.
   * - There is no `Content-Length` or `Transfer-Encoding` header present, the enumerator contains more than one chunk,
   *   and the protocol the client is using is HTTP 1.0, hence chunked encoding can't be used as a fallback.
   */
  val KeepAlive = Value

  /**
   * Close the connection once the response body has been sent.
   *
   * This will take precedence to any `Connection` header specified in the request.
   *
   * No buffering of the response will be attempted.  This means if the result contains no `Content-Length` header,
   * none will be calculated.
   */
  val Close = Value
}

/**
 * A simple result, which defines the response header and a body ready to send to the client.
 *
 * @param header the response header, which contains status code and HTTP headers
 * @param body the response body
 * @param connection the connection semantics to use
 */
case class SimpleResult(header: ResponseHeader, body: Enumerator[Array[Byte]],
    connection: HttpConnection.Connection = HttpConnection.KeepAlive) extends PlainResult with WithHeaders[SimpleResult] {

  /**
   * Adds headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers the headers to add to this result.
   * @return the new result
   */
  def withHeaders(headers: (String, String)*) = {
    copy(header = header.copy(headers = header.headers ++ headers))
  }

  /**
   * Adds cookies to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withCookies(Cookie("theme", "blue"))
   * }}}
   *
   * @param cookies the cookies to add to this result
   * @return the new result
   */
  def withCookies(cookies: Cookie*): SimpleResult = {
    withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), cookies))
  }

  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingCookies("theme")
   * }}}
   *
   * @param cookies the cookies to discard along to this result
   * @return the new result
   */
  def discardingCookies(cookies: DiscardingCookie*): SimpleResult = {
    withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), cookies.map(_.toCookie)))
  }

  /**
   * Sets a new session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession(session + ("saidHello" -> "true"))
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: Session): SimpleResult = {
    if (session.isEmpty) discardingCookies(Session.discard) else withCookies(Session.encodeAsCookie(session))
  }

  /**
   * Sets a new session for this result, discarding the existing session.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession("saidHello" -> "yes")
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: (String, String)*): SimpleResult = withSession(Session(session.toMap))

  /**
   * Discards the existing session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withNewSession
   * }}}
   *
   * @return the new result
   */
  def withNewSession: SimpleResult = withSession(Session())

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing(flash + ("success" -> "Done!"))
   * }}}
   *
   * @param flash the flash scope to set with this result
   * @return the new result
   */
  def flashing(flash: Flash): SimpleResult = {
    withCookies(Flash.encodeAsCookie(flash))
  }

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing("success" -> "Done!")
   * }}}
   *
   * @param values the flash values to set with this result
   * @return the new result
   */
  def flashing(values: (String, String)*): SimpleResult = flashing(Flash(values.toMap))

  /**
   * Changes the result content type.
   *
   * For example:
   * {{{
   * Ok("<text>Hello world</text>").as("application/xml")
   * }}}
   *
   * @param contentType the new content type.
   * @return the new result
   */
  def as(contentType: String): SimpleResult = withHeaders(CONTENT_TYPE -> contentType)

  override def toString = {
    "SimpleResult(" + header + ")"
  }

}

/**
 * A chunked result, which defines the response header and a chunks enumerator to send asynchronously to the client.
 *
 * @tparam A the response body content type.
 * @param header the response header, which contains status code and HTTP headers
 * @param chunks the chunks enumerator
 */
@deprecated("Use SimpleResult with Results.chunk enumeratee instead. Will be removed in Play 2.3.", "2.2.0")
class ChunkedResult[A](override val header: ResponseHeader, val chunks: Iteratee[A, Unit] => _)(implicit val writeable: Writeable[A]) extends SimpleResult(
  header = header.copy(headers = header.headers ++ writeable.contentType.map(ct => Map(
    CONTENT_TYPE -> ct,
    TRANSFER_ENCODING -> CHUNKED
  )).getOrElse(Map(
    TRANSFER_ENCODING -> CHUNKED
  ))
  ),
  body = new Enumerator[A] {
    // Since chunked result bodies are functions of iteratee to unit, not a future, we need to do this in
    // a somewhat messy way
    def apply[C](i: Iteratee[A, C]): Future[Iteratee[A, C]] = {
      val doneIteratee = Promise[Iteratee[A, C]]
      chunks(i.map { done =>
        doneIteratee.success(Done[A, C](done)).asInstanceOf[Unit]
      })
      doneIteratee.future
    }
  } &> writeable.toEnumeratee &> Results.chunk,
  connection = HttpConnection.KeepAlive) {

  /** The body content type. */
  type BODY_CONTENT = A
}

@deprecated("Use SimpleResult with Results.chunk enumeratee instead. Will be removed in Play 2.3.", "2.2.0")
object ChunkedResult {
  @deprecated("Use SimpleResult with Results.chunk enumeratee instead. Will be removed in Play 2.3.", "2.2.0")
  def apply[A](header: ResponseHeader, chunks: Iteratee[A, Unit] => _)(implicit writeable: Writeable[A]) =
    new ChunkedResult(header, chunks)
}

/**
 * An `AsyncResult` handles a `Promise` of result for cases where the result is not ready yet.
 *
 * @param result the promise of result, which can be any other result type
 */
@deprecated("Use Future[SimpleResult] with Action.async action builder instead. Will be removed in Play 2.3.", "2.2.0")
case class AsyncResult(result: Future[Result]) extends Result with WithHeaders[AsyncResult] {

  /**
   * Apply some transformation to this `AsyncResult`
   *
   * @param f The transformation function
   * @return The transformed `AsyncResult`
   */
  def transform(f: PlainResult => Result)(implicit ec: ExecutionContext): AsyncResult = {
    implicit val functionContext = ec.prepare()
    AsyncResult(result.map {
      case AsyncResult(r) => AsyncResult(r.map {
        case r: PlainResult => f(r)
        case r: AsyncResult => r.transform(f)(functionContext)
      }(functionContext))
      case r: PlainResult => f(r)
    }(functionContext))
  }

  def unflatten: Future[SimpleResult] = result.flatMap {
    case r: SimpleResult => Future.successful(r)
    case r @ AsyncResult(_) => r.unflatten
  }

  def map(f: Result => Result)(implicit ec: ExecutionContext): AsyncResult = AsyncResult(result.map(f)(ec.prepare()))

  /**
   * Adds headers to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withHeaders(ETAG -> "0")
   * }}}
   *
   * @param headers the headers to add to this result.
   * @return the new result
   */
  def withHeaders(headers: (String, String)*): AsyncResult = {
    map(_.withHeaders(headers: _*))
  }

  /**
   * Adds cookies to this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withCookies(Cookie("theme", "blue"))
   * }}}
   *
   * @param cookies the cookies to add to this result
   * @return the new result
   */
  def withCookies(cookies: Cookie*): AsyncResult = {
    map(_.withCookies(cookies: _*))
  }

  /**
   * Discards cookies along this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").discardingCookies("theme")
   * }}}
   *
   * @param cookies the cookies to discard along to this result
   * @return the new result
   */
  def discardingCookies(cookies: DiscardingCookie*): AsyncResult = {
    map(_.discardingCookies(cookies: _*))
  }

  /**
   * Sets a new session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession(session + ("saidHello" -> "true"))
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: Session): AsyncResult = {
    map(_.withSession(session))
  }

  /**
   * Sets a new session for this result, discarding the existing session.
   *
   * For example:
   * {{{
   * Ok("Hello world").withSession("saidHello" -> "yes")
   * }}}
   *
   * @param session the session to set with this result
   * @return the new result
   */
  def withSession(session: (String, String)*): AsyncResult = {
    map(_.withSession(session: _*))
  }

  /**
   * Discards the existing session for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").withNewSession
   * }}}
   *
   * @return the new result
   */
  def withNewSession: AsyncResult = {
    map(_.withNewSession)
  }

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing(flash + ("success" -> "Done!"))
   * }}}
   *
   * @param flash the flash scope to set with this result
   * @return the new result
   */
  def flashing(flash: Flash): AsyncResult = {
    map(_.flashing(flash))
  }

  /**
   * Adds values to the flash scope for this result.
   *
   * For example:
   * {{{
   * Ok("Hello world").flashing("success" -> "Done!")
   * }}}
   *
   * @param values the flash values to set with this result
   * @return the new result
   */
  def flashing(values: (String, String)*): AsyncResult = {
    map(_.flashing(values: _*))
  }

  /**
   * Changes the result content type.
   *
   * For example:
   * {{{
   * Ok("<text>Hello world</text>").as("application/xml")
   * }}}
   *
   * @param contentType the new content type.
   * @return the new result
   */
  def as(contentType: String): AsyncResult = {
    map(_.as(contentType))
  }

}

/**
 * A Codec handle the conversion of String to Byte arrays.
 *
 * @param charset The charset to be sent to the client.
 * @param encode The transformation function.
 */
case class Codec(val charset: String)(val encode: String => Array[Byte], val decode: Array[Byte] => String)

/**
 * Default Codec support.
 */
object Codec {

  /**
   * Create a Codec from an encoding already supported by the JVM.
   */
  def javaSupported(charset: String) = Codec(charset)(str => str.getBytes(charset), bytes => new String(bytes, charset))

  /**
   * Codec for UTF-8
   */
  implicit val utf_8 = javaSupported("utf-8")

  /**
   * Codec for ISO-8859-1
   */
  val iso_8859_1 = javaSupported("iso-8859-1")

}

/** Helper utilities to generate results. */
object Results extends Results {

  /** Empty result, i.e. nothing to send. */
  case class EmptyContent()

}

/** Helper utilities to generate results. */
trait Results {

  import play.api.http.Status._

  /**
   * Generates default `SimpleResult` from a content type, headers and content.
   *
   * @param status the HTTP response status, e.g ‘200 OK’
   */
  class Status(status: Int) extends SimpleResult(header = ResponseHeader(status), body = Enumerator.empty,
    connection = HttpConnection.KeepAlive) {

    /**
     * Set the result's content.
     *
     * @param content The content to send.
     */
    def apply[C](content: C)(implicit writeable: Writeable[C]): SimpleResult = {
      SimpleResult(
        ResponseHeader(status, writeable.contentType.map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty)),
        Enumerator(writeable.transform(content))
      )
    }

    /**
     * Send a file.
     *
     * @param content The file to send.
     * @param inline Use Content-Disposition inline or attachment.
     * @param fileName function to retrieve the file name (only used for Content-Disposition attachment).
     */
    def sendFile(content: java.io.File, inline: Boolean = false, fileName: java.io.File => String = _.getName, onClose: () => Unit = () => ()): SimpleResult = {
      val name = fileName(content)
      SimpleResult(
        ResponseHeader(OK, Map(
          CONTENT_LENGTH -> content.length.toString,
          CONTENT_TYPE -> play.api.libs.MimeTypes.forFileName(name).getOrElse(play.api.http.ContentTypes.BINARY)
        ) ++ (if (inline) Map.empty else Map(CONTENT_DISPOSITION -> ("""attachment; filename="%s"""".format(name))))),
        Enumerator.fromFile(content) &> Enumeratee.onIterateeDone(onClose)(defaultContext)
      )
    }

    /**
     * Stream the results content as chunked.
     *
     * @param content Enumerator providing the content to stream.
     */
    @deprecated("Use Status.chunked instead", "2.3.0")
    def stream[C](content: Enumerator[C])(implicit writeable: Writeable[C]): SimpleResult = chunked(content)

    /**
     * Feed the content as the response, using chunked transfer encoding.
     *
     * Chunked transfer encoding is only supported for HTTP 1.1 clients.  If the client is an HTTP 1.0 client, Play will
     * instead return a 505 error code.
     *
     * Chunked encoding allows the server to send a response where the content length is not known, or for potentially
     * infinite streams, while still allowing the connection to be kept alive and reused for the next request.
     *
     * @param content Enumerator providing the content to stream.
     */
    def chunked[C](content: Enumerator[C])(implicit writeable: Writeable[C]): SimpleResult = {
      SimpleResult(header = ResponseHeader(status,
        writeable.contentType.map(ct => Map(
          CONTENT_TYPE -> ct,
          TRANSFER_ENCODING -> CHUNKED
        )).getOrElse(Map(
          TRANSFER_ENCODING -> CHUNKED
        ))
      ),
        body = content &> writeable.toEnumeratee &> chunk,
        connection = HttpConnection.KeepAlive)
    }

    /**
     * Feed the content as the response.
     *
     * The connection will be closed after the response is sent, regardless of whether there is a content length or
     * transfer encoding defined.
     *
     * @param content Enumerator providing the content to stream.
     */
    def feed[C](content: Enumerator[C])(implicit writeable: Writeable[C]): SimpleResult = {
      SimpleResult(
        header = ResponseHeader(status, writeable.contentType.map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty)),
        body = content &> writeable.toEnumeratee,
        connection = HttpConnection.Close
      )
    }

    /**
     * Set the result's content as chunked.
     *
     * @param content A function that will give you the Iteratee to write in once ready.
     */
    @deprecated("Use stream(Enumerator) instead.  Will be removed in Play 2.3.", "2.2.0")
    def stream[C](content: Iteratee[C, Unit] => Unit)(implicit writeable: Writeable[C]): ChunkedResult[C] = {
      ChunkedResult(
        header = ResponseHeader(status, writeable.contentType.map(ct => Map(CONTENT_TYPE -> ct)).getOrElse(Map.empty)),
        content)
    }

  }

  /**
   * Implements HTTP chunked transfer encoding.
   */
  def chunk: Enumeratee[Array[Byte], Array[Byte]] = chunk(None)

  /**
   * Implements HTTP chunked transfer encoding.
   *
   * @param trailers An optional trailers iteratee.  If supplied, this will be zipped with the output iteratee, so that
   *                 it can calculate some trailing headers, which will be included with the last chunk.
   */
  def chunk(trailers: Option[Iteratee[Array[Byte], Seq[(String, String)]]] = None): Enumeratee[Array[Byte], Array[Byte]] = {

    // Enumeratee that formats each chunk.
    val formatChunks = Enumeratee.map[Array[Byte]] { data =>
      // This will be much nicer if we ever move to ByteString
      val chunkSize = Integer.toHexString(data.length).getBytes("UTF-8")
      // Length of chunk is the digits in chunk size, plus the data length, plus 2 CRLF pairs
      val chunk = new Array[Byte](chunkSize.length + data.length + 4)
      System.arraycopy(chunkSize, 0, chunk, 0, chunkSize.length)
      chunk(chunkSize.length) = '\r'
      chunk(chunkSize.length + 1) = '\n'
      System.arraycopy(data, 0, chunk, chunkSize.length + 2, data.length)
      chunk(chunk.length - 2) = '\r'
      chunk(chunk.length - 1) = '\n'
      chunk
    }

    // The actual enumeratee, which applies the formatting enumeratee maybe zipped with the trailers iteratee, and also
    // adds the last chunk.
    new Enumeratee[Array[Byte], Array[Byte]] {
      def applyOn[A](inner: Iteratee[Array[Byte], A]) = {

        val chunkedInner: Iteratee[Array[Byte], Iteratee[Array[Byte], A]] =
          // First filter out empty chunks - an empty chunk signifies end of stream in chunked transfer encoding
          Enumeratee.filterNot[Array[Byte]](_.isEmpty) ><>
            // Apply the chunked encoding
            formatChunks ><>
            // Don't feed EOF into the iteratee - so we can feed the last chunk ourselves later
            Enumeratee.passAlong &>
            // And apply the inner iteratee
            inner

        trailers match {
          case Some(trailersIteratee) => {
            // Zip the trailers iteratee with the inner iteratee
            Enumeratee.zipWith(chunkedInner, trailersIteratee) { (it, trailers) =>
              // Create last chunk
              val lastChunk = trailers.map(t => t._1 + ": " + t._2 + "\r\n").mkString("0\r\n", "", "\r\n").getBytes("UTF-8")
              Iteratee.flatten(Enumerator(lastChunk) >>> Enumerator.eof |>> it)
            }
          }
          case None => {
            chunkedInner.map { it =>
              // Feed last chunk with no trailers
              Iteratee.flatten(Enumerator("0\r\n\r\n".getBytes("UTF-8")) >>> Enumerator.eof |>> it)
            }
          }
        }
      }
    }
  }

  /**
   * Dechunks a chunked transfer encoding stream.
   *
   * Chunks may span multiple elements in the stream.
   */
  def dechunk: Enumeratee[Array[Byte], Array[Byte]] = {

    // convenience method
    def elOrEmpty(data: Array[Byte]) = {
      if (data.length == 0) Input.Empty else Input.El(data)
    }

    // Read a line. Is quite permissive, a line is anything terminated by LF, and trims the result.
    def readLine(line: List[Array[Byte]] = Nil): Iteratee[Array[Byte], String] = Cont {
      case Input.El(data) => {
        val s = data.takeWhile(_ != '\n')
        if (s.length == data.length) {
          readLine(s :: line)
        } else {
          Done(new String(Array.concat((s :: line).reverse: _*), "UTF-8").trim(), elOrEmpty(data.drop(s.length + 1)))
        }
      }
      case Input.EOF => {
        Error("EOF found while reading line", Input.Empty)
      }
      case Input.Empty => readLine(line)
    }

    // Read the data part of a chunk of the given size
    def readChunkData(size: Int, chunk: List[Array[Byte]] = Nil): Iteratee[Array[Byte], Array[Byte]] = Cont {
      case Input.El(data) => {
        if (data.length >= size) {
          Done(Array.concat((data.take(size) :: chunk).reverse: _*), elOrEmpty(data.drop(size)))
        } else {
          readChunkData(size - data.length, data :: chunk)
        }
      }
      case Input.EOF => {
        Error("EOF found while reading chunk", Input.Empty)
      }
      case Input.Empty => readChunkData(size, chunk)
    }

    // Read a chunk of the given size
    def readChunk(size: Int) = for {
      chunk <- readChunkData(size)
      // Following every chunk data is a newline - read it
      _ <- readLine()
    } yield chunk

    // Read the last chunk. Produces the trailers.
    def readLastChunk: Iteratee[Array[Byte], List[(String, String)]] = for {
      trailer <- readLine(Nil)
      trailers <- if (trailer.length > 0) readLastChunk else Done[Array[Byte], List[(String, String)]](List.empty[(String, String)])
    } yield {
      trailer.split("""\s*:\s*""", 2) match {
        case Array(key, value) => (key -> value) :: trailers
        case Array(key) => (key -> "") :: trailers
      }
    }

    // A chunk parser, produces elements that are either chunks or the last chunk trailers
    val chunkParser: Iteratee[Array[Byte], Either[Array[Byte], Seq[(String, String)]]] = for {
      size <- readLine().map { line =>
        def isHexDigit(c: Char) = Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
        // Parse the size. Ignore any extensions.
        Integer.parseInt(line.takeWhile(isHexDigit), 16)
      }
      chunk <- if (size > 0) readChunk(size).map(Left.apply) else readLastChunk.map(Right.apply)
    } yield chunk

    Enumeratee.grouped(chunkParser) ><>
      Enumeratee.takeWhile[Either[Array[Byte], Seq[(String, String)]]](_.isLeft) ><>
      Enumeratee.map {
        case Left(data) => data
      }
  }

  @deprecated("Use Action.async to build async actions instead", "2.2.0")
  def Async(promise: Future[Result]) = AsyncResult(promise)

  /** Generates a ‘200 OK’ result. */
  val Ok = new Status(OK)

  /** Generates a ‘201 CREATED’ result. */
  val Created = new Status(CREATED)

  /** Generates a ‘202 ACCEPTED’ result. */
  val Accepted = new Status(ACCEPTED)

  /** Generates a ‘203 NON_AUTHORITATIVE_INFORMATION’ result. */
  val NonAuthoritativeInformation = new Status(NON_AUTHORITATIVE_INFORMATION)

  /** Generates a ‘204 NO_CONTENT’ result. */
  val NoContent = SimpleResult(header = ResponseHeader(NO_CONTENT), body = Enumerator.empty,
    connection = HttpConnection.KeepAlive)

  /** Generates a ‘205 RESET_CONTENT’ result. */
  val ResetContent = SimpleResult(header = ResponseHeader(RESET_CONTENT), body = Enumerator.empty,
    connection = HttpConnection.KeepAlive)

  /** Generates a ‘206 PARTIAL_CONTENT’ result. */
  val PartialContent = new Status(PARTIAL_CONTENT)

  /** Generates a ‘207 MULTI_STATUS’ result. */
  val MultiStatus = new Status(MULTI_STATUS)

  /**
   * Generates a ‘301 MOVED_PERMANENTLY’ simple result.
   *
   * @param url the URL to redirect to
   */
  def MovedPermanently(url: String): SimpleResult = Redirect(url, MOVED_PERMANENTLY)

  /**
   * Generates a ‘302 FOUND’ simple result.
   *
   * @param url the URL to redirect to
   */
  def Found(url: String): SimpleResult = Redirect(url, FOUND)

  /**
   * Generates a ‘303 SEE_OTHER’ simple result.
   *
   * @param url the URL to redirect to
   */
  def SeeOther(url: String): SimpleResult = Redirect(url, SEE_OTHER)

  /** Generates a ‘304 NOT_MODIFIED’ result. */
  val NotModified = SimpleResult(header = ResponseHeader(NOT_MODIFIED), body = Enumerator.empty,
    connection = HttpConnection.KeepAlive)

  /**
   * Generates a ‘307 TEMPORARY_REDIRECT’ simple result.
   *
   * @param url the URL to redirect to
   */
  def TemporaryRedirect(url: String): SimpleResult = Redirect(url, TEMPORARY_REDIRECT)

  /** Generates a ‘400 BAD_REQUEST’ result. */
  val BadRequest = new Status(BAD_REQUEST)

  /** Generates a ‘401 UNAUTHORIZED’ result. */
  val Unauthorized = new Status(UNAUTHORIZED)

  /** Generates a ‘403 FORBIDDEN’ result. */
  val Forbidden = new Status(FORBIDDEN)

  /** Generates a ‘404 NOT_FOUND’ result. */
  val NotFound = new Status(NOT_FOUND)

  /** Generates a ‘405 METHOD_NOT_ALLOWED’ result. */
  val MethodNotAllowed = new Status(METHOD_NOT_ALLOWED)

  /** Generates a ‘406 NOT_ACCEPTABLE’ result. */
  val NotAcceptable = new Status(NOT_ACCEPTABLE)

  /** Generates a ‘408 REQUEST_TIMEOUT’ result. */
  val RequestTimeout = new Status(REQUEST_TIMEOUT)

  /** Generates a ‘409 CONFLICT’ result. */
  val Conflict = new Status(CONFLICT)

  /** Generates a ‘410 GONE’ result. */
  val Gone = new Status(GONE)

  /** Generates a ‘412 PRECONDITION_FAILED’ result. */
  val PreconditionFailed = new Status(PRECONDITION_FAILED)

  /** Generates a ‘413 REQUEST_ENTITY_TOO_LARGE’ result. */
  val EntityTooLarge = new Status(REQUEST_ENTITY_TOO_LARGE)

  /** Generates a ‘414 REQUEST_URI_TOO_LONG’ result. */
  val UriTooLong = new Status(REQUEST_URI_TOO_LONG)

  /** Generates a ‘415 UNSUPPORTED_MEDIA_TYPE’ result. */
  val UnsupportedMediaType = new Status(UNSUPPORTED_MEDIA_TYPE)

  /** Generates a ‘417 EXPECTATION_FAILED’ result. */
  val ExpectationFailed = new Status(EXPECTATION_FAILED)

  /** Generates a ‘422 UNPROCESSABLE_ENTITY’ result. */
  val UnprocessableEntity = new Status(UNPROCESSABLE_ENTITY)

  /** Generates a ‘423 LOCKED’ result. */
  val Locked = new Status(LOCKED)

  /** Generates a ‘424 FAILED_DEPENDENCY’ result. */
  val FailedDependency = new Status(FAILED_DEPENDENCY)

  /** Generates a ‘429 TOO_MANY_REQUEST’ result. */
  val TooManyRequest = new Status(TOO_MANY_REQUEST)

  /** Generates a ‘500 INTERNAL_SERVER_ERROR’ result. */
  val InternalServerError = new Status(INTERNAL_SERVER_ERROR)

  /** Generates a ‘501 NOT_IMPLEMENTED’ result. */
  val NotImplemented = new Status(NOT_IMPLEMENTED)

  /** Generates a ‘502 BAD_GATEWAY’ result. */
  val BadGateway = new Status(BAD_GATEWAY)

  /** Generates a ‘503 SERVICE_UNAVAILABLE’ result. */
  val ServiceUnavailable = new Status(SERVICE_UNAVAILABLE)

  /** Generates a ‘504 GATEWAY_TIMEOUT’ result. */
  val GatewayTimeout = new Status(GATEWAY_TIMEOUT)

  /** Generates a ‘505 HTTP_VERSION_NOT_SUPPORTED’ result. */
  val HttpVersionNotSupported = new Status(HTTP_VERSION_NOT_SUPPORTED)

  /** Generates a ‘507 INSUFFICIENT_STORAGE’ result. */
  val InsufficientStorage = new Status(INSUFFICIENT_STORAGE)

  /**
   * Generates a simple result.
   *
   * @param code the status code
   */
  def Status(code: Int) = new Status(code)

  /**
   * Generates a redirect simple result.
   *
   * @param url the URL to redirect to
   * @param status HTTP status
   */
  def Redirect(url: String, status: Int): SimpleResult = Redirect(url, Map.empty, status)

  /**
   * Generates a redirect simple result.
   *
   * @param url the URL to redirect to
   * @param queryString queryString parameters to add to the queryString
   * @param status HTTP status
   */
  def Redirect(url: String, queryString: Map[String, Seq[String]] = Map.empty, status: Int = SEE_OTHER) = {
    import java.net.URLEncoder
    val fullUrl = url + Option(queryString).filterNot(_.isEmpty).map { params =>
      (if (url.contains("?")) "&" else "?") + params.toSeq.flatMap { pair =>
        pair._2.map(value => (pair._1 + "=" + URLEncoder.encode(value, "utf-8")))
      }.mkString("&")
    }.getOrElse("")
    Status(status).withHeaders(LOCATION -> fullUrl)
  }

  /**
   * Generates a redirect simple result.
   *
   * @param call Call defining the URL to redirect to, which typically comes from the reverse router
   */
  def Redirect(call: Call): SimpleResult = Redirect(call.url)

}
