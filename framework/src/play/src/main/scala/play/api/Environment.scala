/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import java.io.File

trait Environment {
  def configuration: Configuration
  def rootPath: File
  def classLoader: ClassLoader
  def mode: Mode.Mode
}
