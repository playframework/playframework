package play.api.http

/**
 * Represent a media range as defined by the RFC 2616.
 * @param mediaType Media type (e.g. "audio", "text", etc.)
 * @param mediaSubType Media sub type (e.g. "html", "plain", etc.)
 * @param parameters Media parameters (e.g. "level=1")
 */
case class MediaRange(mediaType: String, mediaSubType: String, parameters: Option[String]) {

  /**
   * @return true if `mimeType` matches this media type, otherwise false
   */
  def accepts(mimeType: String): Boolean =
    mediaType + "/" + mediaSubType == mimeType ||
    (mediaSubType == "*" && mediaType == mimeType.takeWhile(_ != '/')) ||
    (mediaType == "*" && mediaSubType == "*")

}

object MediaRange {

  /**
   * Convenient factory method to create a MediaRange from its MIME type followed by the type parameters.
   * {{{
   *   MediaRange("text/html")
   *   MediaRange("text/html;level=1")
   * }}}
   */
  def apply(mediaRange: String) = {
    val parts = mediaRange.split('/')
    val mediaType = parts(0)
    val rhs = parts(1)
    val i = rhs.indexOf(';')
    val (mediaSubType, parameters) =
      if (i > 0) (rhs.take(i), Some(rhs.drop(i + 1)))
      else (rhs, None)
    new MediaRange(mediaType, mediaSubType, parameters)
  }

  implicit val ordering = new Ordering[play.api.http.MediaRange] {
    // “The asterisk "*" character is used to group media types into ranges, with "*/*" indicating all media types
    // and "type/*" indicating all subtypes of that type.”
    // “If more than one media range applies to a given type, the most specific reference has precedence.”
    def compare(a: play.api.http.MediaRange, b: play.api.http.MediaRange) = {
      if (a.mediaType == b.mediaType) {
        if (a.mediaSubType == b.mediaSubType) Ordering[Option[String]].compare(a.parameters, b.parameters)
        else if (a.mediaSubType == "*") -1
        else if (b.mediaSubType == "*") 1
        else 0
      } else if (a.mediaType == "*") -1
      else if (b.mediaType == "*") 1
      else 0
    }
  }

}