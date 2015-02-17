/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import java.lang.management.ManagementFactory
import java.util.Properties

/**
 * Abstracts a JVM process so it can be mocked for testing or to
 * isolate pseudo-processes within a VM. Code using this class
 * should use the methods in this class instead of methods like
 * `System.getProperties()`, `System.exit()`, etc.
 */
trait ServerProcess {
  /** The ClassLoader that should be used */
  def classLoader: ClassLoader
  /** The command line arguments the process as invoked with */
  def args: Seq[String]
  /** The process's system properties */
  def properties: Properties
  /** Helper for getting properties */
  final def prop(name: String): Option[String] = Option(properties.getProperty(name))
  /** The process's id */
  def pid: Option[String]
  /** Add a hook to run when the process shuts down */
  def addShutdownHook(hook: => Unit): Unit
  /** Exit the process with a message and optional cause and return code */
  def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing
}

/**
 * A ServerProcess that wraps a real JVM process. Calls have a real
 * effect on the JVM, e.g. `exit` calls `System.exit.`
 */
class RealServerProcess(val args: Seq[String]) extends ServerProcess {
  def classLoader: ClassLoader = Thread.currentThread.getContextClassLoader
  def properties: Properties = System.getProperties
  def pid: Option[String] = {
    ManagementFactory.getRuntimeMXBean.getName.split('@').headOption
  }
  def addShutdownHook(hook: => Unit): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() = hook
    })
  }
  def exit(message: String, cause: Option[Throwable] = None, returnCode: Int = -1): Nothing = {
    System.err.println(message)
    cause.foreach(_.printStackTrace())
    System.exit(returnCode)
    // Code never reached, but throw an exception to give a type of Nothing
    throw new Exception("SystemProcess.exit called")
  }
}
