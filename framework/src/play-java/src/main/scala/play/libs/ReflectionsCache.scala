/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs

import org.reflections.util.FilterBuilder

/**
 * Provides a cache for reflections, so that classloader scanning over the same classloader for the same package
 * multiple times doesn't need to be done.
 *
 * This is most useful in tests, when each test starts a new FakeApplication, and so things like Ebean scan the
 * classloader for @Entity annotated classes in a given package.  Profiling shows that without this cache, over 90%
 * of a tests time might be spent in classpath scanning.
 */
object ReflectionsCache {
  import ref.SoftReference
  import org.reflections.{ scanners, util, Reflections }
  import scala.collection.concurrent._

  // A soft reference is used so that we don't force the classloader or reflections to be live after a test run,
  // but we don't use weak reference as this is the only reference to the tuple, and it will just always get collected
  // on each eden space collection if it was weak.
  @volatile private var reflectionsMapRef: Option[SoftReference[(ClassLoader, Map[String, Reflections])]] = None

  def getReflections(classLoader: ClassLoader, pkg: String) = {
    // Detect if the classloader is different from last time, if it is, create a new cache and replace the old
    val reflectionsMap = reflectionsMapRef.flatMap(_.get).filter(_._1 == classLoader).map(_._2).getOrElse {
      val map = TrieMap.empty[String, Reflections]
      reflectionsMapRef = Some(new SoftReference((classLoader, map), null))
      map
    }
    reflectionsMap.get(pkg).getOrElse {

      val reflections = new Reflections(new util.ConfigurationBuilder()
        .addUrls(util.ClasspathHelper.forPackage(pkg, classLoader))
        .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(pkg + ".")))
        .setScanners(new scanners.TypeAnnotationsScanner, new scanners.TypeElementsScanner))

      reflectionsMap.putIfAbsent(pkg, reflections).getOrElse(reflections)
    }
  }
}
