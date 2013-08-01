package play.api.http

import play.api.Play
import scala.util.parsing.input.CharSequenceReader
import scala.util.parsing.combinator.Parsers
import scala.collection.BitSet
import play.api.http.MediaRange.MediaRangeParser

/**
 * A media type as defined by RFC 2616 3.7.
 *
 * @param mediaType The media type
 * @param mediaSubType The media sub type
 * @param parameters The parameters
 */
case class MediaType(mediaType: String, mediaSubType: String, parameters: Seq[(String, Option[String])]) {
  override def toString = {
    mediaType + "/" + mediaSubType + parameters.map { param =>
      "; " + param._1 + param._2.map { value =>
        if (MediaRangeParser.token(new CharSequenceReader(value)).next.atEnd) {
          "=" + value
        } else {
          "=\"" + value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\""
        }
      }.getOrElse("")
    }.mkString("")
  }
}

/**
 * A media range as defined by RFC 2616 14.1
 *
 * @param mediaType The media type
 * @param mediaSubType The media sub type
 * @param parameters The parameters
 * @param qValue The Q value
 * @param acceptExtensions The accept extensions
 */
class MediaRange(mediaType: String,
    mediaSubType: String,
    parameters: Seq[(String, Option[String])],
    val qValue: Option[BigDecimal],
    val acceptExtensions: Seq[(String, Option[String])]) extends MediaType(mediaType, mediaSubType, parameters) {

  /**
   * @return true if `mimeType` matches this media type, otherwise false
   */
  def accepts(mimeType: String): Boolean =
    (mediaType + "/" + mediaSubType).equalsIgnoreCase(mimeType) ||
      (mediaSubType == "*" && mediaType.equalsIgnoreCase(mimeType.takeWhile(_ != '/'))) ||
      (mediaType == "*" && mediaSubType == "*")

  /**
   * Since floats are notoriously inexact, when comparing qValues, we want to use a function that uses exact values.
   *
   * This function converts the value to an exact integer value, such that comparing this integer from two different
   * qValues will return the right result.
   *
   * Exploits the fact that RFC 2616 defines that q values may not have more than 3 digits after the decimal point.
   */
  lazy val toExactIntWeight = {
    qValue.map { q =>
      // Truncate to 3 decimal places
      q.setScale(3, BigDecimal.RoundingMode.DOWN)
        // Move decimal point 3 places to the right
        .bigDecimal.movePointRight(3)
        // It now should be an exact integer
        .intValueExact()
    }.getOrElse(1000)
  }

  override def toString = {
    new MediaType(mediaType, mediaSubType,
      parameters ++ qValue.map(q => ("q", Some(q.toString()))).toSeq ++ acceptExtensions).toString
  }
}

object MediaType {

  /**
   * Function and extractor object for parsing media types.
   */
  object parse {
    import MediaRange.MediaRangeParser

    def unapply(mediaType: String): Option[MediaType] = apply(mediaType)

    def apply(mediaType: String): Option[MediaType] = {
      MediaRangeParser.mediaType(new CharSequenceReader(mediaType)) match {
        case MediaRangeParser.Success(mt: MediaType, next) => {
          if (!next.atEnd) {
            Play.logger.debug("Unable to parse part of media type '" + next.source + "'")
          }
          Some(mt)
        }
        case MediaRangeParser.NoSuccess(err, next) => {
          Play.logger.debug("Unable to parse media type '" + next.source + "'")
          None
        }
      }
    }
  }
}

object MediaRange {

  /**
   * Convenient factory method to create a MediaRange from its MIME type followed by the type parameters.
   * {{{
   *   MediaRange("text/html")
   *   MediaRange("text/html;level=1")
   * }}}
   */
  @deprecated("Use MediaType.parse function or extractor object instead", "2.2")
  def apply(mediaRange: String): MediaType = {
    MediaType.parse(mediaRange).getOrElse(throw new IllegalArgumentException("Unable to parse media range from String " + mediaRange))
  }

  /**
   * Function and extractor object for parsing media ranges.
   */
  object parse {

    def apply(mediaRanges: String): Seq[MediaRange] = {
      MediaRangeParser(new CharSequenceReader(mediaRanges)) match {
        case MediaRangeParser.Success(mrs: List[MediaRange], next) =>
          if (next.atEnd) {
            Play.logger.debug("Unable to parse part of media range header '" + next.source + "'")
          }
          mrs.sorted
        case MediaRangeParser.NoSuccess(err, _) =>
          Play.logger.debug("Unable to parse media range header '" + mediaRanges + "': " + err)
          Nil
      }
    }
  }

