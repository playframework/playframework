/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import scala.concurrent.Future

import play.api.http.HeaderNames._
import play.api.http.MediaRange
import play.api.mvc.Results._
import play.core.Execution.Implicits.trampoline

trait Rendering {
  object render {

    /**
     * Tries to render the most acceptable result according to the request’s Accept header value.
     * {{{
     * def myAction = Action { implicit req =>
     *   val value = ...
     *   render {
     *     case Accepts.Html() => Ok(views.html.show(value))
     *     case Accepts.Json() => Ok(Json.toJson(value))
     *   }
     * }
     * }}}
     *
     * @param f A partial function returning a `Result` for a given request media range
     * @return A result provided by `f`, if it is defined for the current request media ranges, otherwise NotAcceptable
     */
    def apply(f: PartialFunction[MediaRange, Result])(implicit request: RequestHeader): Result = {
      def _render(ms: Seq[MediaRange]): Result = ms match {
        case Nil             => NotAcceptable
        case Seq(m, ms @ _*) =>
          f.applyOrElse(m, (m: MediaRange) => _render(ms))
      }

      // “If no Accept header field is present, then it is assumed that the client accepts all media types.”
      val result =
        if (request.acceptedTypes.isEmpty) _render(Seq(new MediaRange("*", "*", Nil, None, Nil)))
        else _render(request.acceptedTypes)
      result.withHeaders(result.header.varyWith(ACCEPT))
    }

    /**
     * Tries to render the most acceptable result according to the request’s Accept header value.
     *
     * This function can be used if you want to do asynchronous processing in your render function.
     * {{{
     * def myAction = Action.async { implicit req =>
     *   val value = ...
     *   render.async {
     *     case Accepts.Html() => loadData.map(data => Ok(views.html.show(value, data))))
     *     case Accepts.Json() => Future.successful(Ok(Json.toJson(value)))
     *   }
     * }
     * }}}
     *
     * @param f A partial function returning a `Future[Result]` for a given request media range
     * @return A result provided by `f`, if it is defined for the current request media ranges, otherwise NotAcceptable
     */
    def async(f: PartialFunction[MediaRange, Future[Result]])(implicit request: RequestHeader): Future[Result] = {
      def _render(ms: Seq[MediaRange]): Future[Result] = ms match {
        case Nil             => Future.successful(NotAcceptable)
        case Seq(m, ms @ _*) =>
          f.applyOrElse(m, (m: MediaRange) => _render(ms))
      }

      // “If no Accept header field is present, then it is assumed that the client accepts all media types.”
      val result =
        if (request.acceptedTypes.isEmpty) _render(Seq(new MediaRange("*", "*", Nil, None, Nil)))
        else _render(request.acceptedTypes)
      result.map(r => r.withHeaders(r.header.varyWith(ACCEPT)))
    }
  }
}
