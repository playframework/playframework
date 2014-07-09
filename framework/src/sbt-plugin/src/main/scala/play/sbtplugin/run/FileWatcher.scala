/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbtplugin.run

import java.io.{ IOException, File }

import sbt.{ WatchState, SourceModificationWatch, IO }
import sbt.Path._

import scala.util.control.NonFatal

/**
 * A service that can watch files
 */
trait PlayWatchService {
  /**
   * Watch the given sequence of files or directories.
   *
   * @param filesToWatch The files to watch.
   * @param onChange A callback that is executed whenever something changes.
   * @return A watcher
   */
  def watch(filesToWatch: Seq[File], onChange: () => Unit): PlayWatcher
}

/**
 * A watcher, that watches files.
 */
trait PlayWatcher {

  /**
   * Stop watching the files.
   */
  def stop(): Unit
}

object PlayWatchService {
  def apply(targetDirectory: File): PlayWatchService = {
    JNotifyPlayWatchService(targetDirectory).getOrElse(new SbtPlayWatchService(500))
  }
}

/**
 * A polling Play watch service.  Polls in the background.
 */
private class SbtPlayWatchService(pollDelayMillis: Int) extends PlayWatchService {

  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {

    @volatile var stopped = false

    val thread = new Thread(new Runnable {
      def run() = {
        var state = WatchState.empty
        while (!stopped) {
          val (triggered, newState) = SourceModificationWatch.watch(filesToWatch.***.distinct, pollDelayMillis,
            state)(stopped)
          if (triggered) onChange()
          state = newState
        }
      }
    }, "sbt-play-watch-service")
    thread.setDaemon(true)
    thread.start()

    new PlayWatcher {
      def stop() = stopped = true
    }
  }
}

private class JNotifyPlayWatchService(delegate: JNotifyPlayWatchService.JNotifyDelegate) extends PlayWatchService {
  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    val listener = delegate.newListener(onChange)
    val registeredIds = filesToWatch.map { file =>
      delegate.addWatch(file.getAbsolutePath, listener)
    }
    new PlayWatcher {
      def stop() = registeredIds.foreach(delegate.removeWatch)
    }
  }
}

private object JNotifyPlayWatchService {

  import java.lang.reflect.{ Method, InvocationHandler, Proxy }

  /**
   * Captures all the reflection invocations in one place.
   */
  class JNotifyDelegate(classLoader: ClassLoader, listenerClass: Class[_], addWatchMethod: Method, removeWatchMethod: Method) {
    def addWatch(fileOrDirectory: String, listener: AnyRef): Int = {
      addWatchMethod.invoke(null,
        fileOrDirectory, // The file or directory to watch
        15: java.lang.Integer, // flags to say watch for all events
        true: java.lang.Boolean, // Watch subtree
        listener).asInstanceOf[Int]
    }
    def removeWatch(id: Int): Unit = {
      try {
        removeWatchMethod.invoke(null, id.asInstanceOf[AnyRef])
      } catch {
        case _: Throwable =>
        // Ignore, if we fail to remove a watch it's not the end of the world.
        // http://sourceforge.net/p/jnotify/bugs/12/
        // We match on Throwable because matching on an IOException didn't work.
        // http://sourceforge.net/p/jnotify/bugs/5/
      }
    }
    def newListener(onChange: () => Unit): AnyRef = {
      Proxy.newProxyInstance(classLoader, Seq(listenerClass).toArray, new InvocationHandler {
        def invoke(proxy: AnyRef, m: Method, args: Array[AnyRef]): AnyRef = {
          onChange()
          null
        }
      })
    }
  }

  // Try state - null means no attempt to load yet, None means failed load, Some means successful load
  @volatile var watchService: Option[JNotifyPlayWatchService] = null

  def apply(targetDirectory: File): Option[PlayWatchService] = {

    try {
      watchService match {
        case null =>
          watchService = None
          val jnotifyJarFile = this.getClass.getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs
            .map(_.getFile)
            .find(_.contains("/jnotify"))
            .map(new File(_))
            .getOrElse(sys.error("Missing JNotify?"))

          val sbtLoader = this.getClass.getClassLoader.getParent.asInstanceOf[java.net.URLClassLoader]
          val method = classOf[java.net.URLClassLoader].getDeclaredMethod("addURL", classOf[java.net.URL])
          method.setAccessible(true)
          method.invoke(sbtLoader, jnotifyJarFile.toURI.toURL)

          val nativeLibrariesDirectory = new File(targetDirectory, "native_libraries")

          if (!nativeLibrariesDirectory.exists) {
            // Unzip native libraries from the jnotify jar to target/native_libraries
            IO.unzip(jnotifyJarFile, targetDirectory, (name: String) => name.startsWith("native_libraries"))
          }

          val libs = new File(nativeLibrariesDirectory, System.getProperty("sun.arch.data.model") + "bits").getAbsolutePath

          // Hack to set java.library.path
          System.setProperty("java.library.path", {
            Option(System.getProperty("java.library.path")).map { existing =>
              existing + java.io.File.pathSeparator + libs
            }.getOrElse(libs)
          })
          val fieldSysPath = classOf[ClassLoader].getDeclaredField("sys_paths")
          fieldSysPath.setAccessible(true)
          fieldSysPath.set(null, null)

          val jnotifyClass = sbtLoader.loadClass("net.contentobjects.jnotify.JNotify")
          val jnotifyListenerClass = sbtLoader.loadClass("net.contentobjects.jnotify.JNotifyListener")
          val addWatchMethod = jnotifyClass.getMethod("addWatch", classOf[String], classOf[Int], classOf[Boolean], jnotifyListenerClass)
          val removeWatchMethod = jnotifyClass.getMethod("removeWatch", classOf[Int])

          val d = new JNotifyDelegate(sbtLoader, jnotifyListenerClass, addWatchMethod, removeWatchMethod)

          // Try it
          d.removeWatch(0)

          watchService = Some(new JNotifyPlayWatchService(d))

        case other => other
      }

      watchService

    } catch {
      case NonFatal(e) =>
        reportError(e)
        None
      // JNotify failure on FreeBSD
      case e: ExceptionInInitializerError =>
        reportError(e)
        None
      case e: NoClassDefFoundError =>
        reportError(e)
        None
      // JNotify failure on Linux
      case e: UnsatisfiedLinkError =>
        reportError(e)
        None
    }

  }

  private def reportError(e: Throwable) = {
    println(play.sbtplugin.Colors.red(
      """|
         |Cannot load the JNotify native library (%s)
         |Play will poll for file changes, so expect increased system load.
         |""".format(e.getMessage).stripMargin
    ))
  }
}

