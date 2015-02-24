/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import scala.reflect.ClassTag

package object inject {

  /**
   * Create a binding key for the given class.
   */
  def bind[T](clazz: Class[T]): BindingKey[T] = BindingKey(clazz)

  /**
   * Create a binding key for the given class.
   */
  def bind[T: ClassTag]: BindingKey[T] = BindingKey(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]])

}
