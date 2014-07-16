/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
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
}
