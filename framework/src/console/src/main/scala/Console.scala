
package play.console

import java.io._
import scalax.file._
import scala.annotation.tailrec
import jline.UnixTerminal

/**
 * provides the console infrastructure for all play apps
 */
object Console {

  val consoleReader = new jline.console.ConsoleReader

  val logo = Colors.yellow(
    """|       _
           | _ __ | | __ _ _  _
           || '_ \| |/ _' | || |
           ||  __/|_|\____|\__ /
           ||_|            |__/
           |
           |""".stripMargin) +
    ("play " + play.core.PlayVersion.current +
      " built with Scala " + play.core.PlayVersion.scalaVersion +
      " (running Java " + System.getProperty("java.version") + ")," +
      " http://www.playframework.com")

  // -- Commands

  def replace(file: File, tokens: (String, String)*) {
    if (file.exists) {
      Path(file).write(tokens.foldLeft(Path(file).string) { (state, token) =>
        state.replace("%" + token._1 + "%", token._2)
      })
    }
  }

  private def generateLocalTemplate(template: String, name: String, path: File): Unit = {
    val random = new java.security.SecureRandom
    val newSecret = (1 to 64).map { _ =>
      (random.nextInt(74) + 48).toChar
    }.mkString.replaceAll("\\\\+", "/")

    def copyRecursively(from: Path, target: Path) {
      import scala.util.control.Exception._
      from.copyTo(target)
      from.children().foreach { child =>
        catching(classOf[java.io.IOException]) opt copyRecursively(child, target / child.name)
      }
    }

    copyRecursively(
      from = Path(new File(System.getProperty("play.home") + "/skeletons/" + template)),
      target = Path(path))

    replace(new File(path, "build.sbt"),
      "APPLICATION_NAME" -> name)
    replace(new File(path, "project/plugins.sbt"),
      "PLAY_VERSION" -> play.core.PlayVersion.current)
    replace(new File(path, "project/build.properties"),
      "SBT_VERSION" -> play.core.PlayVersion.sbtVersion)
    replace(new File(path, "conf/application.conf"),
      "APPLICATION_SECRET" -> newSecret)
  }

  private def haveFun(name: String) =
    """|OK, application %s is created.
                  |
                  |Have fun!
                  |""".stripMargin.format(name).trim

  /**
   * Could use the SBT ID parser, except that means going direct into SBT code, which is supposed to be separated
   * and bridged by the xsbti API.  This is semantically equivalent.
   */
  private val IdParser = """(\p{L}[\p{L}\p{LD}_-]*)""".r

  /**
   * Creates a new play app skeleton either based on local templates on g8 templates fetched from github
   * Also, one can create a g8 template directly like this: play new app_name --g8 githubuser/repo.g8
   * @param args first parameter is the application name
   */
  def newCommand(args: Array[String]): (String, Int) = {

    val path = args.headOption.map(new File(_)).getOrElse(new File(".")).getCanonicalFile

    val defaultName = path.getName

    println()
    println(Colors.green("The new application will be created in %s".format(path.getAbsolutePath)))
    println()

    if (path.exists) {
      (Colors.red("The directory already exists, cannot create a new application here."), -1)
    } else {
      val template: (String, String) = if (args.length == 3 && args(1) == "--g8") (args.last, defaultName)
      else {
        val name = readApplicationName(defaultName)

        consoleReader.println()
        consoleReader.println("Which template do you want to use for this new application? ")
        consoleReader.println(
          """|
               |  1             - Create a simple Scala application
               |  2             - Create a simple Java application
               |""".stripMargin)

        consoleReader.putString("")

        val templateToUse = Option(consoleReader.readLine(Colors.cyan("> "))).map(_.trim).getOrElse("") match {
          case "1" => "scala-skel"
          case "2" => "java-skel"
          case other => other
        }
        (templateToUse, name)
      }
      try {
        //either generate templates based on local skeletons or use g8 templates
        if (template._1.endsWith("-skel")) {
          generateLocalTemplate(template._1, template._2, path)
          (haveFun(template._2), 0)
        } else {
          ("Unknown option: " + template._1, -1)
        }
      } catch {
        case ex: Exception =>
          ("Ooops - Something went wrong! Exception:" + ex.toString, -1)
      }
    }

  }

  @tailrec
  def readApplicationName(defaultName: String): String = {
    consoleReader.println("What is the application name? [%s]".format(defaultName))
    Option(consoleReader.readLine(Colors.cyan("> "))).map(_.trim).filter(_.size > 0).getOrElse(defaultName) match {
      case IdParser(name) => name
      case _ => {
        consoleReader.println(Colors.red("Error: ") + "Application name may only contain letters, digits, '_' and '-', and it must start with a letter.")
        consoleReader.println()
        readApplicationName(defaultName)
      }
    }
  }

  def helpCommand(args: Array[String]): (String, Int) = {
    (
      "Welcome to Play " + play.core.PlayVersion.current + """!
            |
            |These commands are available:
            |-----------------------------
            |license            Display licensing informations.
            |new [directory]    Create a new Play application in the specified directory.
            |
            |You can also browse the complete documentation at http://www.playframework.com.""".stripMargin, 0)
  }

  /**
   * This is a general main method that runs the console and returns an exit code.
   */
  def run(args: Array[String]): Int = {
    println(logo)

    val (text, status) = args.headOption.collect {
      case "new" => newCommand _
      case "help" => helpCommand _
    }.map { command =>
      command(args.drop(1))
    }.getOrElse {
      (Colors.red("\nThis is not a play application!\n") + ("""|
           |Use `play new` to create a new Play application in the current directory,
           |or go to an existing application and launch the development console using `play`.
           |
           |You can also browse the complete documentation at http://www.playframework.com.""".stripMargin), -1)
    }

    println(text)
    println()

    consoleReader.getTerminal.restore()

    status
  }

  /**
   * If play is run as an application then this will be the entry point.
   */
  def main(args: Array[String]): Unit = {
    val status = run(args)
    System.exit(status)
  }
}

/**
 * this is the sbt entry point for Play's console implementation
 */
class Console extends xsbti.AppMain {

  def run(app: xsbti.AppConfiguration): Exit = {
    val status = Console.run(app.arguments)
    Exit(status)
  }

  case class Exit(val code: Int) extends xsbti.Exit

}

object Colors {

  import scala.Console._

  lazy val isANSISupported = {
    Option(System.getProperty("sbt.log.noformat")).map(_ != "true").orElse {
      Option(System.getProperty("os.name"))
        .map(_.toLowerCase)
        .filter(_.contains("windows"))
        .map(_ => false)
    }.getOrElse(true)
  }

  def red(str: String): String = if (isANSISupported) (RED + str + RESET) else str
  def blue(str: String): String = if (isANSISupported) (BLUE + str + RESET) else str
  def cyan(str: String): String = if (isANSISupported) (CYAN + str + RESET) else str
  def green(str: String): String = if (isANSISupported) (GREEN + str + RESET) else str
  def magenta(str: String): String = if (isANSISupported) (MAGENTA + str + RESET) else str
  def white(str: String): String = if (isANSISupported) (WHITE + str + RESET) else str
  def black(str: String): String = if (isANSISupported) (BLACK + str + RESET) else str
  def yellow(str: String): String = if (isANSISupported) (YELLOW + str + RESET) else str

}

