/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import java.lang.invoke.MethodHandle

import akka.actor.typed.Behavior
import akka.annotation.ApiMayChange
import com.google.inject.{ Inject, Injector, Provider }
import javax.inject.Singleton

import scala.reflect.ClassTag

@Singleton
@ApiMayChange
final class BehaviorProvider[T: ClassTag](val methodHandle: MethodHandle) extends Provider[Behavior[T]] {
  @Inject private val guiceInjector: Injector  = null

  lazy val get = {
    val parameters = methodHandle
      .`type`()
      .parameterArray()
      .map { clazz =>
        guiceInjector.getInstance(clazz)
      }
    methodHandle.invokeWithArguments(parameters:_*).asInstanceOf[Behavior[T]]
  }
}
