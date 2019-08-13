/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.fp

// #fp-app-module
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

object AppModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindTypedActor(HelloActor(), "hello-actor")         // uses apply
    bindTypedActor(ConfiguredActor, "configured-actor") // uses object
  }
}
// #fp-app-module
