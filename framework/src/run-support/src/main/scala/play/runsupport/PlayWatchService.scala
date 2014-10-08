/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.runsupport

import java.net.URLClassLoader
import java.util.Locale
import java.io.File

import scala.reflect.ClassTag
import scala.util.{ Properties, Try }
import scala.util.control.NonFatal

import sbt._
import java.io.File

import scala.reflect.ClassTag
import scala.util.{ Properties, Try }
import scala.util.control.NonFatal
import java.io.File
import Path._

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

  def default(targetDirectory: File, pollDelayMillis: Int, logger: LoggerProxy): PlayWatchService = new PlayWatchService {
    lazy val delegate = os match {
      // If Windows or Linux and JDK7, use JDK7 watch service
      case (Windows | Linux) if Properties.isJavaAtLeast("1.7") => JDK7PlayWatchService(logger).get
      // If Windows, Linux or OSX, use JNotify but fall back to SBT
      case (Windows | Linux | OSX) => JNotifyPlayWatchService(targetDirectory).recover {
        case e =>
          logger.warn("Error loading JNotify watch service: " + e.getMessage)
          logger.trace(e)
          sbt(pollDelayMillis)
      }.get
      case _ => sbt(pollDelayMillis)
    }

    def watch(filesToWatch: Seq[File], onChange: () => Unit) = delegate.watch(filesToWatch, onChange)
  }

  def jnotify(targetDirectory: File): PlayWatchService = optional(JNotifyPlayWatchService(targetDirectory))

  def jdk7(logger: LoggerProxy): PlayWatchService = optional(JDK7PlayWatchService(logger))

  def optional(watchService: Try[PlayWatchService]): PlayWatchService = new OptionalPlayWatchServiceDelegate(watchService)

  def sbt(pollDelayMillis: Int): PlayWatchService = new SbtPlayWatchService(pollDelayMillis)
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

    @throws[Throwable]("If we were not able to successfully load JNotify")
    def ensureLoaded(): Unit = {
      removeWatchMethod.invoke(null, 0.asInstanceOf[java.lang.Integer])
    }
  }

  // Tri state - null means no attempt to load yet, None means failed load, Some means successful load
  @volatile var watchService: Option[Try[JNotifyPlayWatchService]] = None

  def apply(targetDirectory: File): Try[PlayWatchService] = {

    watchService match {
      case None =>
        val ws = scala.util.control.Exception.allCatch.withTry {

          val classloader = GlobalStaticVar.get[ClassLoader]("playWatchServiceJNotifyHack").getOrElse {
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

            GlobalStaticVar.set("playWatchServiceJNotifyHack", loader)

            loader
          }

          val jnotifyClass = classloader.loadClass("net.contentobjects.jnotify.JNotify")
          val jnotifyListenerClass = classloader.loadClass("net.contentobjects.jnotify.JNotifyListener")
          val addWatchMethod = jnotifyClass.getMethod("addWatch", classOf[String], classOf[Int], classOf[Boolean], jnotifyListenerClass)
          val removeWatchMethod = jnotifyClass.getMethod("removeWatch", classOf[Int])

          val d = new JNotifyDelegate(classloader, jnotifyListenerClass, addWatchMethod, removeWatchMethod)

          // Try it
          d.ensureLoaded()

          new JNotifyPlayWatchService(d)
        }
        watchService = Some(ws)
        ws
      case Some(ws) => ws
    }
  }
}

private class JDK7PlayWatchService(delegate: JDK7PlayWatchService.JDK7WatchServiceDelegate, logger: LoggerProxy) extends PlayWatchService {

  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    val dirsToWatch = filesToWatch.filter { file =>
      if (file.isDirectory) {
        true
      } else if (file.isFile) {
        // JDK7 WatchService can't watch files
        logger.warn("JDK7 WatchService only supports watching directories, but an attempt has been made to watch the file: " + file.getCanonicalPath)
        logger.warn("This file will not be watched. Either remove the file from playMonitoredFiles, or configure a different WatchService, eg:")
        logger.warn("PlayKeys.playWatchService := play.sbtplugin.run.PlayWatchService.jnotify(target.value)")
        false
      } else false
    }

