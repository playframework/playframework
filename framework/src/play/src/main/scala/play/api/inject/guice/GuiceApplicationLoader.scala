/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject
package guice

import com.google.inject.{ Module => GuiceModule, _ }
import com.google.inject.util.Providers
import play.api._
import play.api.inject.{ Module => PlayModule, Binding => PlayBinding, Injector => PlayInjector }
import play.core.WebCommands

import scala.reflect.ClassTag

class GuiceLoadException(message: String) extends RuntimeException(message)

/**
 * An ApplicationLoader that uses guice to bootstrap the application.
 */
class GuiceApplicationLoader(val additionalModules: GuiceModule*) extends ApplicationLoader {
  def this() = this(Seq.empty: _*)

  /**
   * Load the modules from the environment and configuration.
   *
   * By default this uses `Modules.locate` to find modules in `play.modules` files, and adds `additionalModules`.
   *
   * Override this method if you want to disable the autoloading functionality and manually install all Guice
   * modules, including those provided by Play and third-party libraries.
   *
   * @return a `Seq[com.google.inject.Module]`, containing Guice modules to be loaded
   */
  protected def loadModules(env: Environment, conf: Configuration): Seq[GuiceModule] =
    Modules.locate(env, conf).map(guicify(env, conf, _)) ++ additionalModules

  import GuiceApplicationLoader._

  def load(context: ApplicationLoader.Context): Application = {

    val env = context.environment

    // Load global
    val global = GlobalSettings(context.initialConfiguration, env)

    // Create the final configuration
    // todo - abstract this logic out into something pluggable, with the default delegating to global
    val configuration = global.onLoadConfig(context.initialConfiguration, env.rootPath, env.classLoader, env.mode)

    Logger.configure(env, configuration)

    val guiceModules = guiced(Seq(
      BindingKey(classOf[GlobalSettings]) to global,
      BindingKey(classOf[OptionalSourceMapper]) to new OptionalSourceMapper(context.sourceMapper),
      BindingKey(classOf[WebCommands]) to context.webCommands
    )) +: loadModules(env, configuration)

    try {
      val injector = createGuiceInjector(guiceModules)
      injector.getInstance(classOf[Application])
    } catch {
      case e: CreationException => e.getCause match {
        case p: PlayException => throw p
        case _ => throw e
      }
    }
  }

  override def createInjector(env: Environment, conf: Configuration, modules: Seq[Any]) = {
    Some(createGuiceInjector(modules.map(guicify(env, conf, _))).getInstance(classOf[PlayInjector]))
  }

  private def createGuiceInjector(modules: Seq[GuiceModule]) = {
    val guiceModules = modules :+ guiced(Seq(BindingKey(classOf[PlayInjector]).to[GuiceInjector]))
    import scala.collection.JavaConverters._
    Guice.createInjector(guiceModules.asJavaCollection)
  }

  /**
   * Attempt to convert a module of unknown type to a Guice module
   */
  protected def guicify(env: Environment, conf: Configuration, module: Any): GuiceModule = module match {
    case playModule: PlayModule => guiced(playModule.bindings(env, conf))
    case guiceModule: GuiceModule => guiceModule
    case unknown => throw new PlayException(
      "Unknown module type",
      s"Module [$unknown] is not a Play module or a Guice module"
    )
  }

}

object GuiceApplicationLoader {

  /**
   * Convert the given bindings Play bindings to a Guice module
   */
  private[play] def guiced(bindings: Seq[PlayBinding[_]]): AbstractModule = {
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
