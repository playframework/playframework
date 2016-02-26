/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import java.nio.charset.StandardCharsets

import org.asynchttpclient.util.HttpUtils
import org.slf4j.LoggerFactory
import play.api.libs.ws._

import scala.concurrent.Future

/**
 * Logs WSRequest and pulls information into Curl format to an SLF4J logger.
 *
 * @param logger an SLF4J logger
 */
class AhcCurlRequestLogger(logger: org.slf4j.Logger) extends WSRequestFilter with CurlFormat {
  def apply(executor: WSRequestExecutor): WSRequestExecutor = {
    new WSRequestExecutor {
      override def execute(request: WSRequest): Future[WSResponse] = {
        val ningRequest = request.asInstanceOf[AhcWSRequest]
        logger.info(toCurl(ningRequest))
        executor.execute(request)
      }
    }
  }
}

object AhcCurlRequestLogger {

  private val logger = LoggerFactory.getLogger("play.api.libs.ws.ahc.AhcCurlRequestLogger")

  private val instance = new AhcCurlRequestLogger(logger)

  def apply() = instance

  def apply(logger: org.slf4j.Logger): AhcCurlRequestLogger = {
    new AhcCurlRequestLogger(logger)
  }
}

trait CurlFormat {
  def toCurl(request: AhcWSRequest): String = {
    val b = new StringBuilder("curl \\\n")

    // verbose, since it's a fair bet this is for debugging
    b.append("  --verbose")
    b.append(" \\\n")

    // method
    b.append(s"  --request ${request.method}")
    b.append(" \\\n")

    // headers
    request.headers.foreach {
      case (k, values) =>
        values.foreach { v =>
          b.append(s"  --header '${quote(k)}: ${quote(v)}'")
          b.append(" \\\n")
        }
    }

    // body (note that this has only been checked for text, not binary)
    request.getBody.map { body =>
      val charset = findCharset(request)
      val bodyString = body.decodeString(charset)
      // XXX Need to escape any quotes within the body of the string.
      b.append(s"  --data '${quote(bodyString)}'")
      b.append(" \\\n")
    }

    // pull out some underlying values from the request.  This creates a new Request
    // but should be harmless.
    val asyncHttpRequest = request.buildRequest()
    val proxyServer = asyncHttpRequest.getProxyServer
    if (proxyServer != null) {
      b.append(s"  --proxy ${proxyServer.getHost}:${proxyServer.getPort}")
      b.append(" \\\n")
    }

    // url
    b.append(s"  '${quote(asyncHttpRequest.getUrl)}'")

    val curlOptions = b.toString()
    curlOptions
  }

  protected def findCharset(request: AhcWSRequest): String = {
    request.contentType.map { ct =>
      Option(HttpUtils.parseCharset(ct)).getOrElse {
        StandardCharsets.UTF_8
      }.name()
    }.getOrElse(HttpUtils.parseCharset("UTF-8").name())
  }

  def quote(unsafe: String): String = unsafe.replace("'", "'\\''")
}
