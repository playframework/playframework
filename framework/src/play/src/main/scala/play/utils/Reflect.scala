/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import play.api.{ PlayConfig, Environment, PlayException }
import play.api.inject.{ BindingKey, Binding }
import scala.reflect.ClassTag

object Reflect {

  /**
   * Lookup the given key from the given configuration, and provide bindings for the ScalaTrait to a class by that key.
   *
   * The end goal is to provide a binding for `ScalaTrait`.  The logic for finding the implementation goes like this:
   *
   * - If the value of the configured key is `provided`, this indicates the user will provide their own binding, so
   *   return nothing.
   * - If the value of the configured key is a class that exists, then use that
   * - If the value of the configured key is not a class that exists, fail
   * - Otherwise if no configuration value is found for key, then if there is a class found with name `defaultClassName`, use that
   * - Otherwise, use the class `Default`
   *
   * If a class has been located, convert that to a binding, by the following rules:
   *
   * - If it's a subclass of `ScalaTrait` bind it directly
   * - Otherwise, if it's a subclass of `JavaInterface`, bind that to `JavaInterface`, and then also return a binding
   *   of `JavaAdapter` to `ScalaTrait`
   * - Otherwise, fail
   *
   * @param environment The environment to load classes from
   * @param config The configuration
   * @param key The key to look up the classname from the configuration
   * @tparam ScalaTrait The trait to bind
   * @tparam JavaInterface The Java interface for Java versions of the implementation
   * @tparam JavaAdapter An adapter class that depends on `JavaInterface` and provides `ScalaTrait`
   * @tparam JavaDelegate An implementation of `JavaInterface` that delegates to `ScalaTrait`, for when the configured
   *                      class is not an instance of `JavaInterface`.
   * @tparam Default The default implementation of `ScalaTrait` if no user implementation has been provided
   * @return Zero or more bindings to provide `ScalaTrait`
   */
  def bindingsFromConfiguration[ScalaTrait, JavaInterface, JavaAdapter <: ScalaTrait, JavaDelegate <: JavaInterface, Default <: ScalaTrait](
    environment: Environment, config: PlayConfig, key: String, defaultClassName: String)(implicit scalaTrait: SubClassOf[ScalaTrait],
      javaInterface: SubClassOf[JavaInterface], javaAdapter: ClassTag[JavaAdapter], javaDelegate: ClassTag[JavaDelegate], default: ClassTag[Default]): Seq[Binding[_]] = {

    def bind[T: SubClassOf]: BindingKey[T] = BindingKey(implicitly[SubClassOf[T]].runtimeClass)

    configuredClass[ScalaTrait, JavaInterface, Default](environment, config, key, defaultClassName) match {

      // Directly implements the scala trait
      case Some(Left(direct)) =>
        Seq(
          bind[ScalaTrait].to(direct),
          bind[JavaInterface].to[JavaDelegate]
        )
      // Implements the java interface
      case Some(Right(java)) =>
        Seq(
          bind[ScalaTrait].to[JavaAdapter],
          bind[JavaInterface].to(java)
        )

      case None => Nil
    }
  }

