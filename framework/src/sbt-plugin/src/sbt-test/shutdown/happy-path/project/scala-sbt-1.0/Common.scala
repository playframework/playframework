import sbt._
import scala.sys.process._

object Common {

  /** Invokes an external process and returns the stdout produced by that process.
    */
  def runProcess(command: String): String = "jps".!!

  def runSbtCommand(command: String, state: State): State =
    SbtInternals.commandProcess(command, state)
}

package sbt {

  object SbtInternals {

    // TODO: remove the use of this internal API
   import sbt.internal.util.complete.DefaultParsers

    // This is copy/pasted from https://github.com/sbt/sbt/commit/dfbb67e7d6699fd6c131d7259e1d5f72fdb097f6.
    // Command.process was remove in sbt 1.0 and put back on sbt 1.2. For this code to run
    // on sbt 0.13.x, 1.0.x, 1.1.x and 1.2.x I'm copy/pasting here.
    def commandProcess(command: String, state: State): State = {
      val parser = Command.combine(state.definedCommands)
      DefaultParsers.parse(command, parser(state)) match {
        case Right(s) => s() // apply command.  command side effects happen here
        case Left(errMsg) =>
          state.log error errMsg
          state.fail
      }
    }


  }

}