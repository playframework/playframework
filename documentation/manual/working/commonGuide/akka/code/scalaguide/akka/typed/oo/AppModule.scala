/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #oo-app-module
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

object AppModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindTypedActor(classOf[HelloActor], "hello-actor")
    bindTypedActor(classOf[ConfiguredActor], "configured-actor")
  }
}
// #oo-app-module
