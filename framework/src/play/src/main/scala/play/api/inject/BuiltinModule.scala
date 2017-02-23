/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.inject

import java.util.concurrent.Executor
import javax.inject.{ Inject, Provider, Singleton }

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api._
import play.api.http._
import play.api.libs.Files.{ DefaultTemporaryFileCreator, TemporaryFileCreator }
import play.api.libs.concurrent.{ ActorSystemProvider, ExecutionContextProvider, MaterializerProvider }
import play.api.libs.crypto._
import play.api.routing.Router
import play.core.j.JavaRouterAdapter
import play.libs.concurrent.HttpExecutionContext

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

/**
 * The Play BuiltinModule.
 *
 * Provides all the core components of a Play application. This is typically automatically enabled by Play for an
 * application.
 */
class BuiltinModule extends Module {
  def bindings(env: Environment, configuration: Configuration): Seq[Binding[_]] = {
    def dynamicBindings(factories: ((Environment, Configuration) => Seq[Binding[_]])*) = {
      factories.flatMap(_(env, configuration))
    }

    Seq(
      bind[Environment] to env,
      bind[ConfigurationProvider].to(new ConfigurationProvider(configuration)),
      bind[Configuration].toProvider[ConfigurationProvider],
      bind[HttpConfiguration].toProvider[HttpConfiguration.HttpConfigurationProvider],

      // Application lifecycle, bound both to the interface, and its implementation, so that Application can access it
      // to shut it down.
      bind[DefaultApplicationLifecycle].toSelf,
      bind[ApplicationLifecycle].to(bind[DefaultApplicationLifecycle]),

      bind[Application].to[DefaultApplication],
      bind[play.Application].to[play.DefaultApplication],

      bind[Router].toProvider[RoutesProvider],
      bind[play.routing.Router].to[JavaRouterAdapter],
      bind[ActorSystem].toProvider[ActorSystemProvider],
      bind[Materializer].toProvider[MaterializerProvider],
      bind[ExecutionContextExecutor].toProvider[ExecutionContextProvider],
      bind[ExecutionContext].to[ExecutionContextExecutor],
      bind[Executor].to[ExecutionContextExecutor],
      bind[HttpExecutionContext].toSelf,

      bind[CryptoConfig].toProvider[CryptoConfigParser],
      bind[CookieSigner].toProvider[CookieSignerProvider],
      bind[CSRFTokenSigner].toProvider[CSRFTokenSignerProvider],
      bind[AESCrypter].toProvider[AESCrypterProvider],
      bind[play.api.libs.Crypto].toSelf,
      bind[TemporaryFileCreator].to[DefaultTemporaryFileCreator]
    ) ++ dynamicBindings(
        HttpErrorHandler.bindingsFromConfiguration,
        HttpFilters.bindingsFromConfiguration,
        HttpRequestHandler.bindingsFromConfiguration,
        ActionCreator.bindingsFromConfiguration
      )
  }
}

// This allows us to access the original configuration via this
// provider while overriding the binding for Configuration itself.
class ConfigurationProvider(val get: Configuration) extends Provider[Configuration]

@Singleton
class RoutesProvider @Inject() (injector: Injector, environment: Environment, configuration: Configuration, httpConfig: HttpConfiguration) extends Provider[Router] {
  lazy val get = {
    val prefix = httpConfig.context

    val router = Router.load(environment, configuration)
      .fold[Router](Router.empty)(injector.instanceOf(_))
    router.withPrefix(prefix)
  }
}
