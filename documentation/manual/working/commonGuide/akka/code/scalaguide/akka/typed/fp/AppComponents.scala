/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.fp

// #compile-time-di
import akka.actor.typed.scaladsl.adapter._
import play.api._
import play.api.routing.Router

final class AppComponents(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with NoHttpFiltersComponents {
  val router = Router.empty

  val helloActor =
    actorSystem.spawn(HelloActor(), "hello-actor")
  val configuredActor =
    actorSystem.spawn(ConfiguredActor(configuration), "configured-actor")

  val main = new Main(helloActor, configuredActor)
}
// #compile-time-di
