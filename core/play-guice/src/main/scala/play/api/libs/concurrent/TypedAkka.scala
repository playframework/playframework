/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import scala.language.higherKinds

import java.lang.reflect.ParameterizedType

import scala.reflect.ClassTag
import scala.reflect.classTag

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.annotation.ApiMayChange
import com.google.inject.TypeLiteral
import com.google.inject.util.Types

/** Utility methods related to using Akka's typed API. */
@ApiMayChange
private[play] object TypedAkka {

  /** Equivalent to `new TypeLiteral[ActorRef[T]]() {}`, but with a `ClassTag[T]`. */
  def actorRefOf[T: ClassTag]: TypeLiteral[ActorRef[T]] = typeLiteral(classTag[T].runtimeClass)
  def behaviorOf[T: ClassTag]: TypeLiteral[Behavior[T]] = typeLiteral(classTag[T].runtimeClass)

  /** Equivalent to `new TypeLiteral[ActorRef[T]]() {}`, but with a `Class[T]`. */
  def actorRefOf[T](cls: Class[T]): TypeLiteral[ActorRef[T]] = typeLiteral(cls)
  def behaviorOf[T](cls: Class[T]): TypeLiteral[Behavior[T]] = typeLiteral(cls)

  /** Returns the behavior's message type. Requires the class is a nominal subclass. */
  def messageTypeOf[T](behaviorClass: Class[_ <: Behavior[T]]): Class[T] = {
    val tpe = behaviorClass.getGenericSuperclass.asInstanceOf[ParameterizedType]
    tpe.getActualTypeArguments()(0).asInstanceOf[Class[T]]
  }

  private def typeLiteral[C[_], T](cls: Class[_])(implicit C: ClassTag[C[_]]) = {
    val parameterizedType = Types.newParameterizedType(C.runtimeClass, cls)
    TypeLiteral.get(parameterizedType).asInstanceOf[TypeLiteral[C[T]]]
  }
}
