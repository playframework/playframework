/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import play.api.Environment
import scala.reflect.ClassTag

abstract class Module {
  def bindings(env: Environment): Seq[Binding[_]]

  def bind[T](clazz: Class[T]): BindingKey[T] = BindingKey(clazz, Seq.empty)

  def bind[T: ClassTag]: BindingKey[T] = BindingKey(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]], Seq.empty)
}
