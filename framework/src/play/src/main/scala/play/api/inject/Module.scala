/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject

import java.lang.reflect.Constructor

import play.{ Environment => JavaEnvironment }
import play.api._
import play.libs.reflect.ConstructorUtils

import scala.annotation.varargs
import scala.reflect.ClassTag

/**
 * A Play dependency injection module.
 *
 * Dependency injection modules can be used by Play plugins to provide bindings for JSR-330 compliant
 * ApplicationLoaders.  Any plugin that wants to provide components that a Play application can use may implement
 * one of these.
 *
 * Providing custom modules can be done by appending their fully qualified class names to `play.modules.enabled` in
 * `application.conf`, for example
 *
 * {{{
 *   play.modules.enabled += "com.example.FooModule"
 *   play.modules.enabled += "com.example.BarModule"
 * }}}
 *
 * It is strongly advised that in addition to providing a module for JSR-330 DI, that plugins also provide a Scala
 * trait that constructs the modules manually.  This allows for use of the module without needing a runtime dependency
 * injection provider.
 *
 * The `bind` methods are provided only as a DSL for specifying bindings. For example:
 *
 * {{{
 *   def bindings(env: Environment, conf: Configuration) = Seq(
 *     bind[Foo].to[FooImpl],
 *     bind[Bar].to(new Bar()),
 *     bind[Foo].qualifiedWith[SomeQualifier].to[OtherFoo]
 *   )
 * }}}
 */
abstract class Module {

  /**
   * Get the bindings provided by this module.
   *
   * Implementations are strongly encouraged to do *nothing* in this method other than provide bindings.  Startup
   * should be handled in the constructors and/or providers bound in the returned bindings.  Dependencies on other
   * modules or components should be expressed through constructor arguments.
   *
   * The configuration and environment a provided for the purpose of producing dynamic bindings, for example, if what
   * gets bound depends on some configuration, this may be read to control that.
   *
   * @param environment The environment
   * @param configuration The configuration
   * @return A sequence of bindings
   */
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]]

  /**
   * Create a binding key for the given class.
   */
  @deprecated("Use play.inject.Module.bindClass instead if the Module is coded in Java. Scala modules can use play.api.inject.bind[T: ClassTag]", "2.7.0")
  final def bind[T](clazz: Class[T]): BindingKey[T] = play.api.inject.bind(clazz)

  /**
   * Create a binding key for the given class.
   */
  final def bind[T: ClassTag]: BindingKey[T] = play.api.inject.bind[T]

  /**
   * Create a seq.
   *
   * For Java compatibility.
   */
  @deprecated("Use play.inject.Module instead if the Module is coded in Java.", "2.7.0")
  @varargs
  final def seq(bindings: Binding[_]*): Seq[Binding[_]] = bindings
}

/**
 * A simple Play module, which can be configured by passing a function or a list of bindings.
 */
class SimpleModule(bindingsFunc: (Environment, Configuration) => Seq[Binding[_]]) extends Module {
  def this(bindings: Binding[_]*) = this((_, _) => bindings)

  override final def bindings(environment: Environment, configuration: Configuration) = bindingsFunc(environment, configuration)
}

/**
 * Locates and loads modules from the Play environment.
 */
object Modules {

  private val DefaultModuleName = "Module"

  /**
   * Locate the modules from the environment.
   *
   * Loads all modules specified by the play.modules.enabled property, minus the modules specified by the
   * play.modules.disabled property. If the modules have constructors that take an `Environment` and a
   * `Configuration`, then these constructors are called first; otherwise default constructors are called.
   *
   * @param environment The environment.
   * @param configuration The configuration.
   * @return A sequence of objects. This method makes no attempt to cast or check the types of the modules being loaded,
   *         allowing ApplicationLoader implementations to reuse the same mechanism to load modules specific to them.
   */
  def locate(environment: Environment, configuration: Configuration): Seq[Any] = {

    val includes = configuration.getOptional[Seq[String]]("play.modules.enabled").getOrElse(Seq.empty)
    val excludes = configuration.getOptional[Seq[String]]("play.modules.disabled").getOrElse(Seq.empty)

    val moduleClassNames = includes.toSet -- excludes

    // Construct the default module if it exists
    // Allow users to add "Module" to the excludes to exclude even attempting to look it up
    val defaultModule = if (excludes.contains(DefaultModuleName)) None else try {
      val defaultModuleClass = environment.classLoader.loadClass(DefaultModuleName).asInstanceOf[Class[Any]]
      Some(constructModule(environment, configuration, DefaultModuleName, () => defaultModuleClass))
    } catch {
      case e: ClassNotFoundException => None
    }

    moduleClassNames.map { className =>
      constructModule(environment, configuration, className,
        () => environment.classLoader.loadClass(className).asInstanceOf[Class[Any]])
    }.toSeq ++ defaultModule
  }

  private def constructModule[T](environment: Environment, configuration: Configuration, className: String, loadModuleClass: () => Class[T]): T = {
    try {
      val moduleClass = loadModuleClass()

      def tryConstruct(args: AnyRef*): Option[T] = {
        val constructor: Option[Constructor[T]] = try {
          val argTypes = args.map(_.getClass)
          Option(ConstructorUtils.getMatchingAccessibleConstructor(moduleClass, argTypes: _*))
        } catch {
          case _: NoSuchMethodException => None
          case _: SecurityException => None
        }
        constructor.map(_.newInstance(args: _*))
      }

      {
        tryConstruct(environment, configuration)
      } orElse {
        tryConstruct(new JavaEnvironment(environment), configuration.underlying)
      } orElse {
        tryConstruct()
      } getOrElse {
        throw new PlayException("No valid constructors", "Module [" + className + "] cannot be instantiated.")
      }
    } catch {
      case e: PlayException => throw e
      case e: VirtualMachineError => throw e
      case e: ThreadDeath => throw e
      case e: Throwable => throw new PlayException(
        "Cannot load module",
        "Module [" + className + "] cannot be instantiated.",
        e)
    }
  }
}
