/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import java.util.Optional
import java.util.OptionalInt
import java.util.OptionalLong
import java.util.OptionalDouble

import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.routing.Router

import scala.jdk.CollectionConverters._

/**
 * A route
 */
object Route {

  /**
   * Extractor of route from a request.
   */
  trait ParamsExtractor {
    def unapply(request: RequestHeader): Option[RouteParams]
  }

  /**
   * Create a params extractor from the given method and path pattern.
   */
  def apply(method: String, pathPattern: PathPattern) = new ParamsExtractor {
    def unapply(request: RequestHeader): Option[RouteParams] = {
      if (method == request.method) {
        pathPattern(request.path).map { groups =>
          RouteParams(groups, request.queryString)
        }
      } else {
        None
      }
    }
  }
}

/**
 * An included router
 */
class Include(val router: Router) {
  def unapply(request: RequestHeader): Option[Handler] = {
    router.routes.lift(request)
  }
}

/**
 * An included router
 */
object Include {
  def apply(router: Router) = new Include(router)
}

case class Param[T](name: String, value: Either[String, T])

case class RouteParams(path: Map[String, Either[Throwable, String]], queryString: Map[String, Seq[String]]) {
  def fromPath[T](key: String, default: Option[T] = None)(implicit binder: PathBindable[T]): Param[T] = {
    Param(
      key,
      path.get(key).map(v => v.fold(t => Left(t.getMessage), binder.bind(key, _))).getOrElse {
        default.map(d => Right(d)).getOrElse(Left("Missing parameter: " + key))
      }
    )
  }

  def fromQuery[T](key: String, default: Option[T] = None)(implicit binder: QueryStringBindable[T]): Param[T] = {
    val bindResult = binder.bind(key, queryString)
    if (bindResult == Some(Right(None))
        || bindResult == Some(Right(Nil))
        || bindResult == Some(Right(Some(Nil)))) {
      Param(key, default.map(d => Right(d)).getOrElse(bindResult.get))
    } else {
      Param(key, bindResult.getOrElse {
        default.map(d => Right(d)).getOrElse(Left("Missing parameter: " + key))
      })
    }
  }
}

/**
 * A generated router.
 */
abstract class GeneratedRouter extends Router {
  def errorHandler: HttpErrorHandler

  def badRequest(error: String) = ActionBuilder.ignoringBody.async { request =>
    errorHandler.onClientError(request, play.api.http.Status.BAD_REQUEST, error)
  }

  def named(name: String)(generator: => Handler) =
    Handler.Stage.modifyRequest(_.addAttr(play.api.routing.Router.Attrs.ActionName, name), generator)

  def call(generator: => Handler): Handler = {
    generator
  }

  def call[P](pa: Param[P])(generator: (P) => Handler): Handler = {
    pa.value.fold(badRequest, generator)
  }

  //Keep the old versions for avoiding compiler failures while building for Scala 2.10,
  // and for avoiding warnings when building for newer Scala versions
  // format: off
  def call[A1, A2](pa1: Param[A1], pa2: Param[A2])(generator: Function2[A1, A2, Handler]): Handler = {
    (for {
a1 <- pa1.value
 a2 <- pa2.value}
      yield (a1, a2))
      .fold(badRequest, { case (a1, a2) => generator(a1, a2) })
  }

  def call[A1, A2, A3](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3])(generator: Function3[A1, A2, A3, Handler]): Handler = {
    (for {
a1 <- pa1.value
 a2 <- pa2.value
 a3 <- pa3.value}
      yield (a1, a2, a3))
      .fold(badRequest, { case (a1, a2, a3) => generator(a1, a2, a3) })
  }

  def call[A1, A2, A3, A4](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4])(generator: Function4[A1, A2, A3, A4, Handler]): Handler = {
    (for {
a1 <- pa1.value
 a2 <- pa2.value
 a3 <- pa3.value
 a4 <- pa4.value}
      yield (a1, a2, a3, a4))
      .fold(badRequest, { case (a1, a2, a3, a4) => generator(a1, a2, a3, a4) })
  }

  def call[A1, A2, A3, A4, A5](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5])(generator: Function5[A1, A2, A3, A4, A5, Handler]): Handler = {
    (for {
a1 <- pa1.value
 a2 <- pa2.value
 a3 <- pa3.value
 a4 <- pa4.value
 a5 <- pa5.value}
      yield (a1, a2, a3, a4, a5))
      .fold(badRequest, { case (a1, a2, a3, a4, a5) => generator(a1, a2, a3, a4, a5) })
  }

  def call[A1, A2, A3, A4, A5, A6](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6])(generator: Function6[A1, A2, A3, A4, A5, A6, Handler]): Handler = {
    (for {
a1 <- pa1.value
 a2 <- pa2.value
 a3 <- pa3.value
 a4 <- pa4.value
 a5 <- pa5.value
 a6 <- pa6.value}
      yield (a1, a2, a3, a4, a5, a6))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6) => generator(a1, a2, a3, a4, a5, a6) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7])(generator: Function7[A1, A2, A3, A4, A5, A6, A7, Handler]): Handler = {
    (for {
a1 <- pa1.value
 a2 <- pa2.value
 a3 <- pa3.value
 a4 <- pa4.value
 a5 <- pa5.value
 a6 <- pa6.value
 a7 <- pa7.value}
      yield (a1, a2, a3, a4, a5, a6, a7))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7) => generator(a1, a2, a3, a4, a5, a6, a7) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8])(generator: Function8[A1, A2, A3, A4, A5, A6, A7, A8, Handler]): Handler = {
    (for {
a1 <- pa1.value
 a2 <- pa2.value
 a3 <- pa3.value
 a4 <- pa4.value
 a5 <- pa5.value
 a6 <- pa6.value
 a7 <- pa7.value
 a8 <- pa8.value}
      yield (a1, a2, a3, a4, a5, a6, a7, a8))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8) => generator(a1, a2, a3, a4, a5, a6, a7, a8) })
  }
  // format: on

  def call[T](params: List[Param[_]])(generator: (Seq[_]) => Handler): Handler =
    (params
      .foldLeft[Either[String, Seq[_]]](Right(Seq[T]())) { (seq, param) =>
        seq.flatMap(s => param.value.map(s :+ _))
      })
      .fold(badRequest, generator)
}
