/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject

import java.util.concurrent.Executor

import javax.inject.{ Inject, Provider, Singleton }
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.stream.Materializer
import com.typesafe.config.Config
import play.api._
import play.api.http.HttpConfiguration._
import play.api.http._
import play.api.libs.Files.TemporaryFileReaperConfigurationProvider
import play.api.libs.Files._
import play.api.libs.concurrent._
import play.api.mvc._
import play.api.mvc.request.{ DefaultRequestFactory, RequestFactory }
import play.api.routing.Router
import play.core.j.JavaRouterAdapter
import play.core.routing.GeneratedRouter
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
    bind[MessagesActionBuilder].to[DefaultMessagesActionBuilderImpl],
    bind[MessagesControllerComponents].to[DefaultMessagesControllerComponents],
    bind[Futures].to[DefaultFutures],

    // Application lifecycle, bound both to the interface, and its implementation, so that Application can access it
    // to shut it down.
    bind[DefaultApplicationLifecycle].toSelf,
    bind[ApplicationLifecycle].to(bind[DefaultApplicationLifecycle]),

    bind[Application].to[DefaultApplication],
    bind[play.Application].to[play.DefaultApplication],

    bind[play.routing.Router].to[JavaRouterAdapter],
    bind[ActorSystem].toProvider[ActorSystemProvider],
    bind[Materializer].toProvider[MaterializerProvider],
    bind[CoordinatedShutdown].toProvider[CoordinatedShutdownProvider],
    bind[ExecutionContextExecutor].toProvider[ExecutionContextProvider],
    bind[ExecutionContext].to(bind[ExecutionContextExecutor]),
    bind[Executor].to(bind[ExecutionContextExecutor]),
    bind[HttpExecutionContext].toSelf,

    bind[play.core.j.JavaContextComponents].to[play.core.j.DefaultJavaContextComponents],
    bind[play.core.j.JavaHandlerComponents].to[play.core.j.DefaultJavaHandlerComponents],
    bind[FileMimeTypes].toProvider[DefaultFileMimeTypesProvider]
  ) ++ dynamicBindings(
      HttpErrorHandler.bindingsFromConfiguration,
      HttpFilters.bindingsFromConfiguration,
      HttpRequestHandler.bindingsFromConfiguration,
      ActionCreator.bindingsFromConfiguration,
      RoutesProvider.bindingsFromConfiguration
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

object RoutesProvider {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val routerClass = Router.load(environment, configuration)

    // If it's a generated router, then we need to provide a binding for it. Otherwise, it's the users
    // (or the library that provided the router) job to provide a binding for it.
    val routerInstanceBinding = routerClass match {
      case Some(generated) if classOf[GeneratedRouter].isAssignableFrom(generated) =>
        Seq(bind(generated).toSelf)
      case _ => Nil
    }
    routerInstanceBinding :+ bind[Router].toProvider[RoutesProvider]
  }
}
