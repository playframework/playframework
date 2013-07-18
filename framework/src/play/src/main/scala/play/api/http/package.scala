package play.api

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTimeZone

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
  val dateFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID("GMT"))
}