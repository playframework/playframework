/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.utils

import java.lang.{ StringBuilder => JStringBuilder }
import java.util.function.IntConsumer
import java.util.{ BitSet => JBitSet }

/**
 * Support for rending HTTP header parameters according to RFC5987.
 */
private[play] object HttpHeaderParameterEncoding {

  private def charSeqToBitSet(chars: Seq[Char]): JBitSet = {
    val ints: Seq[Int] = chars.map(_.toInt)
    val max = ints.fold(0)(Math.max(_, _))
    assert(max <= 256) // We should only be dealing with 7 or 8 bit chars
    val bitSet = new JBitSet(max)
    ints.foreach(bitSet.set(_))
    bitSet
  }

  private val AlphaNum: Seq[Char] = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')

  // From https://tools.ietf.org/html/rfc5987#section-3.2.1:
  //
  // attr-char     = ALPHA / DIGIT
  // / "!" / "#" / "$" / "&" / "+" / "-" / "."
  // / "^" / "_" / "`" / "|" / "~"
  // ; token except ( "*" / "'" / "%" )
  private val AttrCharPunctuation: Seq[Char] = Seq('!', '#', '$', '&', '+', '-', '.', '^', '_', '`', '|', '~')

  // From https://tools.ietf.org/html/rfc2616#section-2.2
  //
  //   separators     = "(" | ")" | "<" | ">" | "@"
  //                  | "," | ";" | ":" | "\" | <">
  //                  | "/" | "[" | "]" | "?" | "="
  //                  | "{" | "}" | SP | HT
  //
  // Rich: We exclude <">, "\" since they can be used for quoting/escaping and HT since it is
  // rarely used and seems like it should be escaped.
  private val Separators: Seq[Char] = Seq('(', ')', '<', '>', '@', ',', ';', ':', '/', '[', ']', '?', '=', '{', '}', ' ')

  /**
   * A subset of the 'qdtext' defined in https://tools.ietf.org/html/rfc2616#section-2.2. These are the
   * characters which can be inside a 'quoted-string' parameter value. These should form a
   * superset of the [[AttrChar]] set defined below. We exclude some characters which are technically
   * valid, but might be problematic, e.g. "\" and "%" could be treated as escape characters by some
   * clients. We can be conservative because we can express these characters clearly as an extended
   * parameter.
   */
  private val PartialQuotedText: JBitSet = charSeqToBitSet(
    AlphaNum ++ AttrCharPunctuation ++
      // we include 'separators' plus some chars excluded from 'attr-char'
      Separators ++ Seq('*', '\''))

  /**
   * The 'attr-char' values defined in https://tools.ietf.org/html/rfc5987#section-3.2.1. Should be a
   * subset of [[PartialQuotedText]] defined above.
   */
  private val AttrChar: JBitSet = charSeqToBitSet(AlphaNum ++ AttrCharPunctuation)

  private val PlaceholderChar: Char = '?'

  /**
   * Render a parameter name and value, handling character set issues as
   * recommended in RFC5987.
   *
   * Examples:
   * [[
   * render("filename", "foo.txt") ==> "filename=foo.txt"
   * render("filename", "naïve.txt") ==> "filename=na_ve.txt; filename*=utf8''na%C3%AFve.txt"
   * ]]
   */
  def encode(name: String, value: String): String = {
    val builder = new JStringBuilder
    encodeToBuilder(name, value, builder)
    builder.toString
  }

  /**
   * Render a parameter name and value, handling character set issues as
   * recommended in RFC5987.
   *
   * Examples:
   * [[
   * render("filename", "foo.txt") ==> "filename=foo.txt"
   * render("filename", "naïve.txt") ==> "filename=na_ve.txt; filename*=utf8''na%C3%AFve.txt"
   * ]]
   */
  def encodeToBuilder(name: String, value: String, builder: JStringBuilder): Unit = {

    // This flag gets set if we encounter extended characters when rendering the
    // regular parameter value.
    var hasExtendedChars = false

    // Render ASCII parameter
    // E.g. naïve.txt --> "filename=na_ve.txt"

    builder.append(name)
    builder.append("=\"")

    // Iterate over code points here, because we only want one
    // ASCII character or placeholder per logical character. If
    // we use the value's encoded bytes or chars then we might
    // end up with multiple placeholders per logical character.
    value.codePoints().forEach(new IntConsumer {
      override def accept(codePoint: Int): Unit = {
        // We could support a wider range of characters here by using
        // the 'token' or 'quoted printable' encoding, however it's
        // simpler to use the subset of characters that is also valid
        // for extended attributes.
        if (codePoint >= 0 && codePoint <= 255 && PartialQuotedText.get(codePoint)) {
          builder.append(codePoint.toChar)
        } else {
          // Set flag because we need to render an extended parameter.
          hasExtendedChars = true
          // Render a placeholder instead of the unsupported character.
          builder.append(PlaceholderChar)
        }
      }
    })

    builder.append('"')

    // Optionally render extended, UTF-8 encoded parameter
    // E.g. naïve.txt --> "; filename*=utf8''na%C3%AFve.txt"
    //
    // Renders both regular and extended parameters, as suggested by:
    // - https://tools.ietf.org/html/rfc5987#section-4.2
    // - https://tools.ietf.org/html/rfc6266#section-4.3 (for Content-Disposition filename parameter)

    if (hasExtendedChars) {

      def hexDigit(x: Int): Char = (if (x < 10) (x + '0') else (x - 10 + 'a')).toChar

      // From https://tools.ietf.org/html/rfc5987#section-3.2.1:
      //
      // Producers MUST use either the "UTF-8" ([RFC3629]) or the "ISO-8859-1"
      // ([ISO-8859-1]) character set.  Extension character sets (mime-

      val CharacterSetName = "utf-8"

      builder.append("; ")
      builder.append(name)

      builder.append("*=")
      builder.append(CharacterSetName)
      builder.append("''")

      // From https://tools.ietf.org/html/rfc5987#section-3.2.1:
      //
      // Inside the value part, characters not contained in attr-char are
      // encoded into an octet sequence using the specified character set.
      // That octet sequence is then percent-encoded as specified in Section
      // 2.1 of [RFC3986].

      val bytes = value.getBytes(CharacterSetName)
      for (b <- bytes) {
        if (AttrChar.get(b & 0xFF)) {
          builder.append(b.toChar)
        } else {
          builder.append('%')
          builder.append(hexDigit((b >> 4) & 0xF))
          builder.append(hexDigit(b & 0xF))
        }
      }
    }
  }

}
