/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing

import javax.inject.{ Inject, Provider }

import play.api.inject._
import play.api.{ Configuration, Environment }
import play.api.mvc.PlayBodyParsers
import play.core.j.JavaContextComponents
import play.mvc.BodyParser.Default

/**
 * A Play binding for the RoutingDsl API.
 */
class RoutingDslModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[Default].toSelf, // this bind is here because it is needed by RoutingDsl only
      bind[RoutingDsl].toProvider[JavaRoutingDslProvider]
    )
  }
}

class JavaRoutingDslProvider @Inject() (bodyParser: play.mvc.BodyParser.Default, contextComponents: JavaContextComponents) extends Provider[RoutingDsl] {
  override def get(): RoutingDsl = new RoutingDsl(bodyParser, contextComponents)
}