package play.api.mvc

trait RequestExtractors extends AcceptExtractors {

  /**
   * Convenient extractor allowing to apply two extractors.
   * Example of use:
   * {{{
   * request match {
   *   case Accepts.Json() & Accepts.Html() => "This request accepts both JSON and HTML"
   * }
   * }}}
   */
  object & {
    def unapply(request: RequestHeader): Option[(RequestHeader, RequestHeader)] = Some((request, request))
  }

}

/**
 * Define a set of extractors allowing to pattern match on the Accept HTTP header of a request
 */
trait AcceptExtractors {

  /**
   * Common extractors to check if a request accepts JSON, Html, etc.
   * Example of use:
   * {{{
   * request match {
   *   case Accepts.Json() => Ok(toJson(value))
   *   case _ => Ok(views.html.show(value))
   * }
   * }}}
   */
  object Accepts {
    import play.api.http.MimeTypes
    val Json = Accepting(MimeTypes.JSON)
    val Html = Accepting(MimeTypes.HTML)
    val Xml = Accepting(MimeTypes.XML)
    val JavaScript = Accepting(MimeTypes.JAVASCRIPT)
  }

}

/**
 * Convenient class to generate extractors checking if a given mime type matches the Accept header of a request.
 * Example of use:
 * {{{
 * val AcceptsMp3 = Accepting("audio/mp3")
 * }}}
 * Then:
 * {{{
 * request match {
 *   case AcceptsMp3() => ...
 * }
 * }}}
 */
case class Accepting(val mimeType: String) {
  def unapply(request: RequestHeader): Boolean = request.accepts(mimeType)
  def unapply(mediaRange: play.api.http.MediaRange): Boolean = mediaRange.accepts(mimeType)
}
