/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing

import play.api.{ ApplicationLoader, BuiltInComponentsFromContext }
import play.api.mvc.{ AnyContent, BodyParser }
import play.core.j.JavaContextComponents

/**
 * RoutingDsl components (for compile-time injection).
 */
trait RoutingDslComponents {
  def defaultBodyParser: BodyParser[AnyContent]
  def javaContextComponents: JavaContextComponents
  def routingDsl: RoutingDsl = new RoutingDsl(defaultBodyParser, javaContextComponents)
}

/**
 * RoutingDsl components from the built in components.
 *
 * @see [[BuiltInComponentsFromContext]]
 * @see [[RoutingDslComponents]]
 */
abstract class RoutingDslComponentsFromContext(context: ApplicationLoader.Context) extends BuiltInComponentsFromContext(context) with RoutingDslComponents