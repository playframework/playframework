/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.lang.reflect.ParameterizedType

import scala.reflect.classTag
import scala.reflect.ClassTag

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.annotation.ApiMayChange
import com.google.inject.util.Types
import com.google.inject.TypeLiteral

/** Utility methods related to using Pekko's typed API. */
@ApiMayChange
private[play] object TypedPekko {

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

  private def typeLiteral[C[_], T](cls: Class[_])(implicit C: ClassTag[C[Any]]) = {
    val parameterizedType = Types.newParameterizedType(C.runtimeClass, cls)
    TypeLiteral.get(parameterizedType).asInstanceOf[TypeLiteral[C[T]]]
  }
}
