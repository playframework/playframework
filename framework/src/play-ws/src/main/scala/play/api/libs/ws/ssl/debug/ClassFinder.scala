/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl.debug

import java.net.{ URLConnection, URL }
import java.util.jar.{ JarEntry, JarInputStream }

/**
 * Loads a set of classes from a package (including ones which are NOT already in the classloader)
 * and return the set that
 */
trait ClassFinder {

  def logger: org.slf4j.Logger

  /**
   * A resource (in a jar file, usually) in the format "/java/lang/String.class".  This returns
   * an initial URL that leads to the JAR file we search for classes.
   */
  def initialResource: String

  /**
   * Returns true if this is a "valid" class, i.e. one we want to return in a set.  Note that all
   * found classes are loaded into the current thread's classloader, even they are not returned.
   *
   * @param className
   * @return true if this class should be returned in the set of findClasses, false otherwise.
   */
  def isValidClass(className: String): Boolean

  def findClasses: Set[Class[_]] = {
    logger.debug(s"findClasses: using initialResource = ${initialResource}")

    val classSet = scala.collection.mutable.Set[Class[_]]()
    val classLoader: ClassLoader = Thread.currentThread.getContextClassLoader

    val urlToSource: URL = this.getClass.getResource(initialResource)
    logger.debug(s"findClasses: urlToSource = ${urlToSource}")

    val parts: Array[String] = urlToSource.toString.split("!")
    val jarURLString: String = parts(0).replace("jar:", "")

    logger.debug(s"findClasses: Loading from ${jarURLString}")

    val jar: URL = new URL(jarURLString)
    val jarConnection: URLConnection = jar.openConnection
    val jis: JarInputStream = new JarInputStream(jarConnection.getInputStream)
    try {
      var je: JarEntry = jis.getNextJarEntry
      while (je != null) {
        if (!je.isDirectory) {
          var className: String = je.getName.substring(0, je.getName.length - 6)
          className = className.replace('/', '.')
          if (isValidClass(className)) {
            //logger.debug(s"findClasses: adding valid class ${className}")

            val c: Class[_] = classLoader.loadClass(className)
            classSet.add(c)
          }
        }
        je = jis.getNextJarEntry
      }
    } finally {
      jis.close()
    }
    classSet.toSet
  }
}
