/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import play.api.Logger
import play.api.mvc.RequestHeader

import scala.collection.BitSet
import scala.util.Try
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

object ContentEncoding {
  // Taken from https://www.iana.org/assignments/http-parameters/http-parameters.xhtml
  val Aes128gcm = "aes128gcm"
  val Gzip = "gzip"
  val Brotli = "br"
  val Compress = "compress"
  val Deflate = "deflate"
  val Exi = "exi"
  val Pack200Gzip = "pack200-gzip"
  val Identity = "identity"
  // not official but common
  val Bzip2 = "bzip2"
  val Xz = "xz"

  val `*` = "*"
}

/**
 * A representation of an encoding preference as specified in the Accept-Encoding header. This contains an encoding
 * name (or *), and an optional q-value.
 */
case class EncodingPreference(name: String = "*", qValue: Option[BigDecimal] = None) {
  /**
   * `true` if this is a wildcard `*` preference.
   */
  val matchesAny: Boolean = name == "*"

  /**
   * The effective q-value. Defaults to 1 if none is specified.
   */
  val q: BigDecimal = qValue getOrElse 1.0

  /**
   * Check if this encoding preference matches the specified encoding name.
   */
  def matches(contentEncoding: String): Boolean = matchesAny || name.equalsIgnoreCase(contentEncoding)
}

object EncodingPreference {
  /**
   * Ordering for encodings, in order of highest priority to lowest priority.
   */
  implicit val ordering: Ordering[EncodingPreference] = ordering(_ compare _)

  /**
   * An ordering for EncodingPreferences with a specific function for comparing names. Useful to allow the server to
   * provide a preference.
   */
  def ordering(compareByName: (String, String) => Int): Ordering[EncodingPreference] = new Ordering[EncodingPreference] {
    def compare(a: EncodingPreference, b: EncodingPreference) = {
      val qCompare = a.q compare b.q
      val compare = if (qCompare != 0) -qCompare else compareByName(a.name, b.name)
      if (compare != 0) compare
      else if (a.matchesAny) 1
      else if (b.matchesAny) -1
      else 0
    }
  }
}

/**
 * A representation of the Accept-Encoding header
 */
trait AcceptEncoding {

  /**
   * The list of Accept-Encoding headers in order of appearance
   */
  def headers: Seq[String]

  /**
   * A list of encoding preferences, sorted from most to least preferred, and normalized to lowercase names.
   */
  lazy val preferences: Seq[EncodingPreference] = headers.flatMap(AcceptEncoding.parseHeader).map { e =>
    e.copy(name = e.name.toLowerCase)
  }.sorted

  /**
   * Returns `true` if we can safely fall back to the identity encoding if no supported encoding is found.
   */
  lazy val identityAllowed: Boolean = preferences.find(_.matches(ContentEncoding.Identity)).forall(_.q > 0)

  /**
   * Returns `true` if and only if the encoding is accepted by this Accept-Encoding header.
   */
  def accepts(encoding: String): Boolean = {
    preferences.exists(_.matches(encoding))
  }

  /**
   * Given a list of encoding names, choose the most preferred by the client. If several encodings are equally
   * preferred, choose the one first in the list.
   *
   * Note that this chooses not to handle the "*" value, since its presence does not tell us if the client supports
   * the specific encoding.
   */
  def preferred(choices: Seq[String]): Option[String] = {
    // filter matches to ones in the choices
    val filteredMatches = preferences.filter(e => e.q > 0 && choices.exists(e.matches))
    // get top preference by finding max q and then getting preferred option among those
    val preference = if (filteredMatches.isEmpty) None else {
      val maxQ = filteredMatches.maxBy(_.q).q
      filteredMatches.filter(maxQ == _.q).sortBy { pref =>
        val idx = choices.indexWhere(pref.matches)
        if (idx == -1) Int.MaxValue else idx
      }.headOption
    }
    // return the name of the encoding if it matches any, otherwise identity if it is accepted by the client
    preference match {
      case Some(pref) if !pref.matchesAny => Some(pref.name)
      case _ if identityAllowed => Some(ContentEncoding.Identity)
      case _ => None
    }
  }
}

object AcceptEncoding {

  private val logger = Logger(getClass)

