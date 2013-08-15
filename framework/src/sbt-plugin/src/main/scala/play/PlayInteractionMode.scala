package play

trait PlayInteractionMode {
  /**
   * This is our means of blocking a `play run` call until the user has
   *  denoted, via some interface (console or GUI) that play should no
   *  longer be running.
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
 * Default behavior for interaction mode is to wait on jline.
 */
object PlayConsoleInteractionMode extends PlayInteractionMode {

  private val consoleReader = new jline.ConsoleReader

  private def waitForKey(): Unit = {
    def waitEOF(): Unit = {
      consoleReader.readVirtualKey() match {
        case 4 => // STOP
        case 11 => consoleReader.clearScreen(); waitEOF()
        case 10 => println(); waitEOF()
        case _ => waitEOF()
      }
    }
    doWithoutEcho(waitEOF())
  }

  override def doWithoutEcho(f: => Unit): Unit = {
    consoleReader.getTerminal.disableEcho()
    try f
    finally consoleReader.getTerminal.enableEcho()
  }

  override def waitForCancel(): Unit = waitForKey()

  override def toString = "Console Interaction Mode"
}