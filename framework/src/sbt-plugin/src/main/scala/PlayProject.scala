package sbt

import Keys._
import jline._

import play.api._
import play.core._

import play.utils.Colors

object PlayProject extends Plugin with PlayExceptions with PlayKeys with PlayReloader with PlayCommands with PlaySettings {

  private def whichLang(name: String) = {
    if (name == JAVA) {
      defaultJavaSettings
    } else if (name == SCALA) {
      defaultScalaSettings
    } else {
      Seq.empty
    }
  }

  // ----- Create a Play project with default settings

  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), mainLang: String = NONE) = {

    Project(name, path)
      .settings(eclipseCommandSettings(mainLang): _*)
      .settings(PlayProject.defaultSettings: _*)
      .settings(Seq(testListeners += testListener): _*)
      .settings(whichLang(mainLang): _*)
      .settings(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xcheckinit", "-encoding", "utf8"),
        javacOptions ++= Seq("-encoding", "utf8"),
        version := applicationVersion,
        libraryDependencies ++= dependencies
      )
  }
}
