/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.fp

// #fp-app-module
import com.google.inject.AbstractModule
import play.api.libs.concurrent.PekkoGuiceSupport

object AppModule extends AbstractModule with PekkoGuiceSupport {
  override def configure() = {
    bindTypedActor(HelloActor.create(), "hello-actor")  // uses "create" method
    bindTypedActor(ConfiguredActor, "configured-actor") // uses the object itself
  }
}
// #fp-app-module
