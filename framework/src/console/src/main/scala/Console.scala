
package play.console

import jline._
import java.io._
import scalax.file._
import giter8.Giter8

/**
 * provides the console infrastructure for all play apps
 */
object Console {

  val consoleReader = new jline.ConsoleReader

  val logo = Colors.yellow(
    """|       _            _ 
           | _ __ | | __ _ _  _| |
           || '_ \| |/ _' | || |_|
           ||  __/|_|\____|\__ (_)
           ||_|            |__/ 
           |             
           |""".stripMargin) + ("play! " + play.core.PlayVersion.current + ", http://www.playframework.org")

  // -- Commands

  def replace(file: File, tokens: (String, String)*) {
    if (file.exists) {
      Path(file).write(tokens.foldLeft(Path(file).slurpString) { (state, token) =>
        state.replace("%" + token._1 + "%", token._2)
      })
    }
  }

  private def interact(params: Map[String, String]): Map[String, String] = {
    params.map { e =>
      consoleReader.printString(e._1 + "[" + e._2 + "]:".stripMargin)
      consoleReader.putString("")
      e._1 -> Option(consoleReader.readLine()).map(_.trim).filter(_.size > 0).getOrElse(e._2)
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

    replace(new File(path, "project/Build.scala"),
      "APPLICATION_NAME" -> name)
    replace(new File(path, "project/plugins.sbt"),
      "PLAY_VERSION" -> play.core.PlayVersion.current)
    replace(new File(path, "conf/application.conf"),
      "APPLICATION_SECRET" -> newSecret)
  }

  private def haveFun(name: String) =
    """|OK, application %s is created.
                  |
                  |Have fun!
                  |""".stripMargin.format(name).trim

  private def addG8(g8: String) = if (g8.endsWith(".g8")) g8 else g8 + ".g8"

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
      val template: (String, String) = if (args.length == 3 && args(1) == "--g8") (addG8(args.last), defaultName)
      else {
        consoleReader.printString("What is the application name? ")
        consoleReader.printNewline
        consoleReader.printString(Colors.cyan("> "))
        consoleReader.putString(defaultName)
        consoleReader.flushConsole()
        val name = Option(consoleReader.readLine()).map(_.trim).filter(_.size > 0).getOrElse(defaultName)
        consoleReader.printNewline

        consoleReader.printString("Which template do you want to use for this new application? ")
        consoleReader.printNewline
        consoleReader.printString(
          """|
               |  1             - Create a simple Scala application
               |  2             - Create a simple Java application
               |  3             - Create an empty project
               |  <g8 template> - Create an app based on the g8 template hosted on Github
               |""".stripMargin)

        consoleReader.printNewline
        consoleReader.printString(Colors.cyan("> "))
        consoleReader.flushConsole()
        consoleReader.putString("")

        val templateNotFound = new RuntimeException("play.home system property is not set, so can not find template")
        val fs = java.io.File.separator
        val templateToUse = Option(consoleReader.readLine()).map(_.trim).getOrElse("") match {
          case "1" => "scala-skel"
          case "2" => "java-skel"
          case "3" => "empty-skel"
          case g8 @ _ => addG8(g8)
        }
        (templateToUse, name)
      }
      try {
        //either generate templates based on local skeletons or use g8 templates
        if (template._1.endsWith("-skel")) {
          generateLocalTemplate(template._1, template._2, path)
          (haveFun(template._2), 0)
        } else {
          //this is necessary because g8 only provides an option to either pass params or use interactive session to populate fields
          //but in our case we have the application_name (and path) already
          val defaults: Map[String, String] = Giter8.fetchInfo(template._1, Some("master")).right.toOption.map {
            case (defaults, templates) => defaults
            case _ => Map[String, String]()
          }.getOrElse(Map[String, String]())
          consoleReader.printNewline
          consoleReader.printString(defaults.get("description").getOrElse(template._1))
          consoleReader.printNewline
          consoleReader.printNewline
          // populate defaults (application_name is coming from play so that's already populated)
          val params: Array[String] = interact(defaults.filter(e => e._1 != "application_name" && e._1 != "description")).map(e => "--" + e._1 + "=" + e._2).toArray

          val exitCode = Giter8.run(Array(template._1, "--application_name=" + template._2) ++ params)
          if (exitCode == 0)
            (haveFun(template._2), exitCode)
          else
            ("something went wrong while processing g8 template", exitCode)
        }
      } catch {
        case ex: Exception =>
          ("Ooops - Something went wrong! Exception:" + ex.toString, -1)
      }
    }

  }

  def helpCommand(args: Array[String]): (String, Int) = {
    ("""
            |Welcome to Play 2.0!
            |
            |These commands are available:
            |-----------------------------
            |license            Display licensing informations.
            |new [directory]    Create a new Play application in the specified directory.
            |
            |You can also browse the complete documentation at http://www.playframework.org.""".stripMargin, 0)
  }

  def main(args: Array[String]) {
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
           |You can also browse the complete documentation at http://www.playframework.org.""".stripMargin), -1)
    }

    println(text)
    println()

    System.exit(status)
  }

}

/**
 * this is the SBT entry point for Play's console implementation
 */
class Console extends xsbti.AppMain {

  def run(app: xsbti.AppConfiguration): Exit = {
    Console.main(app.arguments)
    Exit(0)
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

