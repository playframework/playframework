/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule

final class AkkaTypedDocSpec extends Specification {
  import AkkaTypedDocSpec._

  "Runtime DI support for FP-style typed actors" in (fpStyle.mainIsInjected)
  "Runtime DI support for OO-style typed actors" in (ooStyle.mainIsInjected)
  "Runtime DI support for multi-instance FP-style typed actors" in (fpStyleMulti.mainIsInjected)
  "Runtime DI support for multi-instance OO-style typed actors" in (ooStyleMulti.mainIsInjected)
  "Compile-time DI without support works" in (compileTimeDI.works)

}

object AkkaTypedDocSpec {

  private object fpStyle { // functional programming style
    import fp._

    def mainIsInjected(): MatchResult[_] = {
      val main = newInjector(AppModule).instanceOf[Main]
      (main.helloActor !== null).and(main.configuredActor !== null)
    }
  }

  private object ooStyle { // object-oriented style
    import oo._

    def mainIsInjected(): MatchResult[_] = {
      val main = newInjector(AppModule).instanceOf[Main]
      (main.helloActor !== null).and(main.configuredActor !== null)
    }
  }

  private object fpStyleMulti { // with multiple spawned ActorRef[T]
    import fp.multi._

    def mainIsInjected(): MatchResult[_] = {
      val main = newInjector(AppModule).instanceOf[Main]
      (main.helloActor1 !== null)
        .and(main.helloActor2 !== null)
        .and(main.configuredActor1 !== null)
        .and(main.configuredActor2 !== null)
    }
  }

  private object ooStyleMulti { // with multiple spawned ActorRef[T]
    import oo.multi._

    def mainIsInjected(): MatchResult[_] = {
      val main = newInjector(AppModule).instanceOf[Main]
      (main.helloActor1 !== null)
        .and(main.helloActor2 !== null)
        .and(main.configuredActor1 !== null)
        .and(main.configuredActor2 !== null)
    }
  }

  private object compileTimeDI { // a sanity-check of what compile-time DI looks like
    import java.io.File
    import play.api._
    import fp._

    def works(): MatchResult[_] = {
      val environment   = Environment(new File("."), getClass.getClassLoader, Mode.Test)
      val context       = ApplicationLoader.Context.create(environment, Map("my.config" -> "foo"))
      val appComponents = new AppComponents(context)
      val main          = appComponents.main
      (main.helloActor !== null).and(main.configuredActor !== null)
    }
  }

  private def newInjector(bindModules: GuiceableModule): Injector = {
    GuiceApplicationBuilder().configure("my.config" -> "foo").bindings(bindModules).injector()
  }
}
