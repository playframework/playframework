/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.io.Closeable
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.InputStream
import jline.console.ConsoleReader
import scala.annotation.tailrec
import scala.concurrent.duration._

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
  def waitForCancel()           = ()
  def doWithoutEcho(f: => Unit) = f

  /**
   * Start the server, if not already started
   *
   * @param server A callback to start the server, that returns a closeable to stop it
   */
  def start(server: => Closeable): Unit

  /**
   * Stop the server started by the last start request, if such a server exists
   */
  def stop(): Unit
}

/**
 * Default behavior for interaction mode is to wait on JLine.
 */
object PlayConsoleInteractionMode extends PlayInteractionMode {
  // This wraps the InputStream with some sleep statements so it becomes interruptible.
  private[play] class InputStreamWrapper(is: InputStream, val poll: Duration) extends FilterInputStream(is) {
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

  private def createReader: ConsoleReader = {
    val in = new InputStreamWrapper(new FileInputStream(FileDescriptor.in), 2.milliseconds)
    new ConsoleReader(in, System.out)
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
          case 4 | 13 | -1 => // STOP on Ctrl-D, Enter or EOF (listen to -1 for jline2, for some reason...)
          case 11          => consoleReader.clearScreen(); waitEOF()
          case 10          => println(); waitEOF()
          case _           => waitEOF()
        }
      }
      doWithoutEcho(waitEOF())
    }
  }

  def doWithoutEcho(f: => Unit): Unit = {
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
   */
  def start(server: => Closeable) = synchronized {
    current match {
      case Some(_) => println("Not starting server since one is already started")
      case None =>
        println("Starting server")
        current = Some(server)
    }
  }

  /**
   * Stop the server started by the last start request, if such a server exists
   */
  def stop() = synchronized {
    current match {
      case None => println("Not stopping server since none is started")
      case Some(server) =>
        println("Stopping server")
        server.close()
        current = None
    }
  }
}
