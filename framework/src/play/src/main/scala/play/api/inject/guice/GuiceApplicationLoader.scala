/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject
package guice

import com.google.inject.{ Binding => GuiceBinding, _ }
import com.google.inject.util.Providers
import play.api.{ Application, ApplicationLoader, Environment }

class GuiceLoadException(message: String) extends RuntimeException(message)

class GuiceApplicationLoader extends ApplicationLoader {
  def load(env: Environment): Application = {
    val builtinModule = new BuiltinModule
    val bindings = builtinModule.bindings(env)
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
