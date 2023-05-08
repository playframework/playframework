/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing

import jakarta.inject.Inject
import jakarta.inject.Provider
import play.api.inject._
import play.api.Configuration
import play.api.Environment
import play.core.j.JavaContextComponents
import play.mvc.BodyParser.Default

/**
 * A Play binding for the RoutingDsl API.
 */
class RoutingDslModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[?]] = {
    Seq(
      bind[Default].toSelf, // this bind is here because it is needed by RoutingDsl only
      bind[RoutingDsl].toProvider[JavaRoutingDslProvider]
    )
  }
}

class JavaRoutingDslProvider @Inject() (
    bodyParser: play.mvc.BodyParser.Default
) extends Provider[RoutingDsl] {
  @deprecated("Use constructor without JavaContextComponents", "2.8.0")
  def this(bodyParser: play.mvc.BodyParser.Default, contextComponents: JavaContextComponents) = {
    this(bodyParser)
  }
  override def get(): RoutingDsl = new RoutingDsl(bodyParser)
}
