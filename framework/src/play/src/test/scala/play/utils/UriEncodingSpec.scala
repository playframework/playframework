/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import org.specs2.mutable._

/**
 * Tests for the UriEncoding object.
 */
object UriEncodingSpec extends Specification {
  import UriEncoding._

  sealed trait EncodingResult
  // Good behaviour
  case object NotEncoded extends EncodingResult
  case class PercentEncoded(encoded: String) extends EncodingResult
  // Bad behaviour
  case class NotEncodedButDecodeDifferent(decodedEncoded: String) extends EncodingResult
  case class PercentEncodedButDecodeDifferent(encoded: String, decodedEncoded: String) extends EncodingResult
  case class PercentEncodedButDecodedInvalid(encoded: String) extends EncodingResult

  def encodingFor(in: String, inCharset: String): EncodingResult = {
    val encoded = encodePathSegment(in, inCharset)
    if (encoded == in) {
      val decodedEncoded = decodePathSegment(encoded, inCharset)
      if (decodedEncoded != in) return NotEncodedButDecodeDifferent(decodedEncoded)
      NotEncoded
    } else {
      val decodedEncoded = decodePathSegment(encoded, inCharset)
      if (decodedEncoded != in) return PercentEncodedButDecodeDifferent(encoded, decodedEncoded)
      try {
        decodePathSegment(in, inCharset)
        return PercentEncodedButDecodedInvalid(encoded) // Decoding should have failed
      } catch {
        case _: InvalidUriEncodingException => () // This is expected behaviour
      }
      PercentEncoded(encoded)
    }
  }

  "Path segment encoding and decoding" should {

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
    "percent-encode reserved characters that aren't allowed in a path segment" in {
      // Not allowed (gen-delims, except ":" / "@")
      encodingFor("/", "utf-8") must_== PercentEncoded("%2F")
      encodingFor("?", "utf-8") must_== PercentEncoded("%3F")
      encodingFor("#", "utf-8") must_== PercentEncoded("%23")
      encodingFor("[", "utf-8") must_== PercentEncoded("%5B")
      encodingFor("]", "utf-8") must_== PercentEncoded("%5D")
    }

    "not percent-encode reserved characters that are allowed in a path segment" in {
      // Allowed (sub-delims / ":" / "@")
      encodingFor("!", "utf-8") must_== NotEncoded
      encodingFor("$", "utf-8") must_== NotEncoded
      encodingFor("&", "utf-8") must_== NotEncoded
      encodingFor("'", "utf-8") must_== NotEncoded
      encodingFor("(", "utf-8") must_== NotEncoded
      encodingFor(")", "utf-8") must_== NotEncoded
      encodingFor("*", "utf-8") must_== NotEncoded
      encodingFor("+", "utf-8") must_== NotEncoded
      encodingFor(",", "utf-8") must_== NotEncoded
      encodingFor(";", "utf-8") must_== NotEncoded
      encodingFor("=", "utf-8") must_== NotEncoded
      encodingFor(":", "utf-8") must_== NotEncoded
      encodingFor("@", "utf-8") must_== NotEncoded
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
      encodingFor("a", "utf-8") must_== NotEncoded
      encodingFor("z", "utf-8") must_== NotEncoded
      encodingFor("A", "utf-8") must_== NotEncoded
      encodingFor("Z", "utf-8") must_== NotEncoded
      encodingFor("0", "utf-8") must_== NotEncoded
      encodingFor("9", "utf-8") must_== NotEncoded
      encodingFor("-", "utf-8") must_== NotEncoded
      encodingFor(".", "utf-8") must_== NotEncoded
      encodingFor("_", "utf-8") must_== NotEncoded
      encodingFor("~", "utf-8") must_== NotEncoded
    }

    /*
2.1.  Percent-Encoding

   A percent-encoding mechanism is used to represent a data octet in a
   component when that octet's corresponding character is outside the
   allowed set...
*/
    "percent-encode any characters that aren't specifically allowed in a path segment" in {
      encodingFor("\000", "US-ASCII") must_== PercentEncoded("%00")
      encodingFor("\037", "US-ASCII") must_== PercentEncoded("%1F")
      encodingFor(" ", "US-ASCII") must_== PercentEncoded("%20")
      encodingFor("\"", "US-ASCII") must_== PercentEncoded("%22")
      encodingFor("%", "US-ASCII") must_== PercentEncoded("%25")
      encodingFor("<", "US-ASCII") must_== PercentEncoded("%3C")
      encodingFor(">", "US-ASCII") must_== PercentEncoded("%3E")
      encodingFor("\\", "US-ASCII") must_== PercentEncoded("%5C")
      encodingFor("^", "US-ASCII") must_== PercentEncoded("%5E")
      encodingFor("`", "US-ASCII") must_== PercentEncoded("%60")
      encodingFor("{", "US-ASCII") must_== PercentEncoded("%7B")
      encodingFor("|", "US-ASCII") must_== PercentEncoded("%7C")
      encodingFor("}", "US-ASCII") must_== PercentEncoded("%7D")
      encodingFor("\177", "ISO-8859-1") must_== PercentEncoded("%7F")
      encodingFor("\377", "ISO-8859-1") must_== PercentEncoded("%FF")
    }

    "percent-encode UTF-8 strings by encoding each octet not allowed in a path segment" in {
      encodingFor("£0.25", "UTF-8") must_== PercentEncoded("%C2%A30.25")
      encodingFor("€100", "UTF-8") must_== PercentEncoded("%E2%82%AC100")
      encodingFor("«küßî»", "UTF-8") must_== PercentEncoded("%C2%ABk%C3%BC%C3%9F%C3%AE%C2%BB")
      encodingFor("“ЌύБЇ”", "UTF-8") must_== PercentEncoded("%E2%80%9C%D0%8C%CF%8D%D0%91%D0%87%E2%80%9D")
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
      encodingFor("\000", "ISO-8859-1") must_== PercentEncoded("%00")
      encodingFor("\231", "ISO-8859-1") must_== PercentEncoded("%99")
      encodingFor("\252", "ISO-8859-1") must_== PercentEncoded("%AA")
      encodingFor("\377", "ISO-8859-1") must_== PercentEncoded("%FF")
    }

    // Misc tests

    "handle strings of different lengths" in {
      encodingFor("", "UTF-8") must_== NotEncoded
      encodingFor("1", "UTF-8") must_== NotEncoded
      encodingFor("12", "UTF-8") must_== NotEncoded
      encodingFor("123", "UTF-8") must_== NotEncoded
      encodingFor("1234567890", "UTF-8") must_== NotEncoded
    }

    "handle strings needing partial percent-encoding" in {
      encodingFor("Hello world", "US-ASCII") must_== PercentEncoded("Hello%20world")
      encodingFor("/home/foo", "US-ASCII") must_== PercentEncoded("%2Fhome%2Ffoo")
    }

    // Path segment encoding differs from query string encoding, which is
    // "application/x-www-form-urlencoded". One difference is the encoding
    // of the "+" and space characters.
    "percent-encode spaces, but not + characters" in {
      encodingFor(" ", "US-ASCII") must_== PercentEncoded("%20") // vs "+" for query strings
      encodingFor("+", "US-ASCII") must_== NotEncoded // vs "%2B" for query strings
      encodingFor(" +", "US-ASCII") must_== PercentEncoded("%20+") // vs "+%2B" for query strings
      encodingFor("1+2=3", "US-ASCII") must_== NotEncoded
      encodingFor("1 + 2 = 3", "US-ASCII") must_== PercentEncoded("1%20+%202%20=%203")
    }

    "decode characters percent-encoded with upper and lowercase hex digits" in {
      decodePathSegment("%aa", "ISO-8859-1") must_== "\252"
      decodePathSegment("%aA", "ISO-8859-1") must_== "\252"
      decodePathSegment("%Aa", "ISO-8859-1") must_== "\252"
      decodePathSegment("%AA", "ISO-8859-1") must_== "\252"
      decodePathSegment("%ff", "ISO-8859-1") must_== "\377"
      decodePathSegment("%fF", "ISO-8859-1") must_== "\377"
      decodePathSegment("%Ff", "ISO-8859-1") must_== "\377"
      decodePathSegment("%FF", "ISO-8859-1") must_== "\377"
    }

    "decode percent-encoded characters that don't really need to be encoded" in {
      decodePathSegment("%21", "utf-8") must_== "!"
      decodePathSegment("%61", "utf-8") must_== "a"
      decodePathSegment("%31%32%33", "UTF-8") must_== "123"
      // Encoded by MIME type "application/x-www-form-urlencoded"
      decodePathSegment("%2b", "US-ASCII") must_== "+"
      decodePathSegment("%7e", "US-ASCII") must_== "~"
    }
  }

