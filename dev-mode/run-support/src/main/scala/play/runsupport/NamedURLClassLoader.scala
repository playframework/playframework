/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport

import java.net.{ URL, URLClassLoader }

/**
 * A ClassLoader with a toString() that prints name/urls.
 */
class NamedURLClassLoader(name: String, urls: Array[URL], parent: ClassLoader) extends URLClassLoader(urls, parent) {
  override def toString = name + "{" + getURLs.map(_.toString).mkString(", ") + "}"
}
