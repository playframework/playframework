package sbt

import Keys._
import jline._

import play.api._
import play.core._

import play.utils.Colors

object PlayProject extends Plugin with PlayExceptions with PlayKeys with PlayReloader with PlayCommands with PlaySettings {

  val JAVA = "java"
  val SCALA = "scala"

  // ----- Create a Play project with default settings

  private def whichLang(name: String) = if (name == JAVA) defaultJavaSettings else defaultScalaSettings

  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), mainLang: String = JAVA) = {

    Project(name, path)
      .settings(Seq(testListeners += testListener): _*)
      .settings(parallelExecution in Test := false)
      .settings(PlayProject.defaultSettings: _*)
      .settings(whichLang(mainLang): _*)
      .settings(

        scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xcheckinit", "-encoding", "utf8"),

        version := applicationVersion,

        libraryDependencies ++= dependencies)
  }
}