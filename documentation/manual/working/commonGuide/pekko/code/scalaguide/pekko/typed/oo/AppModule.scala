/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.oo

// #oo-app-module
import javax.inject.Inject

import com.google.inject.AbstractModule
import com.google.inject.Provider
import com.google.inject.TypeLiteral
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.ActorSystem
import play.api.libs.concurrent.PekkoGuiceSupport
import play.api.Configuration

object AppModule extends AbstractModule with PekkoGuiceSupport {
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
