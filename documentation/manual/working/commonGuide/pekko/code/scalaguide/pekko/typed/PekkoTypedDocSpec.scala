/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed

import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.inject.Injector

final class PekkoTypedDocSpec extends Specification {
  "Runtime DI support for FP-style" in fpStyle
  "Runtime DI support for OO-style" in ooStyle

  "Runtime DI support for multi-instance FP-style" in fpStyleMulti
  "Runtime DI support for multi-instance OO-style" in ooStyleMulti

  "Compile-time DI without support works" in compileTimeDI

  private def fpStyle = { // functional programming style
    import fp._
    val main = newInjector(AppModule).instanceOf[Main]
    (main.helloActor !== null).and(main.configuredActor !== null)
  }

  private def ooStyle = { // object-oriented style
    import oo._
    val main = newInjector(AppModule).instanceOf[Main]
    (main.helloActor !== null).and(main.configuredActor !== null)
  }

  private def fpStyleMulti = { // with multiple spawned ActorRef[T]
    import fp.multi._
    val main = newInjector(AppModule).instanceOf[Main]
    (main.helloActor1 !== null)
      .and(main.helloActor2 !== null)
      .and(main.configuredActor1 !== null)
      .and(main.configuredActor2 !== null)
  }

  private def ooStyleMulti = { // with multiple spawned ActorRef[T]
    import oo.multi._
    val main = newInjector(AppModule).instanceOf[Main]
    (main.helloActor1 !== null)
      .and(main.helloActor2 !== null)
      .and(main.configuredActor1 !== null)
      .and(main.configuredActor2 !== null)
  }

  private def compileTimeDI = { // a sanity-check of what compile-time DI looks like
    import java.io.File
    import play.api._
    import fp._

    val environment   = Environment(new File("."), getClass.getClassLoader, Mode.Test)
    val context       = ApplicationLoader.Context.create(environment, Map("my.config" -> "foo"))
    val appComponents = new AppComponents(context)
    val main          = appComponents.main
    (main.helloActor !== null).and(main.configuredActor !== null)
  }

  private def newInjector(bindModules: GuiceableModule): Injector = {
    GuiceApplicationBuilder().configure("my.config" -> "foo").bindings(bindModules).injector()
  }
}