    // val watcher = FileSystems.getDefault.newWatchService()
    val watcher = delegate.newWatchService()

    // Get all sub directories
    val allDirsToWatch = allSubDirectories(dirsToWatch)
    allDirsToWatch.foreach { dir =>
      // file.toPath.register(watcher, Array(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
      //     com.sun.nio.file.SensitivityWatchEventModifier.HIGH)
      delegate.registerFileWithWatcher(dir, watcher)
    }

    val thread = new Thread(new Runnable {
      def run() = {
        try {
          while (true) {
            // val watchKey = watcher.take()
            val watchKey = delegate.takeFromWatcher(watcher)

            val events = delegate.pollEvents(watchKey)

            import scala.collection.JavaConversions._
            // If a directory has been created, we must watch it and its sub directories
            events.foreach { event =>

              // if (event.kind == ENTRY_CREATE) {
              if (delegate.isEventKindEntryCreate(event)) {
                // val file = watchKey.watchable.asInstanceOf[Path].resolve(event.context.asInstanceOf[Path]).toFile
                val file = delegate.getEventFile(watchKey, event)

                if (file.isDirectory) {
                  allSubDirectories(Seq(file)).foreach { dir =>
                    delegate.registerFileWithWatcher(dir, watcher)
                  }
                }
              }
            }

            onChange()

            // watchKey.reset()
            delegate.resetKey(watchKey)
          }
        } catch {
          case NonFatal(e) => // Do nothing, this means the watch service has been closed, or we've been interrupted.
        } finally {
          // Just in case it wasn't closed.
          // watcher.close()
          delegate.closeWatcher(watcher)
        }
      }
    }, "sbt-play-watch-service")
    thread.setDaemon(true)
    thread.start()

    new PlayWatcher {
      def stop() = {
        // watcher.close()
        delegate.closeWatcher(watcher)
      }
    }

  }

  private def allSubDirectories(dirs: Seq[File]) = {
    (dirs ** (DirectoryFilter -- HiddenFileFilter)).get.distinct
  }
}

private object JDK7PlayWatchService {

  import java.lang.reflect.Array

  @volatile var watchService: Option[Try[PlayWatchService]] = None

  def apply(logger: LoggerProxy): Try[PlayWatchService] = {
    watchService match {
      case None =>
        val ws = Try(new JDK7PlayWatchService(new JDK7WatchServiceDelegate, logger))
        watchService = Some(ws)
        ws
      case Some(ws) => ws
    }
  }

  /**
   * Captures all the reflection for invoking the JDK7 watch service in one place
   */
  class JDK7WatchServiceDelegate() {

    def loadClass(clazz: String): Class[_] = ClassLoader.getSystemClassLoader.loadClass(clazz)

    val fileSystem = loadClass("java.nio.file.FileSystems").getMethod("getDefault").invoke(null)

    val newWatchServiceMethod = loadClass("java.nio.file.FileSystem").getMethod("newWatchService")
    val toPathMethod = classOf[File].getMethod("toPath")

    val watchServiceClass = loadClass("java.nio.file.WatchService")
    val takeMethod = watchServiceClass.getMethod("take")
    val closeMethod = watchServiceClass.getMethod("close")

    val kindClass = loadClass("java.nio.file.WatchEvent$Kind")
    val kindArrayClass = Array.newInstance(kindClass, 0).getClass
    val standardWatchEventKindsClass = loadClass("java.nio.file.StandardWatchEventKinds")
    val entryCreate = standardWatchEventKindsClass.getField("ENTRY_CREATE").get(null)
    val entryDelete = standardWatchEventKindsClass.getField("ENTRY_DELETE").get(null)
    val entryModify = standardWatchEventKindsClass.getField("ENTRY_MODIFY").get(null)
    val dirKinds = {
      val array = Array.newInstance(kindClass, 3)
      Array.set(array, 0, entryCreate)
      Array.set(array, 1, entryDelete)
      Array.set(array, 2, entryModify)
      array
    }

