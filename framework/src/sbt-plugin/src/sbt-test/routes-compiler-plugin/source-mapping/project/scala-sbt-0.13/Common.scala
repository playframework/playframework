/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

import scala.reflect.ClassTag

object Common {

  def assertNotEmpty[T: ClassTag](m: xsbti.Maybe[T]): T = {
    if (m.isEmpty) throw new Exception("Expected Some[" + implicitly[ClassTag[T]] + "]")
    else m.get()
  }

}