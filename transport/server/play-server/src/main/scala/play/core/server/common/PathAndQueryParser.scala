/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
    val parsedPath =
      try {
        new URI(unsafePath).getRawPath
      } catch {
        case _: Throwable =>
          // If the URI has an invalid path, this will trigger a 400 (bad request) error.
          // Also it's probably a good idea to throw our own IllegalStateException were we have
          // control over the message that will be passed to the error handler instead of just passing on the
          // original exception which could be to cryptic in some cases(or could leaks too much internal details)
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
