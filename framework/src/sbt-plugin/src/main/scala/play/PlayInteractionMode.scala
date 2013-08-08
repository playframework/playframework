package play

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
 * Default behavior for interaction mode is to
 *  wait on jline.
 */
object PlayConsoleInteractionMode extends PlayInteractionMode {

  private val consoleReader = new jline.console.ConsoleReader

  private def waitForKey(): Unit = {
    def waitEOF(): Unit = {
      consoleReader.readCharacter() match {
        case 4 | -1 =>
        // Note: we have to listen to -1 for jline2, for some reason...
        // STOP on Ctrl-D or EOF.
        case 11 => consoleReader.clearScreen(); waitEOF()
        case 10 => println(); waitEOF()
        case x => waitEOF()
      }

    }
    doWithoutEcho(waitEOF())
  }
  override def doWithoutEcho(f: => Unit): Unit = {
    consoleReader.getTerminal.setEchoEnabled(false)
    try f
    finally consoleReader.getTerminal.setEchoEnabled(true)
  }
  override def waitForCancel(): Unit = waitForKey()

  override def toString = "Console Interaction Mode"
}
