/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import java.net.Socket
import java.io.{ InputStreamReader, BufferedReader, OutputStreamWriter }
import play.api.test.Helpers._
import org.apache.commons.io.IOUtils

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
   * @return The parsed number of responses.  This may be more than the number of requests, if continue headers are sent.
   */
  def makeRequests(port: Int, checkClosed: Boolean = false, trickleFeed: Option[Long] = None)(requests: BasicRequest*): Seq[BasicResponse] = {
    val client = new BasicHttpClient(port)

    try {
      var requestNo = 0
      val responses = requests.flatMap { request =>
        requestNo += 1
        client.sendRequest(request, requestNo.toString, trickleFeed = trickleFeed)
      }

      if (checkClosed) {
        val line = client.reader.readLine()
        if (line != null) {
          throw new RuntimeException("Unexpected data after responses received: " + line)
        }
      }

      responses

    } finally {
      client.close()
    }
  }

  def pipelineRequests(port: Int, requests: BasicRequest*): Seq[BasicResponse] = {
    val client = new BasicHttpClient(port)

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

class BasicHttpClient(port: Int) {
  val s = new Socket("localhost", port)
  s.setSoTimeout(5000)
  val out = new OutputStreamWriter(s.getOutputStream)
  val reader = new BufferedReader(new InputStreamReader(s.getInputStream))

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
  def sendRequest(request: BasicRequest, requestDesc: String, waitForResponses: Boolean = true,
    trickleFeed: Option[Long] = None): Seq[BasicResponse] = {
    out.write(s"${request.method} ${request.uri} ${request.version}\r\n")
    out.write("Host: localhost\r\n")
    request.headers.foreach { header =>
      out.write(s"${header._1}: ${header._2}\r\n")
    }
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
      request.headers.get("Expect").filter(_ == "100-continue").map { _ =>
        out.flush()
        val response = readResponse(requestDesc + " continue")
        if (response.status == 100) {
          writeBody()
          Seq(response, readResponse(requestDesc))
        } else {
          Seq(response)
        }
      } getOrElse {
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
      // Read status line
      val statusLine = reader.readLine()
      if (statusLine == null) {
        throw new RuntimeException(s"No response $responseDesc: EOF reached")
      }
      val (version, status, reasonPhrase) = statusLine.split(" ", 3) match {
        case Array(v, s, r) => (v, s.toInt, r)
        case Array(v, s) => (v, s.toInt, "")
        case _ => throw new RuntimeException("Invalid status line for response " + responseDesc + ": " + statusLine)
      }
      // Read headers
      def readHeaders: List[(String, String)] = {
        val header = reader.readLine()
        if (header.length == 0) {
          Nil
        } else {
          val parsed = header.split(":", 2) match {
            case Array(name, value) => (name.trim(), value.trim())
            case Array(name) => (name, "")
          }
          parsed :: readHeaders
        }
      }
      val headers = readHeaders.toMap

      def readCompletely(length: Int): String = {
        if (length == 0) {
          ""
        } else {
          val buf = new Array[Char](length)
          def readFromOffset(offset: Int): Unit = {
            val read = reader.read(buf, offset, length - offset)
            if (read + offset < length) readFromOffset(read + offset) else ()
          }
          readFromOffset(0)
          new String(buf)
        }
      }

      // Read body
      val body = headers.get(TRANSFER_ENCODING).filter(_ == CHUNKED).map { _ =>
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
      } toRight {
        headers.get(CONTENT_LENGTH).map { length =>
          readCompletely(length.toInt)
        } getOrElse {
          if (status != CONTINUE && status != NOT_MODIFIED && status != NO_CONTENT) {
            IOUtils.toString(reader)
          } else {
            ""
          }
        }
      }

      BasicResponse(version, status, reasonPhrase, headers, body)
    } catch {
      case e: Exception =>
        throw new RuntimeException(
          s"Exception while reading response $responseDesc ${e.getClass.getName}: ${e.getMessage}", e)
    }
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
case class BasicResponse(version: String, status: Int, reasonPhrase: String, headers: Map[String, String],
  body: Either[String, (Seq[String], Map[String, String])])

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

