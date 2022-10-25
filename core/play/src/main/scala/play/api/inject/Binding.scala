/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject

import java.lang.annotation.Annotation
import java.lang.reflect.Modifier

import javax.inject.Provider
import play.api.PlayException

import scala.language.existentials
import scala.reflect.ClassTag
import play.inject.SourceProvider

/**
 * A binding.
 *
 * Bindings are used to bind classes, optionally qualified by a JSR-330 qualifier annotation, to instances, providers or
 * implementation classes.
 *
 * Bindings may also specify a JSR-330 scope.  If, and only if that scope is [[$javadoc/javax/inject/Singleton javax.inject.Singleton]], then the
 * binding may declare itself to be eagerly instantiated.  In which case, it should be eagerly instantiated when Play
 * starts up.
 *
 * @param key The binding key.
 * @param target The binding target.
 * @param scope The JSR-330 scope.
 * @param eager Whether the binding should be eagerly instantiated.
 * @param source Where this object was bound. Used in error reporting.
 * @see The [[Module]] class for information on how to provide bindings.
 *
 * @define javadoc http://docs.oracle.com/javase/8/docs/api
 */
final case class Binding[T](
    key: BindingKey[T],
    target: Option[BindingTarget[T]],
    scope: Option[Class[_ <: Annotation]],
    eager: Boolean,
    source: Object
) {

  /**
   * Configure the scope for this binding.
   */
  def in[A <: Annotation](scope: Class[A]): Binding[T] = copy(scope = Some(scope))

  /**
   * Configure the scope for this binding.
   */
  def in[A <: Annotation: ClassTag]: Binding[T] =
    in(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]])

  /**
   * Eagerly instantiate this binding when Play starts up.
   */
  def eagerly(): Binding[T] = copy(eager = true)

  override def toString = {
    val eagerDesc = if (eager) " eagerly" else ""
    s"$source:\nBinding($key to ${target.getOrElse("self")}${scope.fold("")(" in " + _)}$eagerDesc)"
  }

  def asJava: play.inject.Binding[T] = new play.inject.Binding[T](this)
}

/**
 * Constructor for a binding Key that doesn't have a qualifier.
 *
 * @see The [[Module]] class for information on how to provide bindings.
 */
object BindingKey {
  def apply[T](clazz: Class[T]): BindingKey[T] = new BindingKey(clazz)
}

/**
 * A binding key.
 *
 * A binding key consists of a class and zero or more JSR-330 qualifiers.
 *
 * @param clazz The class to bind.
 * @param qualifier An optional qualifier.
 * @see The [[Module]] class for information on how to provide bindings.
 */
final case class BindingKey[T](clazz: Class[T], qualifier: Option[QualifierAnnotation]) {
  def this(clazz: Class[T]) = this(clazz, None)

  /**
   * Qualify this binding key with the given instance of an annotation.
   *
   * This can be used to specify bindings with annotations that have particular values.
   */
  def qualifiedWith[A <: Annotation](instance: A): BindingKey[T] =
    BindingKey(clazz, Some(QualifierInstance(instance)))

  /**
   * Qualify this binding key with the given annotation.
   *
   * For example, you may have both a cached implementation, and a direct implementation of a service. To differentiate
   * between them, you may define a Cached annotation:
   *
   * {{{
   *   import scala.annotation._
   *
   *   @target.param
   *   class Cached extends StaticAnnotation
   *
   *   ...
   *
   *   bind[Foo].qualifiedWith(classOf[Cached]).to[FooCached],
   *   bind[Foo].to[FooImpl]
   *
   *   ...
   *
   *   class MyController @Inject() (@Cached foo: Foo) {
   *     ...
   *   }
   * }}}
   *
   * In the above example, the controller will get the cached `Foo` service.
   */
  def qualifiedWith[A <: Annotation](annotation: Class[A]): BindingKey[T] =
    BindingKey(clazz, Some(QualifierClass(annotation)))

  /**
   * Qualify this binding key with the given annotation.
   *
   * For example, you may have both a cached implementation, and a direct implementation of a service. To differentiate
   * between them, you may define a Cached annotation:
   *
   * {{{
   *   import scala.annotation._
   *
   *   @target.param
   *   class Cached extends StaticAnnotation
   *
   *   ...
   *
   *   bind[Foo].qualifiedWith[Cached].to[FooCached],
   *   bind[Foo].to[FooImpl]
   *
   *   ...
   *
   *   class MyController @Inject() (@Cached foo: Foo) {
   *     ...
   *   }
   * }}}
   *
   * In the above example, the controller will get the cached `Foo` service.
   */
  def qualifiedWith[A <: Annotation: ClassTag]: BindingKey[T] =
    qualifiedWith(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]])

  /**
   * Qualify this binding key with the given name.
   *
   * For example, you may have both a cached implementation, and a direct implementation of a service. To differentiate
   * between them, you may decide to name the cached one:
   *
   * {{{
   *   bind[Foo].qualifiedWith("cached").to[FooCached],
   *   bind[Foo].to[FooImpl]
   *
   *   ...
   *
   *   class MyController @Inject() (@Named("cached") foo: Foo) {
   *     ...
   *   }
   * }}}
   *
   * In the above example, the controller will get the cached `Foo` service.
   */
  def qualifiedWith(name: String): BindingKey[T] =
    qualifiedWith(play.inject.Annotations.named(name))

  /**
   * Bind this binding key to the given implementation class.
   *
   * This class will be instantiated and injected by the injection framework.
   */
  def to(implementation: Class[_ <: T]): Binding[T] = {
    Binding(
      this,
      Some(ConstructionTarget(validateTargetNonAbstract(implementation))),
      None,
      false,
      SourceLocator.source
    )
  }

  /**
   * Bind this binding key to the given implementation class.
   *
   * This class will be instantiated and injected by the injection framework.
   */
  def to[C <: T: ClassTag]: Binding[T] =
    to(implicitly[ClassTag[C]].runtimeClass.asInstanceOf[Class[C]])

  /**
   * Bind this binding key to the given provider instance.
   *
   * This provider instance will be invoked to obtain the implementation for the key.
   */
  def to(provider: Provider[_ <: T]): Binding[T] =
    Binding(this, Some(ProviderTarget(provider)), None, false, SourceLocator.source)

  /**
   * Bind this binding key to the given instance.
   */
  def to[A <: T](instance: => A): Binding[T] =
    to(new Provider[A] { def get = instance })

  /**
   * Bind this binding key to another binding key.
   */
  def to(key: BindingKey[_ <: T]): Binding[T] =
    Binding(this, Some(BindingKeyTarget(key)), None, false, SourceLocator.source)

  /**
   * Bind this binding key to the given provider class.
   *
   * The dependency injection framework will instantiate and inject this provider, and then invoke its `get` method
   * whenever an instance of the class is needed.
   */
  def toProvider[P <: Provider[_ <: T]](provider: Class[P]): Binding[T] =
    Binding(
      this,
      Some(ProviderConstructionTarget[T](validateTargetNonAbstract(provider))),
      None,
      false,
      SourceLocator.source
    )

  /**
   * Bind this binding key to the given provider class.
   *
   * The dependency injection framework will instantiate and inject this provider, and then invoke its `get` method
   * whenever an instance of the class is needed.
   */
  def toProvider[P <: Provider[_ <: T]: ClassTag]: Binding[T] =
    toProvider(implicitly[ClassTag[P]].runtimeClass.asInstanceOf[Class[P]])

  /**
   * Bind this binding key to the given instance.
   */
  def toInstance(instance: T): Binding[T] = to(instance)

  /**
   * Bind this binding key to itself.
   */
  def toSelf: Binding[T] = Binding(this, None, None, false, SourceLocator.source)

  override def toString = {
    s"$clazz${qualifier.fold("")(" qualified with " + _)}"
  }

  def asJava: play.inject.BindingKey[T] = new play.inject.BindingKey[T](this)

  private def validateTargetNonAbstract[T](target: Class[T]): Class[T] = {
    if (target.isInterface || Modifier.isAbstract(target.getModifiers)) {
      throw new PlayException(
        "Cannot bind abstract target",
        s"""You have attempted to bind $target as a construction target for $this, however, $target is abstract. If you wish to bind this as an alias, bind it to a ${classOf[
          BindingKey[_]
        ]} instead."""
      )
    }
    target
  }
}

