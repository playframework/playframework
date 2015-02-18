/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.runsupport

import java.net.URLClassLoader
import java.util.List
import java.util.Locale

import sbt.{ SourceModificationWatch, WatchState }
import sbt.{ IO, DirectoryFilter, HiddenFileFilter }
import sbt.Path._

import scala.collection.JavaConversions
import scala.reflect.ClassTag
import scala.util.{ Properties, Try }
import scala.util.control.NonFatal
import java.io.File
import java.util.concurrent.Callable

/**
 * A service that can watch files
 */
trait FileWatchService {
  /**
   * Watch the given sequence of files or directories.
   *
   * @param filesToWatch The files to watch.
   * @param onChange A callback that is executed whenever something changes.
   * @return A watcher
   */
  def watch(filesToWatch: Seq[File], onChange: () => Unit): FileWatcher

  /**
   * Watch the given sequence of files or directories.
   *
   * @param filesToWatch The files to watch.
   * @param onChange A callback that is executed whenever something changes.
   * @return A watcher
   */
  def watch(filesToWatch: List[File], onChange: Callable[Void]): FileWatcher = {
    watch(JavaConversions.asScalaBuffer(filesToWatch), () => { onChange.call })
  }

}

/**
 * A watcher, that watches files.
 */
trait FileWatcher {

  /**
   * Stop watching the files.
   */
  def stop(): Unit
}

object FileWatchService {
  private sealed trait OS
  private case object Windows extends OS
  private case object Linux extends OS
  private case object OSX extends OS
  private case object Other extends OS

  private val os: OS = {
    sys.props.get("os.name").map { name =>
      name.toLowerCase(Locale.ENGLISH) match {
        case osx if osx.contains("darwin") || osx.contains("mac") => OSX
        case windows if windows.contains("windows") => Windows
        case linux if linux.contains("linux") => Linux
        case _ => Other
      }
    }.getOrElse(Other)
  }

  def defaultWatchService(targetDirectory: File, pollDelayMillis: Int, logger: LoggerProxy): FileWatchService = new FileWatchService {
    lazy val delegate = os match {
      // If Windows or Linux and JDK7, use JDK7 watch service
      case (Windows | Linux) if Properties.isJavaAtLeast("1.7") => new JDK7FileWatchService(logger)
      // If Windows, Linux or OSX, use JNotify but fall back to SBT
      case (Windows | Linux | OSX) => JNotifyFileWatchService(targetDirectory).recover {
        case e =>
          logger.warn("Error loading JNotify watch service: " + e.getMessage)
          logger.trace(e)
          new PollingFileWatchService(pollDelayMillis)
      }.get
      case _ => new PollingFileWatchService(pollDelayMillis)
    }

    def watch(filesToWatch: Seq[File], onChange: () => Unit) = delegate.watch(filesToWatch, onChange)
  }

  def jnotify(targetDirectory: File): FileWatchService = optional(JNotifyFileWatchService(targetDirectory))

  def jdk7(logger: LoggerProxy): FileWatchService = new JDK7FileWatchService(logger)

  def sbt(pollDelayMillis: Int): FileWatchService = new PollingFileWatchService(pollDelayMillis)

  def optional(watchService: Try[FileWatchService]): FileWatchService = new OptionalFileWatchServiceDelegate(watchService)
}

private[play] trait DefaultFileWatchService extends FileWatchService {
  def delegate: FileWatchService
}

/**
 * A polling Play watch service.  Polls in the background.
 */
private[play] class PollingFileWatchService(val pollDelayMillis: Int) extends FileWatchService {

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

    new FileWatcher {
      def stop() = stopped = true
    }
  }
}

private[play] class JNotifyFileWatchService(delegate: JNotifyFileWatchService.JNotifyDelegate) extends FileWatchService {
  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    val listener = delegate.newListener(onChange)
    val registeredIds = filesToWatch.map { file =>
      delegate.addWatch(file.getAbsolutePath, listener)
    }
    new FileWatcher {
      def stop() = registeredIds.foreach(delegate.removeWatch)
    }
  }
}

private object JNotifyFileWatchService {

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

    @throws[Throwable]("If we were not able to successfully load JNotify")
    def ensureLoaded(): Unit = {
      removeWatchMethod.invoke(null, 0.asInstanceOf[java.lang.Integer])
    }
  }

  // Tri state - null means no attempt to load yet, None means failed load, Some means successful load
  @volatile var watchService: Option[Try[JNotifyFileWatchService]] = None

  def apply(targetDirectory: File): Try[FileWatchService] = {

    watchService match {
      case None =>
        val ws = scala.util.control.Exception.allCatch.withTry {

          val classloader = GlobalStaticVar.get[ClassLoader]("FileWatchServiceJNotifyHack").getOrElse {
            val jnotifyJarFile = this.getClass.getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs
              .map(_.getFile)
              .find(_.contains("/jnotify"))
              .map(new File(_))
              .getOrElse(sys.error("Missing JNotify?"))

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

            // Create classloader just for jnotify
            val loader = new URLClassLoader(Array(jnotifyJarFile.toURI.toURL), null)

            GlobalStaticVar.set("FileWatchServiceJNotifyHack", loader)

            loader
          }

          val jnotifyClass = classloader.loadClass("net.contentobjects.jnotify.JNotify")
          val jnotifyListenerClass = classloader.loadClass("net.contentobjects.jnotify.JNotifyListener")
          val addWatchMethod = jnotifyClass.getMethod("addWatch", classOf[String], classOf[Int], classOf[Boolean], jnotifyListenerClass)
          val removeWatchMethod = jnotifyClass.getMethod("removeWatch", classOf[Int])

          val d = new JNotifyDelegate(classloader, jnotifyListenerClass, addWatchMethod, removeWatchMethod)

          // Try it
          d.ensureLoaded()

          new JNotifyFileWatchService(d)
        }
        watchService = Some(ws)
        ws
      case Some(ws) => ws
    }
  }
}

