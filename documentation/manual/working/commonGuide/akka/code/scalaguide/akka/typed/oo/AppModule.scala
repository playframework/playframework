/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-app-module
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.ActorRef
import com.google.inject.AbstractModule
import com.google.inject.Provider
import com.google.inject.TypeLiteral
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport

object AppModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindTypedActor(HelloActor.create(), "hello-actor")
    bind(new TypeLiteral[ActorRef[ConfiguredActor.GetConfig]]() {})
      .toProvider(new Provider[ActorRef[ConfiguredActor.GetConfig]] {
        @Inject var actorSystem: ActorSystem     = _
        @Inject var configuration: Configuration = _

        def get() = actorSystem.spawn(ConfiguredActor.create(configuration), "configured-actor")
      })
      .asEagerSingleton()
  }
}
// #oo-app-module
