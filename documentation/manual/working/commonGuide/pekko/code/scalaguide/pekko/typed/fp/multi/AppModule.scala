/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.fp
package multi

import javax.inject.Inject
import javax.inject.Provider

import com.google.inject.name.Names
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.ActorSystem
import play.api.libs.concurrent.PekkoGuiceSupport
import play.api.Configuration

object AppModule extends AbstractModule with PekkoGuiceSupport {
  override def configure() = {
    def bindHelloActor(name: String) = {
      bind(new TypeLiteral[ActorRef[HelloActor.SayHello]]() {})
        .annotatedWith(Names.named(name))
        .toProvider(new Provider[ActorRef[HelloActor.SayHello]] {
          @Inject var actorSystem: ActorSystem = _

          def get() = actorSystem.spawn(HelloActor.create(), name)
        })
        .asEagerSingleton()
    }
    def bindConfiguredActor(name: String) = {
      bind(new TypeLiteral[ActorRef[ConfiguredActor.GetConfig]]() {})
        .annotatedWith(Names.named(name))
        .toProvider(new Provider[ActorRef[ConfiguredActor.GetConfig]] {
          @Inject var actorSystem: ActorSystem     = _
          @Inject var configuration: Configuration = _

          def get() = actorSystem.spawn(ConfiguredActor.create(configuration), name)
        })
        .asEagerSingleton()
    }
    bindHelloActor("hello-actor1")
    bindHelloActor("hello-actor2")
    bindConfiguredActor("configured-actor1")
    bindConfiguredActor("configured-actor2")
  }
}
