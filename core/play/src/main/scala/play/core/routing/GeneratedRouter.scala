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
import play.api.routing.HandlerDef
import play.api.routing.Router

import scala.collection.JavaConverters._

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
    if (bindResult == Some(Right(None)) || bindResult == Some(Right(Optional.empty))
        || bindResult == Some(Right(OptionalInt.empty)) || bindResult == Some(Right(OptionalLong.empty))
        || bindResult == Some(Right(OptionalDouble.empty))
        || bindResult == Some(Right(Nil)) || bindResult == Some(Right(Nil.asJava))
        || bindResult == Some(Right(Some(Nil))) || bindResult == Some(Right(Optional.of(Nil.asJava)))) {
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
a1 <- pa1.value.right
 a2 <- pa2.value.right}
      yield (a1, a2))
      .fold(badRequest, { case (a1, a2) => generator(a1, a2) })
  }

  def call[A1, A2, A3](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3])(generator: Function3[A1, A2, A3, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right}
      yield (a1, a2, a3))
      .fold(badRequest, { case (a1, a2, a3) => generator(a1, a2, a3) })
  }

  def call[A1, A2, A3, A4](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4])(generator: Function4[A1, A2, A3, A4, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right}
      yield (a1, a2, a3, a4))
      .fold(badRequest, { case (a1, a2, a3, a4) => generator(a1, a2, a3, a4) })
  }

  def call[A1, A2, A3, A4, A5](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5])(generator: Function5[A1, A2, A3, A4, A5, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right}
      yield (a1, a2, a3, a4, a5))
      .fold(badRequest, { case (a1, a2, a3, a4, a5) => generator(a1, a2, a3, a4, a5) })
  }

  def call[A1, A2, A3, A4, A5, A6](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6])(generator: Function6[A1, A2, A3, A4, A5, A6, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right}
      yield (a1, a2, a3, a4, a5, a6))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6) => generator(a1, a2, a3, a4, a5, a6) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7])(generator: Function7[A1, A2, A3, A4, A5, A6, A7, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7) => generator(a1, a2, a3, a4, a5, a6, a7) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8])(generator: Function8[A1, A2, A3, A4, A5, A6, A7, A8, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8) => generator(a1, a2, a3, a4, a5, a6, a7, a8) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9])(generator: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10])(generator: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11])(generator: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12])(generator: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13])(generator: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14])(generator: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15])(generator: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right
 a15 <- pa15.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16])(generator: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right
 a15 <- pa15.value.right
 a16 <- pa16.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17])(generator: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right
 a15 <- pa15.value.right
 a16 <- pa16.value.right
 a17 <- pa17.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18])(generator: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right
 a15 <- pa15.value.right
 a16 <- pa16.value.right
 a17 <- pa17.value.right
 a18 <- pa18.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18], pa19: Param[A19])(generator: Function19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right
 a15 <- pa15.value.right
 a16 <- pa16.value.right
 a17 <- pa17.value.right
 a18 <- pa18.value.right
 a19 <- pa19.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18], pa19: Param[A19], pa20: Param[A20])(generator: Function20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right
 a15 <- pa15.value.right
 a16 <- pa16.value.right
 a17 <- pa17.value.right
 a18 <- pa18.value.right
 a19 <- pa19.value.right
 a20 <- pa20.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) })
  }

  def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18], pa19: Param[A19], pa20: Param[A20], pa21: Param[A21])(generator: Function21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, Handler]): Handler = {
    (for {
a1 <- pa1.value.right
 a2 <- pa2.value.right
 a3 <- pa3.value.right
 a4 <- pa4.value.right
 a5 <- pa5.value.right
 a6 <- pa6.value.right
 a7 <- pa7.value.right
 a8 <- pa8.value.right
 a9 <- pa9.value.right
 a10 <- pa10.value.right
 a11 <- pa11.value.right
 a12 <- pa12.value.right
 a13 <- pa13.value.right
 a14 <- pa14.value.right
 a15 <- pa15.value.right
 a16 <- pa16.value.right
 a17 <- pa17.value.right
 a18 <- pa18.value.right
 a19 <- pa19.value.right
 a20 <- pa20.value.right
 a21 <- pa21.value.right}
      yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21))
      .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) })
  }
  // format: on

  def call[T](params: List[Param[_]])(generator: (Seq[_]) => Handler): Handler =
    (params
      .foldLeft[Either[String, Seq[_]]](Right(Seq[T]())) { (seq, param) =>
        seq.right.flatMap(s => param.value.right.map(s :+ _))
      })
      .fold(badRequest, generator)
  def fakeValue[A]: A = throw new UnsupportedOperationException("Can't get a fake value")

  /**
   * Create a HandlerInvoker for a route by simulating a call to the
   * controller method. This method is called by the code-generated routes
   * files.
   */
  def createInvoker[T](fakeCall: => T, handlerDef: HandlerDef)(
      implicit hif: HandlerInvokerFactory[T]
  ): HandlerInvoker[T] = {
    // Get the implicit invoker factory and ask it for an invoker.
    val underlyingInvoker: HandlerInvoker[T] = hif.createInvoker(fakeCall, handlerDef)

    // Precalculate the function that adds routing information to the request
    val modifyRequestFunc: RequestHeader => RequestHeader = { rh: RequestHeader =>
      rh.addAttr(play.api.routing.Router.Attrs.HandlerDef, handlerDef)
    }

    // Wrap the invoker with another invoker that preprocesses requests as they are made,
    // adding routing information to each request.
    new HandlerInvoker[T] {
      override def call(call: => T): Handler = {
        val nextHandler = underlyingInvoker.call(call)
        Handler.Stage.modifyRequest(modifyRequestFunc, nextHandler)
      }
    }
  }
}
