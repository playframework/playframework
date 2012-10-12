package sbt

import Keys._
import jline._

import play.api._
import play.core._

import play.console.Colors

object PlayProject extends Plugin with PlayExceptions with PlayKeys with PlayReloader with PlayCommands
    with PlaySettings with PlayPositionMapper {

  val jdbc =  "play" %% "play-jdbc" % play.core.PlayVersion.current
  val anorm = "play" %% "anorm" % play.core.PlayVersion.current
  val java = "play" %% "play-java" % play.core.PlayVersion.current
  val javaJdbc = "play" %% "play-java-jdbc" % play.core.PlayVersion.current
  val javaEbean = "play" %% "play-java-ebean" % play.core.PlayVersion.current
  val javaJpa = "play" %% "play-java-jpa" % play.core.PlayVersion.current
    
  if(Option(System.getProperty("play.debug.classpath")).filter(_ == "true").isDefined) {
    println()
    this.getClass.getClassLoader.asInstanceOf[sbt.PluginManagement.PluginClassLoader].getURLs.foreach { el =>
      println(Colors.green(el.toString))
    }
    println()
  }

  Option(System.getProperty("play.version")).map {
    case badVersion if badVersion != play.core.PlayVersion.current => {
      println(
        Colors.red("""
          |This project uses Play %s!
          |Update the Play sbt-plugin version to %s (usually in project/plugins.sbt)
        """.stripMargin.format(play.core.PlayVersion.current, badVersion))
      )
    }
    case _ =>
  }

  private def whichLang(name: String): Seq[Setting[_]] = {
    if (name == JAVA) {
      defaultJavaSettings
    } else if (name == SCALA) {
      defaultScalaSettings
    } else {
      Seq.empty
    }
  }

  // ----- Create a Play project with default settings
 
  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), mainLang: String = NONE, settings: => Seq[Setting[_]] = Defaults.defaultSettings): Project = {
    
    val langAndDependencies = if (mainLang == SCALA || mainLang == JAVA) {
      println(Colors.red("""
        |
        |WARNING
        |
        |Looks like you are using a deprecated argument (mainLang) in PlayProject (project/Build.scala).
        |For now we are adding all of the new Play artifacts to your libraryDependencies 
        |but it's possible that you may need to make further changes to your codebase. 
        |Please read the migration manual at http://www.playframework.org/documentation/2.1/MigrationGuide for more details.
        """).stripMargin)
      (mainLang, (dependencies ++ Seq(jdbc, anorm, java, javaJdbc, javaEbean )).toSet.toSeq)
    } else 
      ((if (dependencies.contains(java)) JAVA else SCALA), dependencies)

    
    lazy val playSettings =
      PlayProject.defaultSettings ++ eclipseCommandSettings(langAndDependencies._1) ++ intellijCommandSettings(langAndDependencies._1) ++ Seq(testListeners += testListener) ++ whichLang(langAndDependencies._1) ++ Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions in Compile ++= Seq("-encoding", "utf8", "-g"),
        version := applicationVersion,
        libraryDependencies ++= langAndDependencies._2
      )

    lazy val allSettings = settings ++ playSettings

    Project(name, path, settings = allSettings)

  }
}
