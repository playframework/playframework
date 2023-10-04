/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.fp

// #compile-time-di
import org.apache.pekko.actor.typed.scaladsl.adapter._
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
// #compile-time-di
