package sbt

import Keys._
import jline._

import play.api._
import play.core._

import play.utils.Colors

object PlayProject extends Plugin with PlayExceptions with PlayKeys with PlayReloader with PlayCommands with PlaySettings {

  val JAVA = "java"
  val SCALA = "scala"
  val NONE = "none"

  // ----- Create a Play project with default settings

  private def whichLang(name: String) = {
    if (name == JAVA) {
      defaultJavaSettings
    } else if (name == SCALA) {
      defaultScalaSettings
    } else {
      Seq.empty
    }
  }

  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), mainLang: String = NONE) = {
    import com.typesafe.sbteclipse.EclipsePlugin
    import com.typesafe.sbteclipse.EclipsePlugin._
    Project(name, path)
      .settings(Seq(testListeners += testListener): _*)
      .settings(parallelExecution in Test := false)
      .settings(EclipsePlugin.settings: _*)
      .settings(
        EclipseKeys.commandName := "eclipsify", // TODO Still without effect, more work on sbteclipse needed!
        EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Managed
      )
      .settings(PlayProject.defaultSettings: _*)
      .settings(whichLang(mainLang): _*)
      .settings(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xcheckinit", "-encoding", "utf8"),
        version := applicationVersion,
        libraryDependencies ++= dependencies
      )
  }
}
