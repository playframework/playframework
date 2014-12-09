/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import play.api._
import play.api.mvc._
import play.api.http.HeaderNames._

object ServerResultUtils {

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
