package play.api.mvc

import play.api.http.{Writeable, MediaRange}
import play.api.mvc.Results._
import play.api.http.HeaderNames._

trait Rendering {

  /**
   * Tries to render the most acceptable result according to the request’s Accept header value.
   * {{{
   *   render {
   *     case Accepts.Html() => Ok(views.html.show(value))
   *     case Accepts.Json() => Ok(Json.toJson(value))
   *   }
   * }}}
   * 
   * @param f A partial function returning a `Result` for a given request media range
   * @return A result provided by `f`, if it is defined for the current request media ranges, otherwise NotAcceptable
   */
  def render(f: PartialFunction[MediaRange, Result])(implicit request: RequestHeader): Result = {
    def _render(ms: Seq[MediaRange]): Result = ms match {
      case Nil => NotAcceptable
      case Seq(m, ms @ _*) =>
        if (f.isDefinedAt(m)) f(m)
        else _render(ms)
    }

    // “If no Accept header field is present, then it is assumed that the client accepts all media types.”
    if (request.acceptedTypes.isEmpty) _render(Seq(MediaRange("*", "*", None)))
    else _render(request.acceptedTypes)
  }

}