/**
 * A binding target.
 *
 * This trait captures the four possible types of targets.
 *
 * @see The [[Module]] class for information on how to provide bindings.
 */
sealed trait BindingTarget[T] {
  def asJava: play.inject.BindingTarget[T]
}

/**
 * A binding target that is provided by a provider instance.
 *
 * @see The [[Module]] class for information on how to provide bindings.
 */
final case class ProviderTarget[T](provider: Provider[_ <: T]) extends BindingTarget[T] {
  override def asJava: play.inject.ProviderTarget[T] = new play.inject.ProviderTarget[T](this)
}

/**
 * A binding target that is provided by a provider class.
 *
 * @see The [[Module]] class for information on how to provide bindings.
 */
final case class ProviderConstructionTarget[T](provider: Class[_ <: Provider[_ <: T]]) extends BindingTarget[T] {
  override def asJava: play.inject.ProviderConstructionTarget[T] = new play.inject.ProviderConstructionTarget[T](this)
}

/**
 * A binding target that is provided by a class.
 *
 * @see The [[play.api.inject.Module]] class for information on how to provide bindings.
 */
final case class ConstructionTarget[T](implementation: Class[_ <: T]) extends BindingTarget[T] {
  override def asJava: play.inject.ConstructionTarget[T] = new play.inject.ConstructionTarget[T](this)
}

/**
 * A binding target that is provided by another key - essentially an alias.
 */
final case class BindingKeyTarget[T](key: BindingKey[_ <: T]) extends BindingTarget[T] {
  override def asJava: play.inject.BindingKeyTarget[T] = new play.inject.BindingKeyTarget[T](this)
}

/**
 * A qualifier annotation.
 *
 * Since bindings may specify either annotations, or instances of annotations, this abstraction captures either of
 * those two possibilities.
 *
 * @see The [[Module]] class for information on how to provide bindings.
 */
sealed trait QualifierAnnotation {
  def asJava: play.inject.QualifierAnnotation
}

/**
 * A qualifier annotation instance.
 *
 * @see The [[Module]] class for information on how to provide bindings.
 */
final case class QualifierInstance[T <: Annotation](instance: T) extends QualifierAnnotation {
  override def asJava: play.inject.QualifierInstance[T] = new play.inject.QualifierInstance[T](this)
}

/**
 * A qualifier annotation class.
 *
 * @see The [[Module]] class for information on how to provide bindings.
 */
final case class QualifierClass[T <: Annotation](clazz: Class[T]) extends QualifierAnnotation {
  override def asJava: play.inject.QualifierClass[T] = new play.inject.QualifierClass[T](this)
}

private object SourceLocator {
  val provider =
    SourceProvider.DEFAULT_INSTANCE.plusSkippedClasses(this.getClass, classOf[BindingKey[_]], classOf[Binding[_]])

  def source = provider.get()
}
