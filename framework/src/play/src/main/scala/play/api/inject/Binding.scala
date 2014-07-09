/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import java.lang.annotation.Annotation
import javax.inject.Provider
import scala.language.existentials

final case class Binding[T](key: BindingKey[T], target: BindingTarget[T], scope: Option[Class[_ <: Annotation]], eager: Boolean)

final case class BindingKey[T](clazz: Class[T], qualifiers: Seq[QualifierAnnotation])

sealed trait BindingTarget[T]
final case class ProviderTarget[T](provider: Provider[_ <: T]) extends BindingTarget[T]
final case class ProviderConstructionTarget[T](provider: Class[_ <: Provider[T]]) extends BindingTarget[T]
final case class ConstructionTarget[T](implementation: Class[_ <: T]) extends BindingTarget[T]
//final case class AliasBinding(toKey: BindingKey) extends Binding

sealed trait QualifierAnnotation
final case class QualifierInstance[T <: Annotation](instance: T) extends QualifierAnnotation
final case class QualifierClass[T <: Annotation](clazz: Class[T]) extends QualifierAnnotation
