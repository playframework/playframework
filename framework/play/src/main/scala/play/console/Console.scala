package play.console

import jline._
import java.io._
import sbt.IO

object Console {
    
    val consoleReader = new jline.ConsoleReader
    
    val logo = new ANSIBuffer().yellow(
        """|       _            _ 
           | _ __ | | __ _ _  _| |
           || '_ \| |/ _' | || |_|
           ||  __/|_|\____|\__ (_)
           ||_|            |__/ 
           |             
           |""".stripMargin
    ).append(
        "play! " + play.core.PlayVersion.current + ", "
    ).underscore(
        new ANSIBuffer().append("""http://www.playframework.org""").toString
    )
    
    def newCommand(args: Array[String]) = {
        
        val path = args.headOption.map(new File(_)).getOrElse(new File(".")).getCanonicalFile
        val defaultName = path.getName
        
        Option(path).filterNot(_.exists).foreach(IO.createDirectory(_))
        
        println()        
        println("The new application will be created in %s".format(path.getAbsolutePath))
        
        if(path.listFiles.size > 0) {
            new ANSIBuffer().red("The directory is not empty, cannot create a new application here.")
        } else {
            consoleReader.printString(new ANSIBuffer().cyan("What is the application name? ").toString)
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
            |You can also browse the complete documentation at """.stripMargin +
            new ANSIBuffer().underscore("http://www.playframework.org").append(".")
    }
    
    def main(args: Array[String]) {
        println(logo)
        println(
            args.headOption.collect {
                case "new"  => newCommand _
                case "help" => helpCommand _
            }.map { command =>
                command(args.drop(1))
            }.getOrElse {
                new ANSIBuffer().red("\nThis is not a play application!\n").append(
                    """|
                       |Use `play new` to create a new Play application in the current directory, 
                       |or go to an existing application and launch the development console using `play`.
                       |
                       |You can also browse the complete documentation at """.stripMargin +
                       new ANSIBuffer().underscore("http://www.playframework.org").append(".")
                ).toString
            }
        )
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
            """.stripMargin
        )
        
        IO.write(new File(path, "app/views/index.scala.html"),
            """|@(name: String)
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
            """.stripMargin
        )
        
        IO.write(new File(path, "public/stylesheets/main.css"),
            """|h1 {
               |    color: blue;
               |}
            """.stripMargin
        )
        
        IO.write(new File(path, "conf/application.conf"), 
            """|# Configuration
               |
               |application.name=%s
            """.stripMargin.format(name)
        )
        
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
            """.stripMargin
        )

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
            """.stripMargin.format(name)
        )
        
        IO.write(new File(path, "project/plugins/project/Play.scala"),
            """|import sbt._
               |import Keys._
               |
               |object Play extends Build {
               |
               |    val version = "2.0"
               |
               |    val playRepository = Option(System.getProperty("play.home")).map { home =>
               |        Resolver.file("play-repository", file(home) / "../repository")(Resolver.ivyStylePatterns)
               |    }.toSeq
               |
               |    val play = Project("Play " + version, file(".")).settings(
               |        libraryDependencies += "play" %% "play" % version,
               |        resolvers ++= playRepository
               |    )
               |
               |}
            """.stripMargin
        )
        
        """|OK, application %s is created.
           |Type `play` to enter the development console.
           |Have fun!
        """.stripMargin.format(name).trim
    }
    
    
}