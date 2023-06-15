/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-app-module
import javax.inject.Inject

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorRef
import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.Provider
import com.google.inject.TypeLiteral
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.Configuration

object AppModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindTypedActor(HelloActor.create(), "hello-actor")
    bind(new TypeLiteral[ActorRef[ConfiguredActor.GetConfig]]() {})
      .toProvider(classOf[ConfiguredActorProvider])
      .asEagerSingleton()
  }

  private class ConfiguredActorProvider @Inject() (
      actorSystem: ActorSystem,
      configuration: Configuration,
  ) extends Provider[ActorRef[ConfiguredActor.GetConfig]] {
    def get() = {
      val behavior = ConfiguredActor.create(configuration)
      actorSystem.spawn(behavior, "configured-actor")
    }
  }
}
// #oo-app-module
