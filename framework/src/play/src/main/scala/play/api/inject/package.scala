/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import scala.reflect.ClassTag

/**
 * Play's runtime dependency injection abstraction.
 *
 * Play's runtime dependency injection support is built on JSR-330, which provides a specification for declaring how
 * dependencies get wired to components.  JSR-330 however does not address how components are provided to or located
 * by a DI container.  Play's API seeks to address this in a DI container agnostic way.
 *
 * The reason for providing this abstraction is so that Play, the modules it provides, and third party modules can all
 * express their bindings in a way that is not specific to any one DI container.
 *
 * Components are bound in the DI container.  Each binding is identified by a [[play.api.inject.BindingKey BindingKey]], which is
 * typically an interface that the component implements, and may be optionally qualified by a JSR-330 qualifier
 * annotation. A binding key is bound to a [[play.api.inject.BindingTarget BindingTarget]], which describes how the implementation
 * of the interface that the binding key represents is constructed or provided.  Bindings may also be scoped using
 * JSR-330 scope annotations.
 *
 * Bindings are provided by instances of [[play.api.inject.Module Module]].
 *
 * Out of the box, Play provides an implementation of this abstraction using Guice.
 *
 * @see The [[play.api.inject.Module Module]] class for information on how to provide bindings.
 */
package object inject {

  /**
   * Create a binding key for the given class.
   *
   * @see The [[play.api.inject.Module Module]] class for information on how to provide bindings.
   */
  def bind[T](clazz: Class[T]): BindingKey[T] = BindingKey(clazz)

  /**
   * Create a binding key for the given class.
   *
   * @see The [[play.api.inject.Module Module]] class for information on how to provide bindings.
   */
  def bind[T: ClassTag]: BindingKey[T] = BindingKey(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

}
