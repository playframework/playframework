/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject.guice

import scala.jdk.CollectionConverters._

import com.google.inject.name.Names
import com.google.inject.AbstractModule
import com.google.inject.CreationException
import com.google.inject.Key
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.specs2.mutable.Specification
import play.api.inject._
import play.api.Mode
import play.inject.guice.NoScope

class NoScopeSpec extends Specification {
  import NoScopeSpec._

  "NoScope" should {
    for (mode <- Seq(Mode.Dev, Mode.Prod)) {
      s"construct fresh instances and preserve the implementation singleton in $mode" in {
        val injector = new GuiceInjectorBuilder()
          .in(mode)
          .bindings(new SimpleModule((_, _) => Seq(bind[Service].to[SingletonService].in[NoScope])))
          .injector()
        val first          = injector.instanceOf[Service]
        val implementation = injector.instanceOf[SingletonService]

        first must not(beTheSameAs(injector.instanceOf[Service]))
        first must not(beTheSameAs(implementation))
        implementation must beTheSameAs(injector.instanceOf[SingletonService])
      }

      s"override scope annotations on self bindings in $mode" in {
        val injector = new GuiceInjectorBuilder()
          .in(mode)
          .bindings(bind[Service].to[SingletonService], bind[SingletonService].toSelf.in[NoScope])
          .injector()

        injector.instanceOf[Service] must not(beTheSameAs(injector.instanceOf[Service]))
        injector.instanceOf[SingletonService] must not(beTheSameAs(injector.instanceOf[SingletonService]))
      }
    }

    "support qualified construction bindings" in {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          bind[Service].qualifiedWith("fresh").to[SingletonService].in[NoScope],
          bind[Service].to[SingletonService]
        )
        .injector()
        .instanceOf[com.google.inject.Injector]
      val fresh = Key.get(classOf[Service], Names.named("fresh"))

      injector.getInstance(fresh) must not(beTheSameAs(injector.getInstance(fresh)))
      injector.getInstance(classOf[Service]) must beTheSameAs(injector.getInstance(classOf[SingletonService]))
    }

    "respect Play's requirement for annotated constructors" in {
      new GuiceInjectorBuilder()
        .requireAtInjectOnConstructors()
        .bindings(bind[Service].to[SingletonService].in[NoScope])
        .injector() must throwA[CreationException].like {
        case error => error.getMessage must contain("requires a constructor annotated with @Inject")
      }
    }

    "inject constructor dependencies with strict binder options" in {
      val injector = new GuiceInjectorBuilder()
        .requireAtInjectOnConstructors()
        .requireExplicitBindings()
        .bindings(bind[Service].to[InjectedService].in[NoScope], bind[Dependency].toSelf)
        .injector()
      val first = injector.instanceOf[Service].asInstanceOf[InjectedService]

      first must not(beTheSameAs(injector.instanceOf[Service]))
      first.dependency must beTheSameAs(injector.instanceOf[Dependency])
    }

    "use explicit constructor semantics for requirements enabled by a native Guice module" in {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          new AbstractModule {
            override def configure(): Unit = binder().requireAtInjectOnConstructors()
          },
          bind[Service].to[SingletonService].in[NoScope]
        )
        .injector()

      injector.instanceOf[Service] must not(beTheSameAs(injector.instanceOf[Service]))
    }

    "leave instance reuse to providers and alias targets" in {
      val cached   = new SingletonService
      val injector = new GuiceInjectorBuilder()
        .bindings(
          bind[Service]
            .qualifiedWith("provider")
            .to(new jakarta.inject.Provider[Service] {
              override def get(): Service = cached
            })
            .in[NoScope],
          bind[Service].qualifiedWith("alias").to(bind[SingletonService]).in[NoScope]
        )
        .injector()
        .instanceOf[com.google.inject.Injector]

      injector.getInstance(Key.get(classOf[Service], Names.named("provider"))) must beTheSameAs(cached)
      injector.getInstance(Key.get(classOf[Service], Names.named("alias"))) must beTheSameAs(
        injector.getInstance(classOf[SingletonService])
      )
    }

    "bypass implementation overrides and accept interface overrides" in {
      val replacement            = new SingletonService
      val builder                = new GuiceInjectorBuilder().bindings(bind[Service].to[SingletonService].in[NoScope])
      val implementationOverride = builder.overrides(bind[SingletonService].toInstance(replacement)).injector()

      implementationOverride.instanceOf[SingletonService] must beTheSameAs(replacement)
      implementationOverride.instanceOf[Service] must not(beTheSameAs(replacement))
      builder.overrides(bind[Service].toInstance(replacement)).injector().instanceOf[Service] must beTheSameAs(
        replacement
      )
    }

    "require an override to replace a library's linked binding" in {
      val library = new GuiceInjectorBuilder().bindings(bind[Service].to[SingletonService])
      library.bindings(bind[Service].to[SingletonService].in[NoScope]).injector() must throwA[CreationException].like {
        case error => error.getMessage must contain("BindingAlreadySet")
      }
      val injector = library.overrides(bind[Service].to[SingletonService].in[NoScope]).injector()

      injector.instanceOf[Service] must not(beTheSameAs(injector.instanceOf[Service]))
    }

    "report constructor errors without falling back to a linked binding" in {
      new GuiceInjectorBuilder()
        .bindings(bind[Service].to[NoDefaultConstructor].in[NoScope])
        .injector() must throwA[CreationException].like {
        case error =>
          val messages = error.asInstanceOf[CreationException].getErrorMessages.asScala.map(_.getMessage).mkString("\n")
          messages must contain("NoScope binding to " + classOf[NoDefaultConstructor].getName)
          messages must contain("Use a provider binding")
          error.getMessage must not(contain("MissingImplementation"))
      }
    }

    "reject an eager binding with NoScope" in {
      new GuiceInjectorBuilder()
        .bindings(bind[Service].to[SingletonService].in[NoScope].eagerly())
        .injector() must throwA[CreationException].like {
        case error => error.getMessage must contain("A binding must either declare a scope or be eager")
      }
    }
  }
}

object NoScopeSpec {
  trait Service

  @Singleton
  class SingletonService extends Service

  @Singleton
  class Dependency @Inject() ()

  @Singleton
  class InjectedService @Inject() (val dependency: Dependency) extends Service

  class NoDefaultConstructor(value: String) extends Service
}
