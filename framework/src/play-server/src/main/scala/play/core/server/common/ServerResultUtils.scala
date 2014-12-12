/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import play.api._
import play.api.mvc._
import play.api.http.HeaderNames._
import play.api.libs.iteratee._
import scala.concurrent.{ Future, Promise }

object ServerResultUtils {

  /**
   * Start reading an Enumerator and see if it is only zero or one
   * elements long.
   * - If zero-length, return Left(None).
   * - If one-length, return the element in Left(Some(el))
   * - If more than one element long, return Right(enumerator) where
   *   enumerator is an Enumerator that contains *all* the input. Any
   *   already-read elements will still be included in this Enumerator.
   */
  def readAheadOne[A](enum: Enumerator[A]): Future[Either[Option[A], Enumerator[A]]] = {
    import Execution.Implicits.trampoline
    val result = Promise[Either[Option[A], Enumerator[A]]]()
    val it: Iteratee[A, Unit] = for {
      taken <- Iteratee.takeUpTo(1)
      emptyAfterTaken <- Iteratee.isEmpty
      _ <- {
        if (emptyAfterTaken) {
          assert(taken.length <= 1)
          result.success(Left(taken.headOption))
          Done[A, Unit](())
        } else {
          val (remainingIt, remainingEnum) = Concurrent.joined[A]
          result.success(Right(Enumerator.enumerate(taken) >>> remainingEnum))
          remainingIt
        }
      }
    } yield ()
    enum(it)
    result.future
  }

  def cleanFlashCookie(requestHeader: RequestHeader, result: Result): Result = {
    val header = result.header

    val flashCookie = {
      header.headers.get(SET_COOKIE)
        .map(Cookies.decode(_))
        .flatMap(_.find(_.name == Flash.COOKIE_NAME)).orElse {
          Option(requestHeader.flash).filterNot(_.isEmpty).map { _ =>
            Flash.discard.toCookie
          }
        }
    }

    flashCookie.map { newCookie =>
      result.withHeaders(SET_COOKIE -> Cookies.merge(header.headers.get(SET_COOKIE).getOrElse(""), Seq(newCookie)))
    }.getOrElse(result)
  }

}
