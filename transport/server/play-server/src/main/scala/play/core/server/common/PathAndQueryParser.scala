/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.URI

private[server] object PathAndQueryParser {

  /**
   * Parse URI String into path and query string parts.
   * The path part is validated using [[java.net.URI]].
   *
   * See https://tools.ietf.org/html/rfc3986
   *
   * @throws IllegalArgumentException if path is invalid.
   * @return
   */
  @throws[IllegalArgumentException]
  def parse(uri: String): (String, String) = {
    // https://tools.ietf.org/html/rfc3986#section-3.3
    val withoutHost = uri.dropWhile(_ != '/')
    // The path is terminated by the first question mark ("?")
    // or number sign ("#") character, or by the end of the URI.
    val queryEndPos = Some(withoutHost.indexOf('#')).filter(_ != -1).getOrElse(withoutHost.length)
    val pathEndPos  = Some(withoutHost.indexOf('?')).filter(_ != -1).getOrElse(queryEndPos)
    val unsafePath  = withoutHost.substring(0, pathEndPos)
    // https://tools.ietf.org/html/rfc3986#section-3.4
    // The query component is indicated by the first question
    // mark ("?") character and terminated by a number sign ("#") character
    // or by the end of the URI.
    val queryString = withoutHost.substring(pathEndPos, queryEndPos)

    // wrapping into URI to handle absoluteURI and path validation
    val parsedPath = Option(new URI(unsafePath).getRawPath).getOrElse {
      // if the URI has a invalid path, this will trigger a 400 error
      throw new IllegalStateException(s"Cannot parse path from URI: $unsafePath")
    }
    (parsedPath, queryString)
  }

  /**
   * Parse URI String and extract the path part only.
   * The path part is validated using [[java.net.URI]].
   *
   * See https://tools.ietf.org/html/rfc3986
   *
   * @throws IllegalArgumentException if path is invalid.
   * @return
   */
  @throws[IllegalArgumentException]
  def parsePath(uri: String): String = parse(uri)._1
}
