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
    
    def newCommand = {
        
        val path = new File(".").getCanonicalFile
        val defaultName = path.getName
        
        println()        
        println("Uma nova aplicação vai ser criada em %s".format(path.getAbsolutePath))
        
        if(path.listFiles.size > 0) {
            new ANSIBuffer().red("O diretório não está vazio, não é possível criar uma nova aplicação aqui.")
        } else {
            consoleReader.printString(new ANSIBuffer().cyan("Qual é o nome da aplicação? ").toString)
            consoleReader.putString(defaultName)
            val name = Option(consoleReader.readLine()).map(_.trim).filter(_.size > 0).getOrElse(defaultName)
            println()
            NewApplication(path, name).create()
        }
        
    }
    
    def helpCommand = {
        """
            |Bem-vindo ao Play 2.0!
            |
            |Estes comandos estão disponíveis:
            |-----------------------------
            |(default)      Entrar no console de desenvolvimento.
            |new            Criar uma nova aplicação do Play no diretório atual.
            |
            |Você também pode ver a documentação completa em """.stripMargin +
            new ANSIBuffer().underscore("http://www.playframework.org").append(".")
    }
    
    def main(args:Array[String]) {
        println(logo)
        println(
            args.headOption.collect {
                case "new"  => newCommand _
                case "help" => helpCommand _
            }.map { command =>
                command()
            }.getOrElse {
                new ANSIBuffer().red("\nEsta não é uma aplicação do Play!\n").append(
                    """|
                       |Use `play new` para criar uma nova aplicação do play no diretório atual, 
                       |ou vá para uma aplicação existente e inicie o ambiente de desenvolvimento usando `play`.
                       |
                       |Você também pode ver a documentação completa em """.stripMargin +
                       new ANSIBuffer().underscore("http://www.playframework.org").append(".")
                ).toString
            }
        )
        println()
    }
    
}

case class NewApplication(path:File, name:String) {
    
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
               |        return Html(index.apply("Mundo"));
               |    }
               |
               |}
            """.stripMargin
        )
        
        IO.write(new File(path, "app/views/index.scala.html"),
            """|@(name:String)
               |<html>
               |    <head>
               |        <title>Home</title>
               |        <link rel="shortcut icon" type="image/png" href="http://www.playframework.org/public/images/favicon.png">
               |        <link rel="stylesheet" type="text/css" media="screen" href="/public/stylesheets/main.css"> 
               |    </head>
               |    <body>
               |        <h1>Olá @name!</h1>
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
        
        IO.write(new File(path, "conf/application.yml"), "")
        
        IO.write(new File(path, "conf/routes"), 
            """|# Routes 
               |
               |GET      /           controllers.Application.index()
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
        
        """|OK, a aplicação %s foi criada.
           |Digite `play` para entrar no ambiente de desenvolvimento.
           |Divirta-se!
        """.stripMargin.format(name).trim
    }
}