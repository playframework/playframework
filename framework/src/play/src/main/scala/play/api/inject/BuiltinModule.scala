/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import javax.inject.{ Singleton, Inject, Provider }

import play.api._
import play.api.libs.{ CryptoConfig, Crypto, CryptoConfigParser }
import play.core.Router

class BuiltinModule extends Module {
  def bindings(env: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[Environment] to env,
      bind[Configuration] to configuration,

      // Application lifecycle, bound both to the interface, and its implementation, so that Application can access it
      // to shut it down.
      bind[DefaultApplicationLifecycle].toSelf,
      bind[ApplicationLifecycle].to(bind[DefaultApplicationLifecycle]),

      bind[Application].to[DefaultApplication],
      bind[play.inject.Injector].to[play.inject.DelegateInjector],
      // bind Plugins - eager

      bind[Router.Routes].toProvider[RoutesProvider],
      bind[Plugins].toProvider[PluginsProvider],

      bind[CryptoConfig].toProvider[CryptoConfigParser],
      bind[Crypto].toSelf
    )
  }
}

@Singleton
class RoutesProvider @Inject() (injector: Injector, environment: Environment, configuration: Configuration) extends Provider[Router.Routes] {
  lazy val get = {
    val prefix = configuration.getString("application.context").map { prefix =>
      if (!prefix.startsWith("/")) {
        throw configuration.reportError("application.context", "Invalid application context")
      }
      prefix
    }

    val router = Router.load(environment, configuration)
      .fold[Router.Routes](Router.Null)(injector.instanceOf(_))
    prefix.fold(router)(router.withPrefix)
  }
}

@Singleton
class PluginsProvider @Inject() (environment: Environment, injector: Injector) extends Provider[Plugins] {
  lazy val get = Plugins(environment, injector)
}