  "Path decoding" should {

    "decode basic paths" in {
      decodePath("", "utf-8") must_== ""
      decodePath("/", "utf-8") must_== "/"
      decodePath("/abc", "utf-8") must_== "/abc"
      decodePath("/css/stylesheet.css", "utf-8") must_== "/css/stylesheet.css"
    }

    "decode paths with encoded characters" in {
      decodePath("/hello%20world", "utf-8") must_== "/hello world"
    }

    "decode encoded slashes (although they can't be distinguished from unencoded slashes)" in {
      decodePath("/a%2fb", "utf-8") must_== "/a/b"
      decodePath("/a%2fb/c%2fd", "utf-8") must_== "/a/b/c/d"
    }

    "not decode badly encoded paths" in {
      decodePath("/a|b/", "utf-8") must throwA[InvalidUriEncodingException]
      decodePath("/hello world", "utf-8") must throwA[InvalidUriEncodingException]
    }

    "not perform normalization of dot-segments" in {
      decodePath("a/..", "utf-8") must_== "a/.."
      decodePath("a/.", "utf-8") must_== "a/."
    }

    "not perform normalization of duplicate slashes" in {
      decodePath("//a", "utf-8") must_== "//a"
      decodePath("a//b", "utf-8") must_== "a//b"
      decodePath("a//", "utf-8") must_== "a//"
    }

    "decode complex UTF-8 octets" in {
      decodePath("/path/%C2%ABk%C3%BC%C3%9F%C3%AE%C2%BB", "UTF-8") must_== "/path/«küßî»"
      decodePath("/path/%E2%80%9C%D0%8C%CF%8D%D0%91%D0%87%E2%80%9D", "UTF-8") must_== "/path/“ЌύБЇ”"
    }

  }

  // Internal methods

  "Internal UriEncoding methods" should {

    "know how to split strings" in {
      splitString("", '/') must_== Seq("")
      splitString("/", '/') must_== Seq("", "")
      splitString("a", '/') must_== Seq("a")
      splitString("a/b", '/') must_== Seq("a", "b")
      splitString("a//b", '/') must_== Seq("a", "", "b")
      splitString("/a", '/') must_== Seq("", "a")
      splitString("/a/b", '/') must_== Seq("", "a", "b")
      splitString("/a/b/", '/') must_== Seq("", "a", "b", "")
      splitString("/abc/xyz", '/') must_== Seq("", "abc", "xyz")
    }
  }

}
