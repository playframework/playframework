/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

import scala.reflect.ClassTag

object Common {

  def assertNotEmpty[T: ClassTag](o: java.util.Optional[T]): T = {
    if (o.isPresent) o.get()
    else throw new Exception("Expected Some[" + implicitly[ClassTag[T]] + "]")
  }

}