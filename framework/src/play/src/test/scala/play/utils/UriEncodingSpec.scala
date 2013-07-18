package play.utils

import org.specs2.mutable._

/**
 * Tests for the UriEncoding object.
 */
object UriEncodingSpec extends Specification {
  import UriEncoding._

  "Path segment encoding" should {

/*
RFC 3986 - Uniform Resource Identifier (URI): Generic Syntax

2.2.  Reserved Characters
   ...
      reserved    = gen-delims / sub-delims

      gen-delims  = ":" / "/" / "?" / "#" / "[" / "]" / "@"

      sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
                  / "*" / "+" / "," / ";" / "="
   ...
   URI producing applications should percent-encode data octets that
   correspond to characters in the reserved set unless these characters
   are specifically allowed by the URI scheme to represent data in that
   component.
...
3.3.  Path
   ...
      segment       = *pchar
      segment-nz    = 1*pchar
      segment-nz-nc = 1*( unreserved / pct-encoded / sub-delims / "@" )
                    ; non-zero-length segment without any colon ":"

      pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
*/
    "percent-encode reserved characters, except those allowed in path segment" in {
      // Not allowed (gen-delims, except ":" / "@")
      encodePathSegment("/", "utf-8") must equalTo("%2F")
      encodePathSegment("?", "utf-8") must equalTo("%3F")
      encodePathSegment("#", "utf-8") must equalTo("%23")
      encodePathSegment("[", "utf-8") must equalTo("%5B")
      encodePathSegment("]", "utf-8") must equalTo("%5D")
    }

    "not percent-encode reserved characters that are allowed in path segment" in {
      // Allowed (sub-delims / ":" / "@")
      encodePathSegment("!", "utf-8") must equalTo("!")
      encodePathSegment("$", "utf-8") must equalTo("$")
      encodePathSegment("&", "utf-8") must equalTo("&")
      encodePathSegment("'", "utf-8") must equalTo("'")
      encodePathSegment("(", "utf-8") must equalTo("(")
      encodePathSegment(")", "utf-8") must equalTo(")")
      encodePathSegment("*", "utf-8") must equalTo("*")
      encodePathSegment("+", "utf-8") must equalTo("+")
      encodePathSegment(",", "utf-8") must equalTo(",")
      encodePathSegment(";", "utf-8") must equalTo(";")
      encodePathSegment("=", "utf-8") must equalTo("=")
      encodePathSegment(":", "utf-8") must equalTo(":")
      encodePathSegment("@", "utf-8") must equalTo("@")
    }

/*
2.3.  Unreserved Characters
   ...
      unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
   ... For consistency, percent-encoded octets in the ranges of ALPHA
   (%41-%5A and %61-%7A), DIGIT (%30-%39), hyphen (%2D), period (%2E),
   underscore (%5F), or tilde (%7E) should not be created by URI
   producers and, when found in a URI, should be decoded to their
   corresponding unreserved characters by URI normalizers.
*/
    "not percent-encode unreserved characters" in {
      encodePathSegment("a", "utf-8") must equalTo("a")
      encodePathSegment("z", "utf-8") must equalTo("z")
      encodePathSegment("A", "utf-8") must equalTo("A")
      encodePathSegment("Z", "utf-8") must equalTo("Z")
      encodePathSegment("0", "utf-8") must equalTo("0")
      encodePathSegment("9", "utf-8") must equalTo("9")
      encodePathSegment("-", "utf-8") must equalTo("-")
      encodePathSegment(".", "utf-8") must equalTo(".")
      encodePathSegment("_", "utf-8") must equalTo("_")
      encodePathSegment("~", "utf-8") must equalTo("~")
    }

/*
2.1.  Percent-Encoding

   A percent-encoding mechanism is used to represent a data octet in a
   component when that octet's corresponding character is outside the
   allowed set...
*/
    "percent-encode any characters that aren't specifically allowed in a path segment" in {
      encodePathSegment("\000", "US-ASCII") must equalTo("%00")
      encodePathSegment("\037", "US-ASCII") must equalTo("%1F")
      encodePathSegment(" ", "US-ASCII") must equalTo("%20")
      encodePathSegment("\"", "US-ASCII") must equalTo("%22")
      encodePathSegment("%", "US-ASCII") must equalTo("%25")
      encodePathSegment("<", "US-ASCII") must equalTo("%3C")
      encodePathSegment(">", "US-ASCII") must equalTo("%3E")
      encodePathSegment("\\", "US-ASCII") must equalTo("%5C")
      encodePathSegment("^", "US-ASCII") must equalTo("%5E")
      encodePathSegment("`", "US-ASCII") must equalTo("%60")
      encodePathSegment("{", "US-ASCII") must equalTo("%7B")
      encodePathSegment("|", "US-ASCII") must equalTo("%7C")
      encodePathSegment("}", "US-ASCII") must equalTo("%7D")
      encodePathSegment("\177", "ISO-8859-1") must equalTo("%7F")
      encodePathSegment("\377", "ISO-8859-1") must equalTo("%FF")
    }

    "percent-encode UTF-8 strings by encoding each octet not allowed in a path segment" in {
      encodePathSegment("£0.25", "UTF-8") must equalTo("%C2%A30.25")
      encodePathSegment("€100", "UTF-8") must equalTo("%E2%82%AC100")
      encodePathSegment("«küßî»", "UTF-8") must equalTo("%C2%ABk%C3%BC%C3%9F%C3%AE%C2%BB")
      encodePathSegment("“ЌύБЇ”", "UTF-8") must equalTo("%E2%80%9C%D0%8C%CF%8D%D0%91%D0%87%E2%80%9D")
    }

/*
2.1.  Percent-Encoding

      ... A percent-encoded octet is encoded as a character
   triplet, consisting of the percent character "%" followed by the two
   hexadecimal digits representing that octet's numeric value. ...

      pct-encoded = "%" HEXDIG HEXDIG

   The uppercase hexadecimal digits 'A' through 'F' are equivalent to
   the lowercase digits 'a' through 'f', respectively.  If two URIs
   differ only in the case of hexadecimal digits used in percent-encoded
   octets, they are equivalent.  For consistency, URI producers and
   normalizers should use uppercase hexadecimal digits for all percent-
   encodings.    
*/
    "percent-encode to triplets with upper-case hex" in {
      encodePathSegment("\000", "ISO-8859-1") must equalTo("%00")
      encodePathSegment("\231", "ISO-8859-1") must equalTo("%99")
      encodePathSegment("\252", "ISO-8859-1") must equalTo("%AA")
      encodePathSegment("\377", "ISO-8859-1") must equalTo("%FF")
    }

    // Misc tests

    "handle strings of different lengths" in {
      encodePathSegment("", "UTF-8") must equalTo("")
      encodePathSegment("1", "UTF-8") must equalTo("1")
      encodePathSegment("12", "UTF-8") must equalTo("12")
      encodePathSegment("123", "UTF-8") must equalTo("123")
      encodePathSegment("1234567890", "UTF-8") must equalTo("1234567890")
    }

    "handle strings needing partial percent-encoding" in {
      encodePathSegment("Hello world", "US-ASCII") must equalTo("Hello%20world")
      encodePathSegment("/home/foo", "US-ASCII") must equalTo("%2Fhome%2Ffoo")
    }

    // Path segment encoding differs from query string encoding, which is
    // "application/x-www-form-urlencoded". One difference is the encoding
    // of the "+" and space characters.
    "percent-encode spaces, but not + characters" in {
      encodePathSegment(" ", "US-ASCII") must equalTo("%20") // vs "+" for query strings
      encodePathSegment("+", "US-ASCII") must equalTo("+") // vs "%2B" for query strings
      encodePathSegment(" +", "US-ASCII") must equalTo("%20+") // vs "+%2B" for query strings
      encodePathSegment("1+2=3", "US-ASCII") must equalTo("1+2=3")
      encodePathSegment("1 + 2 = 3", "US-ASCII") must equalTo("1%20+%202%20=%203")
    }
  }
}
