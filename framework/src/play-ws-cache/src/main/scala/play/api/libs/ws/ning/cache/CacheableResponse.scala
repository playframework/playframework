package play.api.libs.ws.ning.cache

import java.io.{ ByteArrayInputStream, IOException, InputStream, OutputStream }
import java.net.MalformedURLException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util
import java.util.concurrent.atomic.AtomicBoolean

import com.ning.http.client._
import com.ning.http.client.cookie.Cookie
import com.ning.http.client.uri.Uri
import com.ning.http.util.AsyncHttpProviderUtils
import org.slf4j.LoggerFactory

class CacheableResponseBuilder {

  protected val builder = new Response.ResponseBuilder()

  protected var maybeStatus: Option[CacheableHttpResponseStatus] = None

  protected var maybeHeaders: Option[CacheableHttpResponseHeaders] = None

  def accumulate(responseStatus: HttpResponseStatus) = {
    val config = responseStatus.getConfig
    val uri = responseStatus.getUri
    val statusCode = responseStatus.getStatusCode
    val statusText = responseStatus.getStatusText
    val protocolText = responseStatus.getProtocolText
    val cacheableStatus = new CacheableHttpResponseStatus(uri, config, statusCode, statusText, protocolText)

    maybeStatus = Some(cacheableStatus)
    builder.accumulate(cacheableStatus)
  }

  def accumulate(responseHeaders: HttpResponseHeaders): Unit = {
    val trailing = responseHeaders.isTraillingHeadersReceived
    val headers = responseHeaders.getHeaders
    val cacheableHeaders = new CacheableHttpResponseHeaders(trailing, headers)
    maybeHeaders = Some(cacheableHeaders)
    builder.accumulate(cacheableHeaders)
  }

  def accumulate(bodyPart: HttpResponseBodyPart) = {
    val cacheableBodypart = new CacheableHttpResponseBodyPart(bodyPart.getBodyPartBytes, bodyPart.isLast)
    builder.accumulate(cacheableBodypart)
  }

  def reset() = {
    builder.reset()
    maybeHeaders = None
    maybeStatus = None
  }

  def build: CacheableResponse = {
    builder.build().asInstanceOf[CacheableResponse]
  }
}

