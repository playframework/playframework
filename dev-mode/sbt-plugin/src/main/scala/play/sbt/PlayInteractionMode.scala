/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.io.Closeable
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream

import scala.annotation.tailrec
import scala.concurrent.duration.*

import jline.console.ConsoleReader

trait PlayInteractionMode {

  /**
   * This is our means of blocking a `play run` call until
   *  the user has denoted, via some interface (console or GUI) that
   *  play should no longer be running.
   */
  def waitForCancel(): Unit

  /**
   * Enables and disables console echo (or does nothing if no console).
   * This ensures console echo is enabled on exception thrown in the
   * given code block.
   */
  def doWithoutEcho(f: => Unit): Unit

}

/**
 * Marker trait to signify a non-blocking interaction mode.
 *
 * This is provided, rather than adding a new flag to PlayInteractionMode, to preserve binary compatibility.
 */
trait PlayNonBlockingInteractionMode extends PlayInteractionMode {
  override def waitForCancel()           = ()
  override def doWithoutEcho(f: => Unit) = f

  /**
   * Start the server, if not already started
   *
   * @param server A callback to start the server, that returns a closeable to stop it
   *
   * @return A boolean indicating if the server was started (true) or not (false).
   */
  def start(server: => Closeable): Boolean

  /**
   * Stop the server started by the last start request, if such a server exists
   */
  def stop(): Unit
}

/**
 * Default behavior for interaction mode is to wait on JLine.
 */
object PlayConsoleInteractionMode extends PlayInteractionMode {

  /**
   * This wraps the InputStream with some sleep statements so it becomes interruptible.
   * Only used in sbt versions <= 1.3
   */
  private[play] class InputStreamWrapperSbtLegacy(is: InputStream, val poll: Duration) extends FilterInputStream(is) {
    @tailrec final override def read(): Int =
      if (is.available() != 0) is.read()
      else {
        Thread.sleep(poll.toMillis)
        read()
      }

    @tailrec final override def read(b: Array[Byte]): Int =
      if (is.available() != 0) is.read(b)
      else {
        Thread.sleep(poll.toMillis)
        read(b)
      }

    @tailrec final override def read(b: Array[Byte], off: Int, len: Int): Int =
      if (is.available() != 0) is.read(b, off, len)
      else {
        Thread.sleep(poll.toMillis)
        read(b, off, len)
      }
  }

  private[play] final class SystemInWrapper() extends InputStream {
    override def read(): Int = System.in.read()
  }

  private[play] final class SystemOutWrapper extends OutputStream {
    override def write(b: Int): Unit                             = System.out.write(b)
    override def write(b: Array[Byte]): Unit                     = write(b, 0, b.length)
    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
      System.out.write(b, off, len)
      System.out.flush()
    }
    override def flush(): Unit = System.out.flush()
  }

  private def createReader: ConsoleReader =
    if (System.in.getClass.getName == "java.io.BufferedInputStream") {
      // sbt <= 1.3:
      // In sbt <= 1.3 we need to create a non-blocking input stream reader, so sbt is able to interrupt the thread
      // (e.g. when user hits Ctrl-C to cancel)
      val originalIn = new FileInputStream(FileDescriptor.in)
      val in         = new InputStreamWrapperSbtLegacy(originalIn, 2.milliseconds)
      new ConsoleReader(in, System.out)
    } else {
      // sbt 1.4+ (class name is "sbt.internal.util.Terminal$proxyInputStream$"):
      // sbt makes System.in non-blocking starting with 1.4.0, therefore we shouldn't
      // create a non-blocking input stream reader ourselves, but just wrap System.in
      // and System.out (otherwise we end up in a deadlock, console will hang, not accepting inputs)
      new ConsoleReader(new SystemInWrapper(), new SystemOutWrapper())
    }

  private def withConsoleReader[T](f: ConsoleReader => T): T = {
    val consoleReader = createReader
    try f(consoleReader)
    finally consoleReader.close()
  }

  private def waitForKey(): Unit = {
    withConsoleReader { consoleReader =>
      @tailrec def waitEOF(): Unit = {
        consoleReader.readCharacter() match {
          case 4 | 13 => // STOP on Ctrl-D or Enter
          case 11     => consoleReader.clearScreen(); waitEOF()
          case 10     => println(); waitEOF()
          case _      => waitEOF()
        }
      }
      doWithoutEcho(waitEOF())
    }
  }

  override def doWithoutEcho(f: => Unit): Unit = {
    withConsoleReader { consoleReader =>
      val terminal = consoleReader.getTerminal
      terminal.setEchoEnabled(false)
      try f
      finally terminal.restore()
    }
  }

  override def waitForCancel(): Unit = waitForKey()

  override def toString = "Console Interaction Mode"
}

/**
 * Simple implementation of the non-blocking interaction mode
 * that simply stores the current application in a static variable.
 */
object StaticPlayNonBlockingInteractionMode extends PlayNonBlockingInteractionMode {
  private var current: Option[Closeable] = None

  /**
   * Start the server, if not already started
   *
   * @param server A callback to start the server, that returns a closeable to stop it
   *
   * @return A boolean indicating if the server was started (true) or not (false).
   */
  def start(server: => Closeable): Boolean = synchronized {
    current match {
      case Some(_) =>
        println("Not starting server since one is already started")
        false
      case None =>
        println("Starting server")
        current = Some(server)
        true
    }
  }

  /**
   * Stop the server started by the last start request, if such a server exists
   */
  def stop() = synchronized {
    current match {
      case None         => println("Not stopping server since none is started")
      case Some(server) =>
        println("Stopping server")
        server.close()
        current = None
    }
  }
}
