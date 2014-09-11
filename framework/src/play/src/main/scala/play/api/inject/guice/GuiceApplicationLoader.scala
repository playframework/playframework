/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject
package guice

import com.google.inject._
import play.api.inject.{ Module => PlayModule, Binding => PlayBinding, Injector => PlayInjector }
import play.core.WebCommands
import com.google.inject.util.Providers
import play.api._

import scala.reflect.ClassTag

class GuiceLoadException(message: String) extends RuntimeException(message)

/**
 * An ApplicationLoader that uses guice to bootstrap the application.
 */
class GuiceApplicationLoader extends ApplicationLoader {

  def load(context: ApplicationLoader.Context): Application = {

    val env = context.environment

    // Load global
    val global = GlobalSettings(context.initialConfiguration, env)

    // Create the final configuration
    // todo - abstract this logic out into something pluggable, with the default delegating to global
    val configuration = global.onLoadConfig(context.initialConfiguration, env.rootPath, env.classLoader, env.mode)

    Logger.configure(env, configuration)

    val modules = guiced(Seq(
      BindingKey(classOf[GlobalSettings]) to global,
      BindingKey(classOf[OptionalSourceMapper]) to new OptionalSourceMapper(context.sourceMapper),
      BindingKey(classOf[WebCommands]) to context.webCommands
    )) +: (Modules.locate(env, configuration) ++ additionalModules)

    try {
      val injector = createGuiceInjector(env, configuration, modules)
      injector.getInstance(classOf[Application])
    } catch {
      case e: CreationException => e.getCause match {
        case p: PlayException => throw p
        case _ => throw e
      }
    }
  }

  /**
   * Extension point to add module bindings in a programmatic way. This class creates a Guice injector loading the
   * modules returned by this method.
   *
   * The default implementation returns an empty sequence. Override this implementation to add modules.
   */
  protected def additionalModules: Seq[Module] = Seq.empty

  override def createInjector(environment: Environment, configuration: Configuration, modules: Seq[Any]) = {
    Some(createGuiceInjector(environment, configuration, modules).getInstance(classOf[PlayInjector]))
  }

  private def createGuiceInjector(environment: Environment, configuration: Configuration, modules: Seq[Any]) = {
    val guiceModules = modules.map {
      case playModule: PlayModule => guiced(playModule.bindings(environment, configuration))
      case guiceModule: Module => guiceModule
      case unknown => throw new PlayException(
        "Unknown module type",
        s"Module [$unknown] is not a Play module or a Guice module"
      )
    } :+ guiced(Seq(BindingKey(classOf[PlayInjector]).to[GuiceInjector]))

    import scala.collection.JavaConverters._

    Guice.createInjector(guiceModules.asJavaCollection)
  }

  private def guiced(bindings: Seq[PlayBinding[_]]): AbstractModule = {
    new AbstractModule {
      def configure(): Unit = {
        for (b <- bindings) {
          val binding = b.asInstanceOf[PlayBinding[Any]]
          val builder = binder().withSource(binding).bind(GuiceKey.toGuice(binding.key))
          binding.target.foreach {
            case ProviderTarget(provider) => builder.toProvider(Providers.guicify(provider))
            case ProviderConstructionTarget(provider) => builder.toProvider(provider)
            case ConstructionTarget(implementation) => builder.to(implementation)
            case BindingKeyTarget(key) => builder.to(GuiceKey.toGuice(key))
          }
          (binding.scope, binding.eager) match {
            case (Some(scope), false) => builder.in(scope)
            case (None, true) => builder.asEagerSingleton()
            case (Some(scope), true) => throw new GuiceLoadException("A binding must either declare a scope or be eager: " + binding)
            case _ => // do nothing
          }
        }
      }
    }
  }

}

object GuiceKey {
  def toGuice[T](key: BindingKey[T]): Key[T] = {
    key.qualifier match {
      case Some(QualifierInstance(instance)) => Key.get(key.clazz, instance)
      case Some(QualifierClass(clazz)) => Key.get(key.clazz, clazz)
      case None => Key.get(key.clazz)
    }
  }
}

private[play] class GuiceInjector @Inject() (injector: Injector) extends PlayInjector {
  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T](implicit ct: ClassTag[T]) = instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T](clazz: Class[T]) = injector.getInstance(clazz)

  /**
   * Get an instance bound to the given binding key.
   */
  def instanceOf[T](key: BindingKey[T]) = injector.getInstance(GuiceKey.toGuice(key))
}
