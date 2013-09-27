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
   * @param inputCharset The name of the encoding that the string `s` is encoded with.
   *     The string `s` will be converted to octets (bytes) using this character encoding.
   * @return An encoded string in the US-ASCII character set.
   */
  def encodePathSegment(s: String, inputCharset: String): String = {
    val in = s.getBytes(inputCharset)
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
    out.toString("US-ASCII")
  }

  /**
   * Decode a string according to the rules for the "path segment"
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
   * When decoding path segments the `decodePathSegment` method should always
   * be used in preference to the [[java.net.URLDecoder.decode(String,String)]]
   * method. `URLDecoder.decode`, despite its name, actually decodes
   * the `application/x-www-form-urlencoded` MIME format which is the encoding
   * used for form data in HTTP GET and POST requests. This format is suitable
   * for inclusion in the query part of a URI. But `URLDecoder.decoder` should not
   * be used for path segment encoding or decoding.
   *
   * @param s The string to decode. Must use the US-ASCII character set.
   * @param outputCharset The name of the encoding that the output should be encoded with.
   *     The output string will be converted from octets (bytes) using this character encoding.
   * @throws InvalidEncodingException If the input is not a valid encoded path segment.
   * @return A decoded string in the `outputCharset` character set.
   */
  def decodePathSegment(s: String, outputCharset: String): String = {
    val in = s.getBytes("US-ASCII")
    val out = new ByteArrayOutputStream()
    var inPos = 0
    def next(): Int = {
      val b = in(inPos) & 0xFF
      inPos += 1
      b
    }
    while (inPos < in.length) {
      val b = next()
      if (b == '%') {
        // Read high digit
        if (inPos >= in.length) throw new InvalidUriEncodingException(s"Cannot decode $s: % at end of string")
        val high = fromHex(next())
        if (high == -1) throw new InvalidUriEncodingException(s"Cannot decode $s: expected hex digit at position $inPos.")
        // Read low digit
        if (inPos >= in.length) throw new InvalidUriEncodingException(s"Cannot decode $s: incomplete percent encoding at end of string")
        val low = fromHex(next())
        if (low == -1) throw new InvalidUriEncodingException(s"Cannot decode $s: expected hex digit at position $inPos.")
        // Write decoded byte
        out.write((high << 4) + low)
      } else if (segmentChars.get(b)) {
        // This character is allowed
        out.write(b)
      } else {
        throw new InvalidUriEncodingException(s"Cannot decode $s: illegal character at position $inPos.")
      }
    }
    out.toString(outputCharset)
  }

  /**
   * Decode the path path of a URI. Each path segment will be decoded
   * using the same rules as ``decodePathSegment``. No normalization is performed:
   * leading, trailing and duplicated slashes, if present are left as they are and
   * if absent remain absent; dot-segments (".." and ".") are ignored.
   *
   * Encoded slash characters are will appear as slashes in the output, thus "a/b"
   * will be indistinguishable from "a%2Fb".
   *
   * @param s The string to decode. Must use the US-ASCII character set.
   * @param outputCharset The name of the encoding that the output should be encoded with.
   *     The output string will be converted from octets (bytes) using this character encoding.
   * @throws InvalidEncodingException If the input is not a valid encoded path.
   * @return A decoded string in the `outputCharset` character set.
   */
  def decodePath(s: String, outputCharset: String): String = {
    // Note: Could easily expose a method to return the decoded path as a Seq[String].
    // This would allow better handling of paths segments with encoded slashes in them.
    // However, there is no need for this yet, so the method hasn't been added yet.
    splitString(s, '/').map(decodePathSegment(_, outputCharset)).mkString("/")
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

  /**
   * Given the ASCII value of a character, return its value as a hex digit.
   * If the character isn't a valid hex digit, return -1 instead.
   */
  private def fromHex(b: Int): Int = {
    if (b >= '0' && b <= '9') {
      b - '0'
    } else if (b >= 'A' && b <= 'Z') {
      10 + b - 'A'
    } else if (b >= 'a' && b <= 'z') {
      10 + b - 'a'
    } else {
      -1
    }
  }

  /**
   * Split a string on a character. Similar to `String.split` except, for this method,
   * the invariant {{{splitString(s, '/').mkString("/") == s}}} holds.
   *
   * For example:
   * {{{
   * splitString("//a//", '/') == Seq("", "", "a", "", "")
   * String.split("//a//", '/') == Seq("", "", "a")
   * }}}
   */
  private[utils] def splitString(s: String, c: Char): Seq[String] = {
    val result = scala.collection.mutable.ListBuffer.empty[String]
    import scala.annotation.tailrec
    @tailrec
    def splitLoop(start: Int): Unit = if (start < s.length) {
      var end = s.indexOf(c, start)
      if (end == -1) {
        result += s.substring(start)
      } else {
        result += s.substring(start, end)
        splitLoop(end + 1)
      }
    } else if (start == s.length) {
      result += ""
    }
    splitLoop(0)
    result
  }

}

/**
 * An error caused by processing a value that isn't encoded correctly.
 */
class InvalidUriEncodingException(msg: String) extends RuntimeException(msg)