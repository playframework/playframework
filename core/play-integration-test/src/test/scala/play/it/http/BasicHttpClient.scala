/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.io._
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

import scala.collection.immutable.TreeMap

import com.google.common.io.CharStreams
import play.api.http.HttpConfiguration
import play.api.libs.crypto.CookieSignerProvider
import play.api.mvc.DefaultCookieHeaderEncoding
import play.api.mvc.DefaultFlashCookieBaker
import play.api.mvc.DefaultSessionCookieBaker
import play.api.test.Helpers._
import play.core.server.common.ServerResultUtils
import play.core.utils.CaseInsensitiveOrdered

object BasicHttpClient {

  /**
   * Very basic HTTP client, for when we want to be very low level about our assertions.
   *
   * Can only work with requests that are entirely ascii, any binary or multi byte characters and it will break.
   *
   * @param port The port to connect to
   * @param checkClosed Whether to check if the channel is closed after receiving the responses
   * @param trickleFeed A timeout to use between sending request body chunks
   * @param requests The requests to make
   * @param secure Whether to use HTTPS
   * @return The parsed number of responses.  This may be more than the number of requests, if continue headers are sent.
   */
  def makeRequests(port: Int, checkClosed: Boolean = false, trickleFeed: Option[Long] = None, secure: Boolean = false)(
      requests: BasicRequest*
  ): Seq[BasicResponse] = {
    val client = new BasicHttpClient(port, secure)
    try {
      var requestNo = 0
      val responses = requests.flatMap { request =>
        requestNo += 1
        client.sendRequest(request, requestNo.toString, trickleFeed = trickleFeed)
      }

      if (checkClosed) {
        try {
          val line = client.reader.readLine()
          if (line != null) {
            throw new RuntimeException("Unexpected data after responses received: " + line)
          }
        } catch {
          case timeout: SocketTimeoutException => throw timeout
        }
      }

      responses
    } finally {
      client.close()
    }
  }

  def pipelineRequests(port: Int, requests: BasicRequest*): Seq[BasicResponse] = {
    val client = new BasicHttpClient(port, secure = false)

    try {
      var requestNo = 0
      requests.foreach { request =>
        requestNo += 1
        client.sendRequest(request, requestNo.toString, waitForResponses = false)
      }
      for (i <- 0 until requests.length) yield {
        client.readResponse(requestNo.toString)
      }
    } finally {
      client.close()
    }
  }
}

class BasicHttpClient(port: Int, secure: Boolean) {
  val s = createSocket
  s.setSoTimeout(5000)
  val out    = new OutputStreamWriter(s.getOutputStream)
  val reader = new BufferedReader(new InputStreamReader(s.getInputStream))

  protected def createSocket = {
    if (!secure) {
      new Socket("localhost", port)
    } else {
      val ctx = SSLContext.getInstance("TLS")
      ctx.init(null, Array(new MockTrustManager()), null)
      ctx.getSocketFactory.createSocket("localhost", port)
    }
  }

  def sendRaw(data: Array[Byte], headers: Map[String, String]): BasicResponse = {
    val outputStream = s.getOutputStream
    outputStream.write("POST / HTTP/1.1\r\n".getBytes("UTF-8"))
    outputStream.write("Host: localhost\r\n".getBytes("UTF-8"))
    headers.foreach { header => outputStream.write(s"${header._1}: ${header._2}\r\n".getBytes("UTF-8")) }
    outputStream.flush()

    outputStream.write("\r\n".getBytes("UTF-8"))
    outputStream.write(data)
    readResponse("0 continue")
  }

  /**
   * Send a request
   *
   * @param request The request to send
   * @param waitForResponses Whether we should wait for responses
   * @param trickleFeed Whether bodies should be trickle fed.  Trickle feeding will simulate a more realistic network
   *                    environment.
   * @return The responses (may be more than one if Expect: 100-continue header is present) if requested to wait for
   *         them
   */
  def sendRequest(
      request: BasicRequest,
      requestDesc: String,
      waitForResponses: Boolean = true,
      trickleFeed: Option[Long] = None
  ): Seq[BasicResponse] = {
    out.write(s"${request.method} ${request.uri} ${request.version}\r\n")
    out.write("Host: localhost\r\n")
    request.headers.foreach { header => out.write(s"${header._1}: ${header._2}\r\n") }
    out.write("\r\n")

    def writeBody() = {
      if (request.body.length > 0) {
        trickleFeed match {
          case Some(timeout) =>
            request.body.grouped(8192).foreach { chunk =>
              out.write(chunk)
              out.flush()
              Thread.sleep(timeout)
            }
          case None =>
            out.write(request.body)
        }
      }
      out.flush()
    }

    if (waitForResponses) {
      request.headers
        .get("Expect")
        .filter(_ == "100-continue")
        .map { _ =>
          out.flush()
          val response = readResponse(requestDesc + " continue")
          if (response.status == 100) {
            writeBody()
            Seq(response, readResponse(requestDesc))
          } else {
            Seq(response)
          }
        }
        .getOrElse {
          writeBody()
          Seq(readResponse(requestDesc))
        }
    } else {
      writeBody()
      Nil
    }
  }

