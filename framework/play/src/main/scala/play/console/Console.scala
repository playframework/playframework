package play.console

import jline._
import java.io._
import sbt.IO

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

  def newCommand(args: Array[String]) = {

    val path = args.headOption.map(new File(_)).getOrElse(new File(".")).getCanonicalFile
    val defaultName = path.getName

    Option(path).filterNot(_.exists).foreach(IO.createDirectory(_))

    println()
    println("The new application will be created in %s".format(path.getAbsolutePath))

    if (path.listFiles.size > 0) {
      Colors.red("The directory is not empty, cannot create a new application here.")
    } else {
      consoleReader.printString(Colors.cyan("What is the application name? "))
      consoleReader.putString(defaultName)
      val name = Option(consoleReader.readLine()).map(_.trim).filter(_.size > 0).getOrElse(defaultName)
      println()
      NewApplication(path, name).create()
    }

  }

  def helpCommand(args: Array[String]) = {
    """
            |Welcome to Play 2.0!
            |
            |These commands are available:
            |-----------------------------
            |(default)      Enter the development console.
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

case class NewApplication(path: File, name: String) {

  def create() = {

    IO.write(new File(path, "app/controllers/Application.java"),
      """|package controllers;
               |
               |import play.mvc.*;
               |import views.html.*;
               |
               |public class Application extends Controller {
               |
               |    public static Result index() {
               |        return ok(index.render("World"));
               |    }
               |
               |}
            """.stripMargin)

    IO.write(new File(path, "app/views/index.scala.html"),
      """|@(name:String)
               |<html>
               |    <head>
               |        <title>Home</title>
               |        <link rel="shortcut icon" type="image/png" href="http://www.playframework.org/public/images/favicon.png">
               |        <link rel="stylesheet" type="text/css" media="screen" href="@routes.Assets.at("stylesheets/main.css")"> 
               |    </head>
               |    <body>
               |        <h1>Hello @name!</h1>
               |    </body>
               |</html>
            """.stripMargin)

    IO.write(new File(path, "public/stylesheets/main.css"),
      """|h1 {
               |    color: blue;
               |}
            """.stripMargin)

    IO.write(new File(path, "conf/application.conf"),
      """|# Configuration
               |
               |application.name=%s
            """.stripMargin.format(name))

    IO.write(new File(path, "conf/routes"),
      """|# Routes
               |# This file defines all application routes (Higher priority routes first)
               |# ~~~~
               |
               |GET     /                       controllers.Application.index()
               |
               |# Map static resources from the /public folder to the /public path
               |GET     /public/*file           controllers.Assets.at(path="/public", file)
               |
            """.stripMargin)

    IO.write(new File(path, "project/Build.scala"),
      """|import sbt._
               |import Keys._
               |
               |object ApplicationBuild extends Build {
               |
               |    val appName         = "%s"
               |    val appVersion      = "0.1"
               |
               |    val appDependencies = Nil
               |
               |    val main = PlayProject(appName, appVersion, appDependencies)
               |
               |}
            """.stripMargin.format(name))

    IO.write(new File(path, "project/plugins.sbt"),
      """|//-------------PLAY CORE ---------------------
      |libraryDependencies += "play" %% "play" % "2.0"
      |//---------------------------------------------""".stripMargin)

    """|OK, application %s is created.
           |Type `play` to enter the development console.
           |Have fun!
        """.stripMargin.format(name).trim
  }

}
