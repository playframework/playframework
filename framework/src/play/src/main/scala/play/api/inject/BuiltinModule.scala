/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.inject

import java.util.concurrent.Executor
import javax.inject.{ Inject, Provider, Singleton }

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.Config
import play.api._
import play.api.http.HttpConfiguration._
import play.api.http._
import play.api.libs.Files.TemporaryFileReaperConfiguration.TemporaryFileReaperConfigurationProvider
import play.api.libs.Files._
import play.api.libs.concurrent.{ ActorSystemProvider, ExecutionContextProvider, MaterializerProvider }
import play.api.mvc._
import play.api.mvc.request.{ DefaultRequestFactory, RequestFactory }
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
class BuiltinModule extends SimpleModule((env, conf) => {
  def dynamicBindings(factories: ((Environment, Configuration) => Seq[Binding[_]])*) = {
    factories.flatMap(_ (env, conf))
  }

  Seq(
    bind[Environment] to env,
    bind[ConfigurationProvider].to(new ConfigurationProvider(conf)),
    bind[Configuration].toProvider[ConfigurationProvider],
    bind[Config].toProvider[ConfigProvider],
    bind[HttpConfiguration].toProvider[HttpConfigurationProvider],
    bind[ParserConfiguration].toProvider[ParserConfigurationProvider],
    bind[CookiesConfiguration].toProvider[CookiesConfigurationProvider],
    bind[FlashConfiguration].toProvider[FlashConfigurationProvider],
    bind[SessionConfiguration].toProvider[SessionConfigurationProvider],
    bind[ActionCompositionConfiguration].toProvider[ActionCompositionConfigurationProvider],
    bind[FileMimeTypesConfiguration].toProvider[FileMimeTypesConfigurationProvider],
    bind[SecretConfiguration].toProvider[SecretConfigurationProvider],
    bind[TemporaryFileReaperConfiguration].toProvider[TemporaryFileReaperConfigurationProvider],

    bind[CookieHeaderEncoding].to[DefaultCookieHeaderEncoding],
    bind[RequestFactory].to[DefaultRequestFactory],
    bind[TemporaryFileReaper].to[DefaultTemporaryFileReaper],
    bind[TemporaryFileCreator].to[DefaultTemporaryFileCreator],
    bind[PlayBodyParsers].to[DefaultPlayBodyParsers],
    bind[BodyParsers.Default].toSelf,
    bind[DefaultActionBuilder].to[DefaultActionBuilderImpl],
    bind[ControllerComponents].to[DefaultControllerComponents],

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

    bind[FileMimeTypes].toProvider[DefaultFileMimeTypesProvider]
  ) ++ dynamicBindings(
      HttpErrorHandler.bindingsFromConfiguration,
      HttpFilters.bindingsFromConfiguration,
      HttpRequestHandler.bindingsFromConfiguration,
      ActionCreator.bindingsFromConfiguration
    )
})

// This allows us to access the original configuration via this
// provider while overriding the binding for Configuration itself.
class ConfigurationProvider(val get: Configuration) extends Provider[Configuration]

class ConfigProvider @Inject() (configuration: Configuration) extends Provider[Config] {
  override def get() = configuration.underlying
}

@Singleton
class RoutesProvider @Inject() (injector: Injector, environment: Environment, configuration: Configuration, httpConfig: HttpConfiguration) extends Provider[Router] {
  lazy val get = {
    val prefix = httpConfig.context

    val router = Router.load(environment, configuration)
      .fold[Router](Router.empty)(injector.instanceOf(_))
    router.withPrefix(prefix)
  }
}