case class CacheableResponse(status: CacheableHttpResponseStatus,
    headers: CacheableHttpResponseHeaders,
    bodyParts: util.List[CacheableHttpResponseBodyPart]) extends Response {

  import CacheableResponse._

  /**
   * The default charset of ISO-8859-1 for text media types has been
   * removed; the default is now whatever the media type definition says.
   * Likewise, special treatment of ISO-8859-1 has been removed from the
   * Accept-Charset header field.  (Section 3.1.1.3 and Section 5.3.3)
   */
  private val DEFAULT_CHARSET = "ISO-8859-1"

  private val uri: Uri = status.getUri
  private var content: String = null
  private val contentComputed: AtomicBoolean = new AtomicBoolean(false)

  def ahcStatus: HttpResponseStatus = status.asInstanceOf[HttpResponseStatus]

  def ahcHeaders: HttpResponseHeaders = headers.asInstanceOf[HttpResponseHeaders]

  def ahcbodyParts: util.List[HttpResponseBodyPart] = bodyParts.asInstanceOf[util.List[HttpResponseBodyPart]]

  def getStatusCode: Int = {
    status.getStatusCode
  }

  def getStatusText: String = {
    status.getStatusText
  }

  @throws(classOf[IOException])
  def getResponseBody: String = {
    if (logger.isTraceEnabled) {
      logger.trace("close: ")
    }
    getResponseBody(DEFAULT_CHARSET)
  }

  @throws(classOf[IOException])
  def getResponseBodyAsBytes: Array[Byte] = {
    AsyncHttpProviderUtils.contentToByte(ahcbodyParts)
  }

  @throws(classOf[IOException])
  def getResponseBodyAsByteBuffer: ByteBuffer = {
    ByteBuffer.wrap(getResponseBodyAsBytes)
  }

  @throws(classOf[IOException])
  def getResponseBody(charset: String): String = {
    if (logger.isTraceEnabled) {
      logger.trace("close: ")
    }
    if (!contentComputed.get) {
      content = AsyncHttpProviderUtils.contentToString(ahcbodyParts, Charset.forName(charset))
    }
    content
  }

  @throws(classOf[IOException])
  def getResponseBodyAsStream: InputStream = {
    if (logger.isTraceEnabled) {
      logger.trace("close: ")
    }
    if (contentComputed.get) {
      return new ByteArrayInputStream(content.getBytes(DEFAULT_CHARSET))
    }
    AsyncHttpProviderUtils.contentToInputStream(ahcbodyParts)
  }

  @throws(classOf[IOException])
  def getResponseBodyExcerpt(maxLength: Int): String = {
    getResponseBodyExcerpt(maxLength, DEFAULT_CHARSET)
  }

  @throws(classOf[IOException])
  def getResponseBodyExcerpt(maxLength: Int, charset: String): String = {
    ???
  }

  @throws(classOf[MalformedURLException])
  def getUri: Uri = {
    uri
  }

  def getContentType: String = {
    getHeader("Content-Type")
  }

  def getHeader(name: String): String = {
    headers.getHeaders.getFirstValue(name)
  }

  def getHeaders(name: String): util.List[String] = {
    headers.getHeaders.get(name)
  }

  def getHeaders: FluentCaseInsensitiveStringsMap = {
    headers.getHeaders
  }

  def isRedirected: Boolean = {
    status.getStatusCode match {
      case 301 | 302 | 303 | 307 | 308 =>
        true
      case _ =>
        false
    }
  }

  def getCookies: util.List[Cookie] = {
    ???
  }

  def hasResponseStatus: Boolean = {
    status != null
  }

  def hasResponseHeaders: Boolean = {
    headers != null
  }

  def hasResponseBody: Boolean = {
    !bodyParts.isEmpty
  }
  override def toString: String = {
    s"CacheableResponse(status = $status, headers = $headers, bodyParts = $bodyParts)"
  }

}

case class CacheableHttpResponseHeaders(trailingHeaders: Boolean, headers: FluentCaseInsensitiveStringsMap)
    extends HttpResponseHeaders(trailingHeaders) {

  override def getHeaders: FluentCaseInsensitiveStringsMap = headers

  override def toString: String = {
    s"CacheableHttpResponseHeaders(trailingHeaders = $trailingHeaders, headers = $headers)"
  }
}

object CacheableResponse {
  private val logger = LoggerFactory.getLogger("play.api.libs.ws.ning.cache.CacheableResponse")
}

class CacheableHttpResponseStatus(uri: Uri,
  config: AsyncHttpClientConfig,
  statusCode: Int,
  statusText: String,
  protocolText: String)
    extends HttpResponseStatus(uri, config) {
  override def getStatusCode: Int = statusCode

  override def getProtocolText: String = protocolText

  override def getProtocolMinorVersion: Int = -1

  override def getProtocolMajorVersion: Int = -1

  override def getStatusText: String = statusText

  override def getProtocolName: String = protocolText

  override def prepareResponse(headers: HttpResponseHeaders, bodyParts: util.List[HttpResponseBodyPart]): Response = {
    new CacheableResponse(this, headers.asInstanceOf[CacheableHttpResponseHeaders], bodyParts.asInstanceOf[util.List[CacheableHttpResponseBodyPart]])
  }

  override def toString = {
    s"CacheableHttpResponseStatus(code = $statusCode, text = $statusText)"
  }
}

class CacheableHttpResponseBodyPart(chunk: Array[Byte], last: Boolean) extends HttpResponseBodyPart(last) {

  override def getBodyPartBytes: Array[Byte] = chunk

  override def getBodyByteBuffer: ByteBuffer = ByteBuffer.wrap(chunk)

  override def writeTo(outputStream: OutputStream): Int = {
    outputStream.write(chunk)
    chunk.length
  }

  override def isLast: Boolean = super.isLast

  override def length(): Int = if (chunk != null) chunk.length else 0

  override def toString: String = {
    s"CacheableHttpResponseBodyPart(last = $last, chunk size = ${chunk.size})"
  }
}
