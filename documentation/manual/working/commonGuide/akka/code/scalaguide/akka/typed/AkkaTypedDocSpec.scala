/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import com.google.inject.name.Names
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.specs2.matcher.Matchers._
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.libs.concurrent.AkkaGuiceSupport

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
    import fp._

    def mainIsInjected(): MatchResult[_] = {
      val main = newInjector(AppModule).instanceOf[Main]
      (main.helloActor1 !== null)
        .and(main.helloActor2 !== null)
        .and(main.configuredActor1 !== null)
        .and(main.configuredActor2 !== null)
    }

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

    @Singleton final class Main @Inject()(
        @Named("hello-actor1") val helloActor1: ActorRef[HelloActor.SayHello],
        @Named("hello-actor2") val helloActor2: ActorRef[HelloActor.SayHello],
        @Named("configured-actor1") val configuredActor1: ActorRef[ConfiguredActor.GetConfig],
        @Named("configured-actor2") val configuredActor2: ActorRef[ConfiguredActor.GetConfig],
    )
  }

  private object ooStyleMulti { // with multiple spawned ActorRef[T]
    import oo._

    def mainIsInjected(): MatchResult[_] = {
      val main = newInjector(AppModule).instanceOf[Main]
      (main.helloActor1 !== null)
        .and(main.helloActor2 !== null)
        .and(main.configuredActor1 !== null)
        .and(main.configuredActor2 !== null)
    }

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

    @Singleton final class Main @Inject()(
        @Named("hello-actor1") val helloActor1: ActorRef[HelloActor.SayHello],
        @Named("hello-actor2") val helloActor2: ActorRef[HelloActor.SayHello],
        @Named("configured-actor1") val configuredActor1: ActorRef[ConfiguredActor.GetConfig],
        @Named("configured-actor2") val configuredActor2: ActorRef[ConfiguredActor.GetConfig],
    )
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

    final class Main(
        val helloActor: ActorRef[HelloActor.SayHello],
        val configuredActor: ActorRef[ConfiguredActor.GetConfig],
    )
  }

  private def newInjector(bindModules: GuiceableModule): Injector = {
    GuiceApplicationBuilder().configure("my.config" -> "foo").bindings(bindModules).injector()
  }

}