  /**
   * Lookup the given key from the given configuration, and load it either as an instance of ScalaTrait, or JavaInterface.
   *
   * If no user provided class can be found, then return Default.
   *
   * - If the value of the configured key is `provided`, this indicates the user will provide their own binding, so
   *   return None.
   * - If the value of the configured key is a class that exists, then use that
   * - If the value of the configured key is not a class that exists, fail
   * - Otherwise if no configuration value is found for key, then if there is a class found with name `defaultClassName`, use that
   * - Otherwise, use the class `Default`
   *
   * If a class has been located, then return Some, according to the following rules:
   *
   * - If it's a subclass of `ScalaTrait` return that as Left
   * - Otherwise, if it's a subclass of `JavaInterface`, return that as Right
   * - Otherwise, fail
   *
   * @param environment The environment to load classes from
   * @param config The configuration
   * @param key The key to look up the classname from the configuration
   * @tparam ScalaTrait The Scala trait to return
   * @tparam JavaInterface The Java interface for Java versions of the implementation
   * @tparam Default The default implementation of `ScalaTrait` if no user implementation has been provided
   */
  def configuredClass[ScalaTrait, JavaInterface, Default <: ScalaTrait](
    environment: Environment, config: PlayConfig, key: String, defaultClassName: String)(implicit scalaTrait: SubClassOf[ScalaTrait],
      javaInterface: SubClassOf[JavaInterface], default: ClassTag[Default]): Option[Either[Class[_ <: ScalaTrait], Class[_ <: JavaInterface]]] = {

    def loadClass(className: String, notFoundFatal: Boolean): Option[Class[_]] = {
      try {
        Some(environment.classLoader.loadClass(className))
      } catch {
        case e: ClassNotFoundException if !notFoundFatal => None
        case e: VirtualMachineError => throw e
        case e: ThreadDeath => throw e
        case e: Throwable =>
          throw new PlayException(s"Cannot load $key", s"$key [$className] was not loaded.", e)
      }
    }

    val maybeClass = config.get[Option[String]](key) match {
      // If provided, don't bind anything
      case Some("provided") => None
      // If empty, use the default
      case None =>
        // If no value, load the default class name, but if it's not found, then fallback to the default class
        loadClass(defaultClassName, notFoundFatal = false)
          .orElse(Some(default.runtimeClass))
      // If a value, load that class
      case Some(className) => loadClass(className, notFoundFatal = true)
    }

    maybeClass.map {

      // Directly implements the scala trait
      case scalaTrait(scalaClass) =>
        Left(scalaClass)
      // Implements the java interface
      case javaInterface(java) =>
        Right(java)

      case unknown =>
        throw new PlayException(s"Cannot load $key", s"$key [${unknown.getClass}}] does not implement ${scalaTrait.runtimeClass} or ${javaInterface.runtimeClass}.")
    }
  }

  def createInstance[T: ClassTag](fqcn: String, classLoader: ClassLoader): T = {
    try {
      createInstance(getClass(fqcn, classLoader))
    } catch {
      case e: VirtualMachineError => throw e
      case e: ThreadDeath => throw e
      case e: Throwable =>
        val name = simpleName(implicitly[ClassTag[T]].runtimeClass)
        throw new PlayException(s"Cannot load $name", s"$name [$fqcn] cannot be instantiated.", e)
    }
  }

  def getClass[T: ClassTag](fqcn: String, classLoader: ClassLoader): Class[_ <: T] = {
    val c = Class.forName(fqcn, false, classLoader).asInstanceOf[Class[_ <: T]]
    val t = implicitly[ClassTag[T]].runtimeClass
    if (t.isAssignableFrom(c)) c
    else throw new ClassCastException(t + " is not assignable from " + c)
  }

  def createInstance[T: ClassTag](clazz: Class[_]): T = {
    val o = clazz.newInstance
    val t = implicitly[ClassTag[T]].runtimeClass
    if (t.isInstance(o)) o.asInstanceOf[T]
    else throw new ClassCastException(clazz.getName + " is not an instance of " + t)
  }

  def simpleName(clazz: Class[_]): String = {
    val name = clazz.getName
    name.substring(name.lastIndexOf('.') + 1)
  }

  class SubClassOf[T](val runtimeClass: Class[T]) {
    def unapply(clazz: Class[_]): Option[Class[_ <: T]] = {
      if (runtimeClass.isAssignableFrom(clazz)) {
        Some(clazz.asInstanceOf[Class[_ <: T]])
      } else {
        None
      }
    }
  }

  object SubClassOf {
    implicit def provide[T: ClassTag]: SubClassOf[T] =
      new SubClassOf[T](implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])
  }
}
