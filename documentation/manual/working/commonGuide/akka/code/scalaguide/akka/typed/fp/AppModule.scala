/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.fp

// #fp-app-module
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

object AppModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindTypedActor(HelloActor.create(), "hello-actor")  // uses "create" method
    bindTypedActor(ConfiguredActor, "configured-actor") // uses the object itself
  }
}
// #fp-app-module
