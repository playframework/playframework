/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import java.lang.annotation.Annotation
import javax.inject.Provider
import scala.language.existentials
import scala.reflect.ClassTag

final case class Binding[T](key: BindingKey[T], target: BindingTarget[T], scope: Option[Class[_ <: Annotation]], eager: Boolean) {
  def in[A <: Annotation](scope: Class[A]): Binding[T] =
    Binding(key, target, Some(scope), eager)

  def in[A <: Annotation: ClassTag]: Binding[T] =
    in(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]])

  def eagerly(): Binding[T] =
    Binding(key, target, scope, true)
}

final case class BindingKey[T](clazz: Class[T], qualifiers: Seq[QualifierAnnotation] = Nil) {
  def qualifiedWith[A <: Annotation](instance: A): BindingKey[T] =
    BindingKey(clazz, qualifiers :+ QualifierInstance(instance))

  def qualifiedWith[A <: Annotation](annotation: Class[A]): BindingKey[T] =
    BindingKey(clazz, qualifiers :+ QualifierClass(annotation))

  def qualifiedWith[A <: Annotation: ClassTag]: BindingKey[T] =
    qualifiedWith(implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]])

  def to(implementation: Class[_ <: T]): Binding[T] =
    Binding(this, ConstructionTarget(implementation), None, false)

  def to[C <: T: ClassTag]: Binding[T] =
    to(implicitly[ClassTag[C]].runtimeClass.asInstanceOf[Class[C]])

  def to(provider: Provider[_ <: T]): Binding[T] =
    Binding(this, ProviderTarget(provider), None, false)

  def to[A <: T](instance: => A): Binding[T] =
    to(new Provider[A] { def get = instance })

  def toProvider[P <: Provider[T]](provider: Class[P]): Binding[T] =
    Binding(this, ProviderConstructionTarget(provider), None, false)

  def toProvider[P <: Provider[T]: ClassTag]: Binding[T] =
    toProvider(implicitly[ClassTag[P]].runtimeClass.asInstanceOf[Class[P]])
}

sealed trait BindingTarget[T]
final case class ProviderTarget[T](provider: Provider[_ <: T]) extends BindingTarget[T]
final case class ProviderConstructionTarget[T](provider: Class[_ <: Provider[T]]) extends BindingTarget[T]
final case class ConstructionTarget[T](implementation: Class[_ <: T]) extends BindingTarget[T]

sealed trait QualifierAnnotation
final case class QualifierInstance[T <: Annotation](instance: T) extends QualifierAnnotation
final case class QualifierClass[T <: Annotation](clazz: Class[T]) extends QualifierAnnotation
