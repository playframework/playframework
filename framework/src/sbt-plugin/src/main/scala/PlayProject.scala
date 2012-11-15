package sbt

import Keys._
import jline._

import play.api._
import play.core._

import play.console.Colors

import play.Project._

@deprecated("use play.Project instead", "2.1")
object PlayProject extends Plugin with PlayExceptions with PlayKeys with PlayReloader with PlayCommands
    with PlaySettings with PlayPositionMapper {

  // ----- 
  @deprecated("use play.Project instead", "2.1")
  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), mainLang: String = NONE, settings: => Seq[Setting[_]] = Defaults.defaultSettings): Project = {
    
    println(Colors.red("""
      |
      |WARNING
      |
      |Looks like you are using a deprecated version of Play's SBT Project (PlayProject in project/Build.scala).
      |We are adding all of the new Play artifacts to your libraryDependencies for now but consider switching to the new API (i.e. play.Project).
      |
      |For any migration related issues, please consult the migration manual at http://www.playframework.org
      """).stripMargin)

    val allDependencies = (dependencies ++ Seq(jdbc, anorm, javaCore, javaJdbc, javaEbean )).toSet.toSeq

    
    lazy val playSettings =
      PlayProject.defaultSettings ++ eclipseCommandSettings(mainLang) ++ intellijCommandSettings(mainLang) ++ Seq(testListeners += testListener) ++ whichLang(mainLang) ++ Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions in Compile ++= Seq("-encoding", "utf8", "-g"),
        version := applicationVersion,
        libraryDependencies ++= allDependencies
      )

    lazy val allSettings = settings ++ playSettings

    Project(name, path, settings = allSettings)

  }
}
