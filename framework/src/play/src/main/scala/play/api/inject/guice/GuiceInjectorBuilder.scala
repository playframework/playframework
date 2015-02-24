/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject
package guice

import com.google.inject.util.{ Modules => GuiceModules, Providers => GuiceProviders }
import com.google.inject.{ CreationException, Guice, Module => GuiceModule }
import java.io.File
import javax.inject.Inject
import play.api.inject.{ Binding => PlayBinding, BindingKey, Injector => PlayInjector, Module => PlayModule }
import play.api.{ Configuration, Environment, Mode, PlayException }
import scala.reflect.ClassTag

class GuiceLoadException(message: String) extends RuntimeException(message)

/**
 * A builder for creating Guice-backed Play Injectors.
 */
abstract class GuiceBuilder[Self] protected (
    environment: Environment,
    configuration: Configuration,
    modules: Seq[GuiceableModule],
    overrides: Seq[GuiceableModule],
    disabled: Seq[Class[_]]) {

  /**
   * Set the environment.
   */
  final def in(env: Environment): Self =
    copyBuilder(environment = env)

  /**
   * Set the environment path.
   */
  final def in(path: File): Self =
    copyBuilder(environment = environment.copy(rootPath = path))

  /**
   * Set the environment mode.
   */
  final def in(mode: Mode.Mode): Self =
    copyBuilder(environment = environment.copy(mode = mode))

  /**
   * Set the environment class loader.
   */
  final def in(classLoader: ClassLoader): Self =
    copyBuilder(environment = environment.copy(classLoader = classLoader))

  /**
   * Add additional configuration.
   */
  final def configure(conf: Configuration): Self =
    copyBuilder(configuration = configuration ++ conf)

  /**
   * Add additional configuration.
   */
  final def configure(conf: Map[String, Any]): Self =
    configure(Configuration.from(conf))

  /**
   * Add additional configuration.
   */
  final def configure(conf: (String, Any)*): Self =
    configure(conf.toMap)

  /**
   * Add Guice modules, Play modules, or Play bindings.
   *
   * @see [[GuiceableModuleConversions]] for the automatically available implicit
   *      conversions to [[GuiceableModule]] from modules and bindings.
   */
  final def bindings(bindModules: GuiceableModule*): Self =
    copyBuilder(modules = modules ++ bindModules)

  /**
   * Override bindings using Guice modules, Play modules, or Play bindings.
   *
   * @see [[GuiceableModuleConversions]] for the automatically available implicit
   *      conversions to [[GuiceableModule]] from modules and bindings.
   */
  final def overrides(overrideModules: GuiceableModule*): Self =
    copyBuilder(overrides = overrides ++ overrideModules)

  /**
   * Disable modules by class.
   */
  final def disable(moduleClasses: Class[_]*): Self =
    copyBuilder(disabled = disabled ++ moduleClasses)

  /**
   * Disable module by class.
   */
  final def disable[T](implicit tag: ClassTag[T]): Self = disable(tag.runtimeClass)

  /**
   * Create a Play Injector backed by Guice using this configured builder.
   */
  def injector(): PlayInjector = createInjector

  /**
   * Internal creation of the injector.
   * Separate method in case builders need to add extra configuration to injector before building.
   */
  protected final def createInjector(): PlayInjector = {
    import scala.collection.JavaConverters._
    try {
      val injectorModule = GuiceableModule.guice(Seq(
        bind[PlayInjector].to[GuiceInjector],
        // Java API injector is bound here so that it's available in both
        // the default application loader and the Java Guice builders
        bind[play.inject.Injector].to[play.inject.DelegateInjector]
      ))
      val enabledModules = modules.map(_.disable(disabled))
      val bindingModules = GuiceableModule.guiced(environment, configuration)(enabledModules) :+ injectorModule
      val overrideModules = GuiceableModule.guiced(environment, configuration)(overrides)
      val guiceInjector = Guice.createInjector(GuiceModules.`override`(bindingModules.asJava).`with`(overrideModules.asJava))
      guiceInjector.getInstance(classOf[PlayInjector])
    } catch {
      case e: CreationException => e.getCause match {
        case p: PlayException => throw p
        case _ => throw e
      }
    }
  }

  /**
   * Internal copy method with defaults.
   */
  private def copyBuilder(
    environment: Environment = environment,
    configuration: Configuration = configuration,
    modules: Seq[GuiceableModule] = modules,
    overrides: Seq[GuiceableModule] = overrides,
    disabled: Seq[Class[_]] = disabled): Self =
    newBuilder(environment, configuration, modules, overrides, disabled)

  /**
   * Create a new Self for this immutable builder.
   * Provided by builder implementations.
   */
  protected def newBuilder(
    environment: Environment,
    configuration: Configuration,
    modules: Seq[GuiceableModule],
    overrides: Seq[GuiceableModule],
    disabled: Seq[Class[_]]): Self

}

/**
 * Default empty builder for creating Guice-backed Injectors.
 */
