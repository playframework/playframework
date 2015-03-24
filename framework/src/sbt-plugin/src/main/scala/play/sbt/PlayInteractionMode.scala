/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt

import java.io.Closeable

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
  // TODO - Hooks for messages that print to screen?
}

/**
 * Marker trait to signify a non blocking interaction mode.
 *
 * This is provided, rather than adding a new flag to PlayInteractionMode, to preserve binary compatibility.
 */
trait PlayNonBlockingInteractionMode extends PlayInteractionMode {
  def waitForCancel() = ()
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

  private def withConsoleReader[T](f: ConsoleReader => T): T = {
    val consoleReader = new ConsoleReader
    try f(consoleReader) finally consoleReader.shutdown()
  }
  private def waitForKey(): Unit = {
    withConsoleReader { consoleReader =>
      def waitEOF(): Unit = {
        consoleReader.readCharacter() match {
          case 4 | -1 =>
          // Note: we have to listen to -1 for jline2, for some reason...
          // STOP on Ctrl-D or EOF.
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
      try f finally terminal.restore()
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