  /**
   * Convenience method for creating an AcceptEncoding from varargs of header strings.
   */
  def apply(allHeaders: String*): AcceptEncoding = fromHeaders(allHeaders)

  /**
   * Get an [[AcceptEncoding]] for this request.
   */
  def forRequest(request: RequestHeader): AcceptEncoding =
    fromHeaders(request.headers.getAll(HeaderNames.ACCEPT_ENCODING))

  /**
   * Create an AcceptEncoding from a list of headers.
   */
  def fromHeaders(allHeaders: Seq[String]) = new AcceptEncoding {
    def headers: Seq[String] = allHeaders
  }

  /**
   * Parse a single Accept-Encoding header and return a list of preferred encodings.
   */
  def parseHeader(acceptEncoding: String): Seq[EncodingPreference] = {
    AcceptEncodingParser(new CharSequenceReader(acceptEncoding)) match {
      case AcceptEncodingParser.Success(encs: Seq[EncodingPreference], next) =>
        if (!next.atEnd) {
          logger.debug(s"Unable to parse part of Accept-Encoding header '${next.source}'")
        }
        encs
      case AcceptEncodingParser.NoSuccess(err, _) =>
        logger.debug(s"Unable to parse Accept-Encoding header '$acceptEncoding': $err")
        Seq.empty
    }
  }

  /**
   * Parser for content encodings
   */
  private[http] object AcceptEncodingParser extends Parsers {

    private val logger = Logger(this.getClass())

    val separatorChars = "()<>@,;:\\\"/[]?={} \t"
    val separatorBitSet = BitSet(separatorChars.toCharArray.map(_.toInt): _*)
    val qChars = "Qq"
    val qBitSet = BitSet(qChars.toCharArray.map(_.toInt): _*)

    type Elem = Char

    val any = acceptIf(_ => true)(_ => "Expected any character")
    val end = not(any)

    /*
     * RFC 2616 section 2.2
     *
     * These patterns are translated directly using the same naming
     */
    val ctl = acceptIf { c =>
      (c >= 0 && c <= 0x1F) || c == 0x7F
    }(_ => "Expected a control character")
    val char = acceptIf(_ < 0x80)(_ => "Expected an ascii character")
    val text = not(ctl) ~> any
    val separators = {
      acceptIf(c => separatorBitSet(c))(_ => s"Expected one of $separatorChars")
    }
    val qParamName = {
      acceptIf(c => qBitSet(c))(_ => s"Expected one of $qChars")
    }

    val token = rep1(not(separators | ctl) ~> any) ^^ charSeqToString

    def badPart(p: Char => Boolean, msg: => String) = rep1(acceptIf(p)(ignoreErrors)) ^^ {
      case chars =>
        logger.debug(msg + ": " + charSeqToString(chars))
        None
    }
    val badQValue = badPart(c => c != ',' && c != ';', "Bad q value format")
    val badEncoding = badPart(c => c != ',', "Bad encoding")

    def tolerant[T](p: Parser[T], bad: Parser[Option[T]]) = p.map(Some.apply) | bad

    val qParameter = qParamName ~> '=' ~> token <~ rep(' ')

    // Either it's a valid parameter followed immediately by the end, a comma, or it's a bad parameter
    val tolerantQParameter = tolerant(qParameter <~ guard(end | ','), badQValue)

    val qValue = opt(';' ~> rep(' ') ~> tolerantQParameter <~ rep(' ')) ^^ (_.flatten)
    val encoding: Parser[EncodingPreference] = (token <~ rep(' ')) ~ qValue ^^ {
      case encoding ~ qValue =>
        EncodingPreference(encoding, qValue.flatMap { q =>
          Try(BigDecimal(q)).filter(q => q >= 0 && q <= 1).map(Some.apply).getOrElse {
            logger.debug(s"Invalid q value: $q")
            None
          }
        })
    }

    val tolerantEncoding = tolerant(encoding <~ guard(end | ','), badEncoding)

    val encodings = rep1sep(tolerantEncoding, ',' ~ rep(' ')).map(_.flatten)

    def apply(in: Input): ParseResult[Seq[EncodingPreference]] = encodings(in)

    def ignoreErrors(c: Char) = ""

    def charSeqToString(chars: Seq[Char]) = new String(chars.toArray)
  }
}