    val modifierClass = loadClass("java.nio.file.WatchEvent$Modifier")
    val modifierArrayClass = Array.newInstance(modifierClass, 0).getClass
    val modifiers = {
      try {
        // OpenJDK specific.
        // The default polling interval (sensitivity) for implementations that poll (OSX) is 10 seconds (MEDIUM).
        // This is controlled by the SensitivityWatchEventModifier modifier.  HIGH is the shortest, at 2 seconds.
        val sensitivityClass = loadClass("com.sun.nio.file.SensitivityWatchEventModifier")
        // There are two ways to cast to a recursive type. One way is forSome. The other is a custom asInstanceOf
        // method that defines the recursive type.
        import scala.language.existentials
        val sensitivity = Enum.valueOf(sensitivityClass.asInstanceOf[Class[T] forSome { type T <: Enum[T] }], "HIGH")
        val array = Array.newInstance(modifierClass, 1)
        Array.set(array, 0, sensitivity)
        array
      } catch {
        case e: ClassNotFoundException => Array.newInstance(modifierClass, 0)
      }
    }

    val pathClass = loadClass("java.nio.file.Path")
    val registerMethod = pathClass.getMethod("register", watchServiceClass, kindArrayClass, modifierArrayClass)
    val toFileMethod = pathClass.getMethod("toFile")
    val resolveMethod = pathClass.getMethod("resolve", pathClass)

    val watchKeyClass = loadClass("java.nio.file.WatchKey")
    val pollEventsMethod = watchKeyClass.getMethod("pollEvents")
    val resetMethod = watchKeyClass.getMethod("reset")
    val watchableMethod = watchKeyClass.getMethod("watchable")

    val watchEventClass = loadClass("java.nio.file.WatchEvent")
    val kindMethod = watchEventClass.getMethod("kind")
    val contextMethod = watchEventClass.getMethod("context")

    def newWatchService(): AnyRef = newWatchServiceMethod.invoke(fileSystem)

    def registerFileWithWatcher(file: File, watcher: AnyRef): AnyRef = {
      val path = toPathMethod.invoke(file)
      registerMethod.invoke(path, watcher, dirKinds, modifiers)
    }

    def takeFromWatcher(watcher: AnyRef): AnyRef = {
      takeMethod.invoke(watcher)
    }

    def pollEvents(watchKey: AnyRef): java.util.List[AnyRef] = {
      pollEventsMethod.invoke(watchKey).asInstanceOf[java.util.List[AnyRef]]
    }

    def getEventKind(watchEvent: AnyRef): AnyRef = {
      kindMethod.invoke(watchEvent)
    }

    def isEventKindEntryCreate(watchEvent: AnyRef): Boolean = {
      kindMethod.invoke(watchEvent) == entryCreate
    }

    def getEventFile(watchKey: AnyRef, watchEvent: AnyRef): File = {
      val childPath = contextMethod.invoke(watchEvent)
      val parentPath = watchableMethod.invoke(watchKey)
      val path = resolveMethod.invoke(parentPath, childPath)
      toFileMethod.invoke(path).asInstanceOf[File]
    }

    def resetKey(watchKey: AnyRef): Boolean = {
      resetMethod.invoke(watchKey).asInstanceOf[Boolean]
    }

    def closeWatcher(watcher: AnyRef): Unit = {
      closeMethod.invoke(watcher)
    }
  }
}

/**
 * Watch service that delegates to a try. This allows it to exist without reporting an exception unless it's used.
 */
private class OptionalPlayWatchServiceDelegate(watchService: Try[PlayWatchService]) extends PlayWatchService {
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
