/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import java.io.File
import play.utils.Threads

case class Environment(
  configuration: Configuration,
  rootPath: File,
  classLoader: ClassLoader,
  mode: Mode.Mode
)

object Environment {
  def apply(rootPath: File, classLoader: ClassLoader, mode: Mode.Mode): Environment = {
    val configuration = Threads.withContextClassLoader(classLoader) {
      Configuration.load(rootPath, mode)
    }
    Environment(configuration, rootPath, classLoader, mode)
  }
}
