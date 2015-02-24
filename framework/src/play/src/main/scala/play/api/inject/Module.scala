/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import play.api._
import play.utils.PlayIO
import scala.annotation.varargs
import scala.reflect.ClassTag

/**
 * A Play dependency injection module.
 *
 * Dependency injection modules can be used by Play plugins to provide bindings for JSR-330 compliant
 * ApplicationLoaders.  Any plugin that wants to provide components that a Play application can use may implement
 * one of these.
 *
 * Providing custom modules can be done by creating a resource on the classpath called `play.modules`. This file is
 * expected to contain a list of module classes, one class per line.  For example:
 *
 * {{{
 *   com.example.FooModule
 *   com.example.BarModule
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
   * should be handled in the the constructors and/or providers bound in the returned bindings.  Dependencies on other
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
  @varargs
  final def seq(bindings: Binding[_]*): Seq[Binding[_]] = bindings
}

object Modules {

  /**
   * Locate the modules from the environment.
   *
   * Loads all modules specified by the play.modules.enabled property, minus the modules specified by the
   * play.modules.disabled property.
   *
   * @param environment The environment.
   * @param configuration The configuration.
   * @return A sequence of objects. This method makes no attempt to cast or check the types of the modules being loaded,
   *         allowing ApplicationLoader implementations to reuse the same mechanism to load modules specific to them.
   */
  def locate(environment: Environment, configuration: Configuration): Seq[Any] = {

    val includes = configuration.getStringSeq("play.modules.enabled").getOrElse(Seq.empty)
    val excludes = configuration.getStringSeq("play.modules.disabled").getOrElse(Seq.empty)

    val moduleClassNames = includes.toSet -- excludes

    moduleClassNames.map { className =>
      try {
        environment.classLoader.loadClass(className).newInstance()
      } catch {
        case e: PlayException => throw e
        case e: VirtualMachineError => throw e
        case e: ThreadDeath => throw e
        case e: Throwable => throw new PlayException(
          "Cannot load module",
          "Module [" + className + "] cannot be instantiated.",
          e)
      }
    }.toSeq
  }
}
