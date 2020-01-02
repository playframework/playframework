/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

import akka.actor.typed.scaladsl.adapter._
import play.api._
import play.api.routing.Router

final class AppComponents(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with NoHttpFiltersComponents {
  val router = Router.empty

  val helloActor = {
    actorSystem.spawn(HelloActor.create(), "hello-actor")
  }
  val configuredActor = {
    val behavior = ConfiguredActor.create(configuration)
    actorSystem.spawn(behavior, "configured-actor")
  }

  val main = new Main(helloActor, configuredActor)
}