final class GuiceInjectorBuilder(
  environment: Environment = Environment.simple(),
  configuration: Configuration = Configuration.empty,
  modules: Seq[GuiceableModule] = Seq.empty,
  overrides: Seq[GuiceableModule] = Seq.empty,
  disabled: Seq[Class[_]] = Seq.empty) extends GuiceBuilder[GuiceInjectorBuilder](
  environment, configuration, modules, overrides, disabled
) {

  // extra constructor for creating from Java
  def this() = this(environment = Environment.simple())

  /**
   * Create a Play Injector backed by Guice using this configured builder.
   */
  def build(): PlayInjector = injector()

  protected def newBuilder(
    environment: Environment,
    configuration: Configuration,
    modules: Seq[GuiceableModule],
    overrides: Seq[GuiceableModule],
    disabled: Seq[Class[_]]): GuiceInjectorBuilder =
    new GuiceInjectorBuilder(environment, configuration, modules, overrides, disabled)
}

/**
 * Magnet pattern for creating Guice modules from Play modules or bindings.
 */
trait GuiceableModule {
  def guiced(env: Environment, conf: Configuration): Seq[GuiceModule]
  def disable(classes: Seq[Class[_]]): GuiceableModule
}

/**
 * Loading and converting Guice modules.
 */
object GuiceableModule extends GuiceableModuleConversions {

  def loadModules(environment: Environment, configuration: Configuration): Seq[GuiceableModule] = {
    Modules.locate(environment, configuration) map guiceable
  }

  /**
   * Attempt to convert a module of unknown type to a GuiceableModule.
   */
  def guiceable(module: Any): GuiceableModule = module match {
    case playModule: PlayModule => fromPlayModule(playModule)
    case guiceModule: GuiceModule => fromGuiceModule(guiceModule)
    case unknown => throw new PlayException(
      "Unknown module type",
      s"Module [$unknown] is not a Play module or a Guice module"
    )
  }

  /**
   * Apply GuiceableModules to create Guice modules.
   */
  def guiced(env: Environment, conf: Configuration)(builders: Seq[GuiceableModule]): Seq[GuiceModule] =
    builders flatMap { module => module.guiced(env, conf) }

}

/**
 * Implicit conversions to GuiceableModules.
 */
trait GuiceableModuleConversions {

  import scala.language.implicitConversions

  implicit def fromGuiceModule(guiceModule: GuiceModule): GuiceableModule = fromGuiceModules(Seq(guiceModule))

  implicit def fromGuiceModules(guiceModules: Seq[GuiceModule]): GuiceableModule = new GuiceableModule {
    def guiced(env: Environment, conf: Configuration): Seq[GuiceModule] = guiceModules
    def disable(classes: Seq[Class[_]]): GuiceableModule = fromGuiceModules(filterOut(classes, guiceModules))
    override def toString = s"GuiceableModule(${guiceModules.mkString(", ")})"
  }

  implicit def fromPlayModule(playModule: PlayModule): GuiceableModule = fromPlayModules(Seq(playModule))

  implicit def fromPlayModules(playModules: Seq[PlayModule]): GuiceableModule = new GuiceableModule {
    def guiced(env: Environment, conf: Configuration): Seq[GuiceModule] = playModules.map(guice(env, conf))
    def disable(classes: Seq[Class[_]]): GuiceableModule = fromPlayModules(filterOut(classes, playModules))
    override def toString = s"GuiceableModule(${playModules.mkString(", ")})"
  }

  implicit def fromPlayBinding(binding: PlayBinding[_]): GuiceableModule = fromPlayBindings(Seq(binding))

  implicit def fromPlayBindings(bindings: Seq[PlayBinding[_]]): GuiceableModule = new GuiceableModule {
    def guiced(env: Environment, conf: Configuration): Seq[GuiceModule] = Seq(guice(bindings))
    def disable(classes: Seq[Class[_]]): GuiceableModule = this // no filtering
    override def toString = s"GuiceableModule(${bindings.mkString(", ")})"
  }

  private def filterOut[A](classes: Seq[Class[_]], instances: Seq[A]): Seq[A] =
    instances.filterNot(o => classes.exists(_.isAssignableFrom(o.getClass)))

  /**
   * Convert the given Play module to a Guice module.
   */
  def guice(env: Environment, conf: Configuration)(module: PlayModule): GuiceModule =
    guice(module.bindings(env, conf))

  /**
   * Convert the given Play bindings to a Guice module.
   */
  def guice(bindings: Seq[PlayBinding[_]]): GuiceModule = {
    new com.google.inject.AbstractModule {
      def configure(): Unit = {
        for (b <- bindings) {
          val binding = b.asInstanceOf[PlayBinding[Any]]
          val builder = binder().withSource(binding).bind(GuiceKey(binding.key))
          binding.target.foreach {
            case ProviderTarget(provider) => builder.toProvider(GuiceProviders.guicify(provider))
            case ProviderConstructionTarget(provider) => builder.toProvider(provider)
            case ConstructionTarget(implementation) => builder.to(implementation)
            case BindingKeyTarget(key) => builder.to(GuiceKey(key))
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

/**
 * Conversion from Play BindingKey to Guice Key.
 */
object GuiceKey {
  import com.google.inject.Key

  def apply[T](key: BindingKey[T]): Key[T] = {
    key.qualifier match {
      case Some(QualifierInstance(instance)) => Key.get(key.clazz, instance)
      case Some(QualifierClass(clazz)) => Key.get(key.clazz, clazz)
      case None => Key.get(key.clazz)
    }
  }
}

/**
 * Play Injector backed by a Guice Injector.
 */
class GuiceInjector @Inject() (injector: com.google.inject.Injector) extends PlayInjector {
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
  def instanceOf[T](key: BindingKey[T]) = injector.getInstance(GuiceKey(key))
}
