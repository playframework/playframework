package play.routing

import javax.inject.{ Inject, Provider }

import play.api.inject._
import play.api.{ Configuration, Environment }
import play.api.mvc.PlayBodyParsers
import play.core.j.JavaContextComponents

/**
 * A Play binding for the RoutingDsl API.
 */
class RoutingDslModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(bind[RoutingDsl].toProvider[RoutingDslProvider])
  }
}

class RoutingDslProvider @Inject() (bodyParsers: PlayBodyParsers, contextComponents: JavaContextComponents) extends Provider[RoutingDsl] {
  override def get(): RoutingDsl = new RoutingDsl(bodyParsers.default, contextComponents)
}
