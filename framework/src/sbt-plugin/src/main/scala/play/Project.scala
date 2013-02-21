package play

import play.api._
import play.core._
import play.sbt._
import _root_.sbt.{Project => SbtProject, _}
import _root_.sbt.Keys._

import play.console.Colors

object Project extends Plugin with PlayExceptions with PlayKeys with PlayReloader with PlayCommands
    with PlaySettings with PlayPositionMapper {

  // ~~ Alerts  
  if(Option(System.getProperty("play.debug.classpath")).filter(_ == "true").isDefined) {
    println()
    this.getClass.getClassLoader.asInstanceOf[PluginManagement.PluginClassLoader].getURLs.foreach { el =>
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


  // ----- Create a Play project with default settings
 
  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), settings: => Seq[Setting[_]] = Defaults.defaultSettings): SbtProject = {
    val mainLang = if (dependencies.contains(javaCore)) JAVA else SCALA

    lazy val playSettings =
      Project.defaultSettings ++ eclipseCommandSettings(mainLang) ++ intellijCommandSettings(mainLang) ++ Seq(testListeners += testListener) ++ whichLang(mainLang) ++ Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions in Compile ++= Seq("-encoding", "utf8", "-g"),
        version := applicationVersion,
        libraryDependencies ++= dependencies
      )

    lazy val allSettings = settings ++ playSettings

    SbtProject(name, path, settings = allSettings)

  }
}
