package play.console

import jline._
import java.io._
import scalax.file._
import play.utils._

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

  def newCommand(args: Array[String]): String = {

    val path = args.headOption.map(new File(_)).getOrElse(new File(".")).getCanonicalFile

    val defaultName = path.getName

    println()
    println(Colors.green("The new application will be created in %s".format(path.getAbsolutePath)))
    println()

    if (path.exists) {
      Colors.red("The directory already exists, cannot create a new application here.")
    } else {
      consoleReader.printString("What is the application name? ")
      consoleReader.printNewline
      consoleReader.printString(Colors.cyan("> "))
      consoleReader.putString(defaultName)
      val name = Option(consoleReader.readLine()).map(_.trim).filter(_.size > 0).getOrElse(defaultName)
      consoleReader.printNewline

      consoleReader.printString("Which template do you want to use for this new application? ")
      consoleReader.printNewline
      consoleReader.printString(
        """|
           |  1 - Create a simple Scala application
           |  2 - Create a simple Java application
           |  3 - Create an empty project
           |""".stripMargin)

      consoleReader.printNewline
      consoleReader.printString(Colors.cyan("> "))
      consoleReader.putString("")

      val template = Option(consoleReader.readLine()).map(_.trim).getOrElse("") match {
        case "1" => "scala-skel"
        case "2" => "java-skel"
        case _ => "empty-skel"
      }

      consoleReader.printNewline

      val random = new java.security.SecureRandom
      val newSecret = (1 to 64).map { _ =>
        (random.nextInt(74) + 48).toChar
      }.mkString.replaceAll("\\\\+", "/")

      def copyRecursively(from: Path, target: Path) {
        from.copyTo(target)
        from.children().foreach { child =>
          copyRecursively(child, target / child.name)
        }
      }

      copyRecursively(
        from = Path(new File(System.getProperty("play.home") + "/skeletons/" + template)),
        target = Path(path))

      replace(new File(path, "project/Build.scala"),
        "APPLICATION_NAME" -> name)
      replace(new File(path, "conf/application.conf"),
        "APPLICATION_SECRET" -> newSecret)

      """|OK, application %s is created.
         |
         |Have fun!
         |""".stripMargin.format(name).trim
    }

  }

  def helpCommand(args: Array[String]): String = {
    """
            |Welcome to Play 2.0!
            |
            |These commands are available:
            |-----------------------------
            |license        Display licensing informations.
            |new            Create a new Play application in the current directory.
            |
            |You can also browse the complete documentation at http://www.playframework.org.""".stripMargin
  }

  def main(args: Array[String]) {
    println(logo)
    println(
      args.headOption.collect {
        case "new" => newCommand _
        case "help" => helpCommand _
      }.map { command =>
        command(args.drop(1))
      }.getOrElse {
        Colors.red("\nThis is not a play application!\n") + ("""|
             |Use `play new` to create a new Play application in the current directory, 
             |or go to an existing application and launch the development console using `play`.
             |
             |You can also browse the complete documentation at http://www.playframework.org.""".stripMargin)
      })
    println()
  }

}

class Console extends xsbti.AppMain {

  def run(app: xsbti.AppConfiguration): Exit = {
    Console.main(app.arguments)
    Exit(0)
  }

  case class Exit(val code: Int) extends xsbti.Exit

}

