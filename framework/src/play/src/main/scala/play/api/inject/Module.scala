/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import play.api.{ Configuration, Environment }
import scala.reflect.ClassTag

/**
 * A Play dependency injection module.
 *
 * Dependency injection modules can be used by Play plugins to provide bindings for JSR-330 compliant
 * ApplicationLoaders.  Any plugin that wants to provide components that a Play application can use may implement
 * one of these.
 *
 * Providing custom modules can be done by... todo
 *
 * It is strongly advised that in addition to providing a module for JSR-330 DI, that plugins also provide a Scala
 * trait that constructs the modules manually.  This allows for use of the module without needing a runtime dependency
 * injection provider.
 */
abstract class Module {

  def bindings(env: Environment, configuration: Configuration): Seq[Binding[_]]

  def bind[T](clazz: Class[T]): BindingKey[T] = BindingKey(clazz, Seq.empty)

  def bind[T: ClassTag]: BindingKey[T] = BindingKey(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Seq.empty)
}
