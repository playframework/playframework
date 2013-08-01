package play.utils

import java.util.BitSet
import java.io.ByteArrayOutputStream

/**
 * Provides support for correctly encoding pieces of URIs.
 *
 * @see http://www.ietf.org/rfc/rfc3986.txt
 */
object UriEncoding {

  /**
   * Encode a string so that it can be used safely in the "path segment"
   * part of a URI. A path segment is defined in RFC 3986. In a URI such
   * as `http://www.example.com/abc/def?a=1&b=2` both `abc` and `def`
   * are path segments.
   *
   * Path segment encoding differs from encoding for other parts of a URI.
   * For example, the "&" character is permitted in a path segment, but
   * has special meaning in query parameters. On the other hand, the "/"
   * character cannot appear in a path segment, as it is the path delimiter,
   * so it must be encoded as "%2F". These are just two examples of the
   * differences between path segment and query string encoding; there are
   * other differences too.
   *
   * When encoding path segments the `encodePathSegment` method should always
   * be used in preference to the [[java.net.URLEncoder.encode(String,String)]]
   * method. `URLEncoder.encode`, despite its name, actually provides encoding
   * in the `application/x-www-form-urlencoded` MIME format which is the encoding
   * used for form data in HTTP GET and POST requests. This encoding is suitable
   * for inclusion in the query part of a URI. But `URLEncoder.encode` should not
   * be used for path segment encoding. (Also note that `URLEncoder.encode` is
   * not quite spec compliant. For example, it percent-encodes the `~` character when
   * really it should leave it as unencoded.)
   *
   * @param s The string to encode.
   * @param encoding The name of the encoding that the string `s` is encoded with.
   *     The string `s` will be converted to octets (bytes) using this character encoding.
   * @return An encoded string in the US-ASCII character set.
   */
  def encodePathSegment(s: String, encoding: String): String = {
    val in = s.getBytes(encoding)
    val out = new ByteArrayOutputStream()
    for (b <- in) {
      val allowed = segmentChars.get(b & 0xFF)
      if (allowed) {
        out.write(b)
      } else {
        out.write('%')
        out.write(upperHex((b >> 4) & 0xF))
        out.write(upperHex(b & 0xF))
      }
    }
    new String(out.toByteArray(), "US-ASCII")
  }

  // RFC 3986, 3.3. Path
  // segment       = *pchar
  // segment-nz    = 1*pchar
  // segment-nz-nc = 1*( unreserved / pct-encoded / sub-delims / "@" )
  //               ; non-zero-length segment without any colon ":"
  /** The set of ASCII character codes that are allowed in a URI path segment. */
  private val segmentChars: BitSet = membershipTable(pchar)

  /** The characters allowed in a path segment; defined in RFC 3986 */
  private def pchar: Seq[Char] = {
    // RFC 3986, 2.3. Unreserved Characters
    // unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
    val alphaDigit = for ((min, max) <- Seq(('a', 'z'), ('A', 'Z'), ('0', '9')); c <- min to max) yield c
    val unreserved = alphaDigit ++ Seq('-', '.', '_', '~')

    // RFC 3986, 2.2. Reserved Characters
    // sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
    //             / "*" / "+" / "," / ";" / "="
    val subDelims = Seq('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=')

    // RFC 3986, 3.3. Path
    // pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
    unreserved ++ subDelims ++ Seq(':', '@')
  }

  /** Create a BitSet to act as a membership lookup table for the given characters. */
  private def membershipTable(chars: Seq[Char]): BitSet = {
    val bits = new BitSet(256)
    for (c <- chars) { bits.set(c.toInt) }
    bits
  }

  /**
   * Given a number from 0 to 16, return the ASCII character code corresponding
   * to its uppercase hexadecimal representation.
   */
  private def upperHex(x: Int): Int = {
    // Assume 0 <= x < 16
    if (x < 10) (x + '0') else (x - 10 + 'A')
  }

}