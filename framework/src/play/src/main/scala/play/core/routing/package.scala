/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import play.utils.UriEncoding

/**
 * The play.core.routing package contains all the code necessary for Play's code generated routers.
 */
package object routing {

  def dynamicString(dynamic: String): String = {
    UriEncoding.encodePathSegment(dynamic, "utf-8")
  }

  def queryString(items: List[Option[String]]) = {
    Option(items.filter(_.isDefined).map(_.get).filterNot(_.isEmpty)).filterNot(_.isEmpty).map("?" + _.mkString("&")).getOrElse("")
  }

}
