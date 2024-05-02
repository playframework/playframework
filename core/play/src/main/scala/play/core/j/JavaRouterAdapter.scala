/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import javax.inject.Inject

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import play.mvc.Http.RequestHeader
import play.routing.Router.RouteDocumentation

/**
 * Adapts the Scala router to the Java Router API
 */

class JavaRouterAdapter @Inject() (underlying: play.api.routing.Router) extends play.routing.Router {
  def route(requestHeader: RequestHeader)             = underlying.handlerFor(requestHeader.asScala()).toJava
  def withPrefix(prefix: String): play.routing.Router = new JavaRouterAdapter(asScala.withPrefix(prefix))
  def documentation() =
    asScala.documentation.map {
      case (httpMethod, pathPattern, controllerMethodInvocation) =>
        new RouteDocumentation(httpMethod, pathPattern, controllerMethodInvocation)
    }.asJava
  override def asScala = underlying
}