  /**
   * Ordering for MediaRanges, in order of highest priority to lowest priority.
   *
   * The reason it is highest to lowest instead of lowest to highest is to ensure sorting is stable, so if two media
   * ranges have the same ordering weight, they will not change order.
   *
   * Ordering rules for MediaRanges:
   *
   * First compare by qValue, default to 1.  Higher means higher priority.
   * Then compare the media type.  If they are not the same, then the least specific (ie, if one is *) has a lower
   * priority, otherwise if they have same priority.
   * Then compare the sub media type.  If they are the same, the one with the more parameters has a higher priority.
   * Otherwise the least specific has the lower priority, otherwise they have the same priority.
   */
  implicit val ordering = new Ordering[play.api.http.MediaRange] {
    def compare(a: play.api.http.MediaRange, b: play.api.http.MediaRange) = {
      if (a.toExactIntWeight > b.toExactIntWeight) -1
      else if (a.toExactIntWeight < b.toExactIntWeight) 1
      else if (a.mediaType == b.mediaType) {
        if (a.mediaSubType == b.mediaSubType) b.parameters.size - a.parameters.size
        else if (a.mediaSubType == "*") 1
        else if (b.mediaSubType == "*") -1
        else 0
      } else if (a.mediaType == "*") 1
      else if (b.mediaType == "*") -1
      else 0
    }
  }

  /**
   * Parser for media ranges.
   *
   * Parses based on RFC2616 section 14.1, and 3.7.
   *
   * Unfortunately the specs are quite ambiguous, leaving much to our discretion.
   */
  private[http] object MediaRangeParser extends Parsers {

    val separatorChars = "()<>@,;:\\\"/[]?={} \t"
    val separatorBitSet = BitSet(separatorChars.toCharArray.map(_.toInt): _*)

    type Elem = Char

    val any = acceptIf(_ => true)(_ => "Expected any character")

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
      acceptIf(c => separatorBitSet(c))(_ => "Expected one of " + separatorChars)
    }

    val token = rep1(not(separators | ctl) ~> any) ^^ charSeqToString

    // The spec is really vague about what a quotedPair means. We're going to assume that it's just to quote quotes,
    // which means all we have to do for the result of it is ignore the slash.
    val quotedPair = '\\' ~> char
    val qdtext = not('"') ~> text
    val quotedString = '"' ~> rep(quotedPair | qdtext) <~ '"' ^^ charSeqToString

    /*
     * RFC 2616 section 3.7
     */
    val parameter = token ~ opt('=' ~> (token | quotedString)) ^^ {
      case name ~ value => name -> value
    }
    val parameters = rep(';' ~> rep(' ') ~> parameter)
    val mediaType: Parser[MediaType] = (token <~ '/') ~ token ~ parameters ^^ {
      case mainType ~ subType ~ ps => MediaType(mainType, subType, ps)
    }

    /*
     * RFC 2616 section 14.1
     *
     * Only difference between this and the spec is it treats * as valid.
     */

    // Some clients think that '*' is a valid media range.  Spec says it isn't, but it's used widely enough that we
    // need to support it.
    val mediaRange = (mediaType | ('*' ~> parameters ^^ (MediaType("*", "*", _)))) ^^ { mediaType =>
      val params = mediaType.parameters.takeWhile(_._1 != "q")
      val rest = mediaType.parameters.drop(params.size)
      val (qValueStr, acceptParams) = rest match {
        case q :: ps => (q._2, ps)
        case _ => (None, Nil)
      }
      val qValue = qValueStr.flatMap { q =>
        try {
          Some(BigDecimal(q))
        } catch {
          case _: NumberFormatException =>
            Play.logger.debug("Invalid q value: " + q)
            None
        }
      }
      new MediaRange(mediaType.mediaType, mediaType.mediaSubType, params, qValue, acceptParams)
    }

    val mediaRanges = rep1sep(mediaRange, ',' ~ rep(' '))

    def apply(in: Input): ParseResult[List[MediaRange]] = mediaRanges(in)

    def ignoreErrors(c: Char) = ""

    def charSeqToString(chars: Seq[Char]) = new String(chars.toArray)
  }

}