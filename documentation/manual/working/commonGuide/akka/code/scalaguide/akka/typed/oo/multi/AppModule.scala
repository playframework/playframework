/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo
package multi

import javax.inject.Inject
import javax.inject.Provider
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorRef
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.name.Names
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport

object AppModule extends AbstractModule with AkkaGuiceSupport {
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