  /**
   * Read a response
   *
   * @param responseDesc Description of the response, for error reporting
   * @return The response
   */
  def readResponse(responseDesc: String) = {
    try {
      val statusLine = reader.readLine()
      if (statusLine == null) {
        // The line can be null when the CI system doesn't respond in time.
        // so retry repeatedly by throwing IOException.
        throw new IOException(s"No response $responseDesc: EOF reached")
      }

      val (version, status, reasonPhrase) = statusLine.split(" ", 3) match {
        case Array(v, s, r) => (v, s.toInt, r)
        case Array(v, s)    => (v, s.toInt, "")
        case _              => throw new RuntimeException("Invalid status line for response " + responseDesc + ": " + statusLine)
      }
      // Read headers
      def readHeaders: List[(String, String)] = {
        val header = reader.readLine()
        if (header.length == 0) {
          Nil
        } else {
          val parsed = header.split(":", 2) match {
            case Array(name, value) => (name.trim(), value.trim())
            case Array(name)        => (name, "")
          }
          parsed :: readHeaders
        }
      }
      val headers = TreeMap(readHeaders: _*)(CaseInsensitiveOrdered)

      def readCompletely(length: Int): String = {
        if (length == 0) {
          ""
        } else {
          val buf                               = new Array[Char](length)
          def readFromOffset(offset: Int): Unit = {
            val read = reader.read(buf, offset, length - offset)
            if (read + offset < length) readFromOffset(read + offset) else ()
          }
          readFromOffset(0)
          new String(buf)
        }
      }

      // Read body
      val body = headers
        .get(TRANSFER_ENCODING)
        .filter(_ == CHUNKED)
        .map { _ =>
          def readChunks: List[String] = {
            val chunkLength = Integer.parseInt(reader.readLine())
            if (chunkLength == 0) {
              Nil
            } else {
              val chunk = readCompletely(chunkLength)
              // Ignore newline after chunk
              reader.readLine()
              chunk :: readChunks
            }
          }
          (readChunks.toSeq, readHeaders.toMap)
        }
        .toRight {
          headers
            .get(CONTENT_LENGTH)
            .map { length => readCompletely(length.toInt) }
            .getOrElse {
              val httpConfig        = HttpConfiguration()
              val serverResultUtils = new ServerResultUtils(
                new DefaultSessionCookieBaker(
                  httpConfig.session,
                  httpConfig.secret,
                  new CookieSignerProvider(httpConfig.secret).get
                ),
                new DefaultFlashCookieBaker(
                  httpConfig.flash,
                  httpConfig.secret,
                  new CookieSignerProvider(httpConfig.secret).get
                ),
                new DefaultCookieHeaderEncoding(httpConfig.cookies)
              )
              if (serverResultUtils.mayHaveEntity(status)) {
                consumeRemaining(reader)
              } else {
                ""
              }
            }
        }

      BasicResponse(version, status, reasonPhrase, headers, body)
    } catch {
      case io: IOException =>
        throw io
      case e: Exception =>
        throw new RuntimeException(
          s"Exception while reading response $responseDesc ${e.getClass.getName}: ${e.getMessage}",
          e
        )
    }
  }

  private def consumeRemaining(reader: BufferedReader): String = {
    val writer = new StringWriter()
    try {
      CharStreams.copy(reader, writer)
    } catch {
      case timeout: SocketTimeoutException => throw timeout
    }
    writer.toString
  }

  def close() = {
    s.close()
  }
}

/**
 * A basic response
 *
 * @param version The HTTP version
 * @param status The HTTP status code
 * @param reasonPhrase The HTTP reason phrase
 * @param headers The HTTP response headers
 * @param body The body, left is a plain body, right is for chunked bodies, which is a sequence of chunks and a map of
 *             trailers
 */
case class BasicResponse(
    version: String,
    status: Int,
    reasonPhrase: String,
    headers: Map[String, String],
    body: Either[String, (Seq[String], Map[String, String])]
)

/**
 * A basic request
 *
 * @param method The HTTP request method
 * @param uri The URI
 * @param version The HTTP version
 * @param headers The HTTP request headers
 * @param body The body
 */
case class BasicRequest(method: String, uri: String, version: String, headers: Map[String, String], body: String)

/**
 * A TrustManager that trusts everything
 */
class MockTrustManager() extends X509TrustManager {
  val nullArray = Array[X509Certificate]()

  def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

  def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

  def getAcceptedIssuers = nullArray
}
