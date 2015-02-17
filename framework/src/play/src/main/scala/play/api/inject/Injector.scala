/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import scala.reflect.ClassTag

/**
 * An injector, capable of providing components.
 *
 * This is an abstraction over whatever dependency injection is being used in Play. A minimal implementation may only
 * call `newInstance` on the passed in class.
 *
 * This abstraction is primarily provided for libraries that want to remain agnostic to the type of dependency
 * injection being used. End users are encouraged to use the facilities provided by the dependency injection framework
 * they are using directly, for example, if using Guice, use [[com.google.inject.Injector]] instead of this.
 */
trait Injector {

  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T: ClassTag]: T

  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T](clazz: Class[T]): T

  /**
   * Get an instance bound to the given binding key.
   */
  def instanceOf[T](key: BindingKey[T]): T
}

/**
 * An injector that simply creates a new instance of the passed in classes using the classes no-arg constructor.
 */
object NewInstanceInjector extends Injector {

  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T](implicit ct: ClassTag[T]) = instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T](clazz: Class[T]) = clazz.newInstance()

  /**
   * Get an instance bound to the given binding key.
   */
  def instanceOf[T](key: BindingKey[T]) = instanceOf(key.clazz)
}

/**
 * A simple map backed injector.
 *
 * This injector is intended for use in the transitional period between when Play fully supports dependency injection
 * across the whole code base, and when some parts of Play still access core components through Play's global state.
 *
 * It is intended to just hold built in Play components, but may be used to add additional components by end users when
 * required.
 *
 * @param fallback The injector to fallback to if no component can be found.
 * @param components The components that this injector provides.
 */
class SimpleInjector(fallback: Injector, components: Map[Class[_], Any] = Map.empty) extends Injector {
  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T](implicit ct: ClassTag[T]) = instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  /**
   * Get an instance of the given class from the injector.
   */
  def instanceOf[T](clazz: Class[T]) = components.getOrElse(clazz, fallback.instanceOf(clazz)).asInstanceOf[T]

  /**
   * Get an instance bound to the given binding key.
   */
  def instanceOf[T](key: BindingKey[T]) = instanceOf(key.clazz)

  /**
   * Add a component to the injector.
   */
  def +[T](component: T)(implicit ct: ClassTag[T]) =
    new SimpleInjector(fallback, components + (ct.runtimeClass -> component))
}
