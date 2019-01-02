/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.time.format.DateTimeFormatter
import java.time.ZoneId

/**
 * Contains standard HTTP constants.
 * For example:
 * {{{
 * val text = ContentTypes.TEXT
 * val ok = Status.OK
 * val accept = HeaderNames.ACCEPT
 * }}}
 */
package object http {
  /** HTTP date formatter, compliant to RFC 1123 */
  val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withLocale(java.util.Locale.ENGLISH).withZone(ZoneId.of("GMT"))
}
