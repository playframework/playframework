import sbt._
import scala.sys.process._

object Common {

  /** Invokes an external process and returns the stdout produced by that process.
    */
  def runProcess(command: String): String = "jps".!!

}
