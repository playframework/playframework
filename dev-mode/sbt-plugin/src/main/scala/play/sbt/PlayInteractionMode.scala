/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
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
  // TODO - Hooks for messages that print to screen?
}

/**
 * Marker trait to signify a non blocking interaction mode.
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
 * Default behavior for interaction mode is to
 *  wait on jline.
 */
object PlayConsoleInteractionMode extends PlayInteractionMode {
<<<<<<< HEAD
  // This wraps the InputStream with some sleep statements
  // so it becomes interruptible.
  private[play] class InputStreamWrapper(is: InputStream, val poll: Duration) extends FilterInputStream(is) {
    @tailrec final override def read(): Int =
      if (is.available() != 0) is.read()
      else {
=======
  // This wraps the InputStream with some sleep statements so it becomes interruptible.
  private[play] final class SystemInWrapper(val poll: FiniteDuration) extends InputStream {
    @tailrec override def read(): Int = {
      if (System.in.available() > 0) {
        System.in.read()
      } else {
>>>>>>> 932aede1d2 (Fix IOException when using sbt thin client)
        Thread.sleep(poll.toMillis)
        read()
      }
    }

    override def read(b: Array[Byte]): Int = read(b, 0, b.length)

    @tailrec override def read(b: Array[Byte], off: Int, len: Int): Int = {
      if (System.in.available() > 0) {
        System.in.read(b, off, len)
      } else {
        Thread.sleep(poll.toMillis)
        read(b, off, len)
      }
    }
  }

<<<<<<< HEAD
  private def createReader: ConsoleReader = {
    val originalIn = new FileInputStream(FileDescriptor.in)
    val in         = new InputStreamWrapper(originalIn, 2.milliseconds)
    new ConsoleReader(in, System.out)
=======
  private[play] final class SystemOutWrapper extends OutputStream {
    override def write(b: Int): Unit         = System.out.write(b)
    override def write(b: Array[Byte]): Unit = write(b, 0, b.length)
    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
      System.out.write(b, off, len)
      System.out.flush()
    }
    override def flush(): Unit = System.out.flush()
>>>>>>> 932aede1d2 (Fix IOException when using sbt thin client)
  }

  private def createReader: ConsoleReader =
    new ConsoleReader(new SystemInWrapper(poll = 2.milliseconds), new SystemOutWrapper())

  private def withConsoleReader[T](f: ConsoleReader => T): T = {
    val consoleReader = createReader
    try f(consoleReader)
    finally consoleReader.close()
  }
  private def waitForKey(): Unit = {
    withConsoleReader { consoleReader =>
      def waitEOF(): Unit = {
        consoleReader.readCharacter() match {
          case 4 | 13 | -1 =>
          // Note: we have to listen to -1 for jline2, for some reason...
          // STOP on Ctrl-D, Enter or EOF.
          case 11 =>
            consoleReader.clearScreen(); waitEOF()
          case 10 =>
            println(); waitEOF()
          case x => waitEOF()
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
 * Simple implementation of the non blocking interaction mode that simply stores the current application in a static
 * variable.
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
      case Some(server) =>
        println("Stopping server")
        server.close()
        current = None
      case None => println("Not stopping server since none is started")
    }
  }
}
