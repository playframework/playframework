/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject
package guice

import com.google.inject.{ Binding => GuiceBinding, _ }
import com.google.inject.util.Providers
import play.api._

class GuiceLoadException(message: String) extends RuntimeException(message)

/**
 * An ApplicationLoader that uses guice to bootstrap the application.
 */
class GuiceApplicationLoader extends ApplicationLoader {

  def load(context: ApplicationLoader.Context): Application = {
    val builtinModule = new BuiltinModule

    val env = context.environment

    // Load global
    val global = GlobalSettings(context.initialConfiguration, env)

    // Create the final configuration
    // todo - abstract this logic out into something pluggable, with the default delegating to global
    val configuration = global.onLoadConfig(context.initialConfiguration, env.rootPath, env.classLoader, env.mode)

    Logger.configure(env.rootPath, configuration, env.mode)

    val bindings = builtinModule.bindings(context.environment, configuration) ++ Seq(
      BindingKey(classOf[GlobalSettings]) to global,
      BindingKey(classOf[OptionalSourceMapper]) to new OptionalSourceMapper(context.sourceMapper)
    )

    // load play module bindings
    val guiceModule = guiced(bindings)
    val injector = Guice.createInjector(guiceModule)
    injector.getInstance(classOf[Application])
  }

  private def guiced(bindings: Seq[Binding[_]]): AbstractModule = {
    new AbstractModule {
      def configure(): Unit = {
        for (b <- bindings) {
          val binding = b.asInstanceOf[Binding[Any]]
          val builder = bind(binding.key.clazz)
          for (qualifier <- binding.key.qualifiers) {
            qualifier match {
              case QualifierInstance(instance) => builder.annotatedWith(instance)
              case QualifierClass(clazz) => builder.annotatedWith(clazz)
            }
          }
          binding.target match {
            case ProviderTarget(provider) => builder.toProvider(Providers.guicify(provider))
            case ProviderConstructionTarget(provider) => builder.toProvider(provider)
            case ConstructionTarget(implementation) => builder.to(implementation)
          }
          for (scope <- binding.scope) {
            builder.in(scope)
            if (binding.eager) {
              if (scope eq classOf[javax.inject.Singleton])
                builder.asEagerSingleton()
              else
                throw new GuiceLoadException("Eager set on non-singleton scope")
            }
          }
        }
      }
    }
  }
}
