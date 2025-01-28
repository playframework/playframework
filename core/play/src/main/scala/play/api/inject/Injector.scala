/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
 * they are using directly, for example, if using Guice, use
 * [[http://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Injector.html com.google.inject.Injector]]
 * instead of this.
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

  /**
   * Get as an instance of the Java injector.
   */
  def asJava: play.inject.Injector = new play.inject.DelegateInjector(this)
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
  def instanceOf[T](clazz: Class[T]) = clazz.getDeclaredConstructor().newInstance()

  /**
   * Get an instance bound to the given binding key.
   */
  def instanceOf[T](key: BindingKey[T]) = instanceOf(key.clazz)
}

/**
 * Wraps an existing injector, ensuring all calls have the correct context `ClassLoader` set.
 */
private[play] class ContextClassLoaderInjector(delegate: Injector, classLoader: ClassLoader) extends Injector {
  override def instanceOf[T: ClassTag]: T           = withContext { delegate.instanceOf[T] }
  override def instanceOf[T](clazz: Class[T]): T    = withContext { delegate.instanceOf(clazz) }
  override def instanceOf[T](key: BindingKey[T]): T = withContext { delegate.instanceOf(key) }

  @inline
  private def withContext[T](body: => T): T = {
    val thread         = Thread.currentThread()
    val oldClassLoader = thread.getContextClassLoader
    thread.setContextClassLoader(classLoader)
    try body
    finally thread.setContextClassLoader(oldClassLoader)
  }
}