private[play] class JDK7FileWatchService(logger: LoggerProxy) extends FileWatchService {

  import java.nio.file._
  import StandardWatchEventKinds._

  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    val dirsToWatch = filesToWatch.filter { file =>
      if (file.isDirectory) {
        true
      } else if (file.isFile) {
        // JDK7 WatchService can't watch files
        logger.warn("JDK7 WatchService only supports watching directories, but an attempt has been made to watch the file: " + file.getCanonicalPath)
        logger.warn("This file will not be watched. Either remove the file from playMonitoredFiles, or configure a different WatchService, eg:")
        logger.warn("PlayKeys.fileWatchService := play.runsupport.FileWatchService.jnotify(target.value)")
        false
      } else false
    }

    val watcher = FileSystems.getDefault.newWatchService()

    def watchDir(dir: File) = {
      dir.toPath.register(watcher, Array[WatchEvent.Kind[_]](ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
        // This custom modifier exists just for polling implementations of the watch service, and means poll every 2 seconds.
        // For non polling event based watchers, it has no effect.
        com.sun.nio.file.SensitivityWatchEventModifier.HIGH)
    }

    // Get all sub directories
    val allDirsToWatch = allSubDirectories(dirsToWatch)
    allDirsToWatch.foreach(watchDir)

    val thread = new Thread(new Runnable {
      def run() = {
        try {
          while (true) {
            val watchKey = watcher.take()

            val events = watchKey.pollEvents()

            import scala.collection.JavaConversions._
            // If a directory has been created, we must watch it and its sub directories
            events.foreach { event =>

              if (event.kind == ENTRY_CREATE) {
                val file = watchKey.watchable.asInstanceOf[Path].resolve(event.context.asInstanceOf[Path]).toFile

                if (file.isDirectory) {
                  allSubDirectories(Seq(file)).foreach(watchDir)
                }
              }
            }

            onChange()

            watchKey.reset()
          }
        } catch {
          case NonFatal(e) => // Do nothing, this means the watch service has been closed, or we've been interrupted.
        } finally {
          // Just in case it wasn't closed.
          watcher.close()
        }
      }
    }, "sbt-play-watch-service")
    thread.setDaemon(true)
    thread.start()

    new FileWatcher {
      def stop() = {
        watcher.close()
      }
    }

  }

  private def allSubDirectories(dirs: Seq[File]) = {
    (dirs ** (DirectoryFilter -- HiddenFileFilter)).get.distinct
  }
}

/**
 * Watch service that delegates to a try. This allows it to exist without reporting an exception unless it's used.
 */
private[play] class OptionalFileWatchServiceDelegate(val watchService: Try[FileWatchService]) extends FileWatchService {
  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    watchService.map(ws => ws.watch(filesToWatch, onChange)).get
  }
}

/**
 * Provides a global (cross classloader) static var.
 *
 * This does not leak classloaders (unless the value passed to it references a classloader that shouldn't be leaked).
 * It uses an MBeanServer to store an AtomicReference as an mbean, exposing the get method of the AtomicReference as an
 * mbean operation, so that the value can be retrieved.
 */
private[runsupport] object GlobalStaticVar {
  import javax.management._
  import javax.management.modelmbean._
  import java.lang.management._
  import java.util.concurrent.atomic.AtomicReference

  private def objectName(name: String) = {
    new ObjectName(":type=GlobalStaticVar,name=" + name)
  }

  /**
   * Set a global static variable with the given name.
   */
  def set(name: String, value: AnyRef): Unit = {

    val reference = new AtomicReference[AnyRef](value)

    // Now we construct a MBean that exposes the AtomicReference.get method
    val getMethod = classOf[AtomicReference[_]].getMethod("get")
    val getInfo = new ModelMBeanOperationInfo("The value", getMethod)
    val mmbi = new ModelMBeanInfoSupport("GlobalStaticVar",
      "A global static variable",
      null, // no attributes
      null, // no constructors
      Array(getInfo), // the operation
      null); // no notifications

    val mmb = new RequiredModelMBean(mmbi)
    mmb.setManagedResource(reference, "ObjectReference")

    // Register the Model MBean in the MBean Server
    ManagementFactory.getPlatformMBeanServer.registerMBean(mmb, objectName(name))
  }

  /**
   * Get a global static variable by the given name.
   */
  def get[T](name: String)(implicit ct: ClassTag[T]): Option[T] = {
    try {
      val value = ManagementFactory.getPlatformMBeanServer.invoke(objectName(name), "get", Array.empty, Array.empty)
      if (ct.runtimeClass.isInstance(value)) {
        Some(value.asInstanceOf[T])
      } else {
        throw new ClassCastException(s"Global static var $name is not an instance of ${ct.runtimeClass}, but is actually a ${Option(value).fold("null")(_.getClass.getName)}")
      }
    } catch {
      case e: InstanceNotFoundException =>
        None
    }
  }

  /**
   * Clear a global static variable with the given name.
   */
  def remove(name: String): Unit = {
    try {
      ManagementFactory.getPlatformMBeanServer.unregisterMBean(objectName(name))
    } catch {
      case e: InstanceNotFoundException =>
    }
  }
}
