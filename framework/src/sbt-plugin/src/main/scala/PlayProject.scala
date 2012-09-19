package sbt

import Keys._
import play.api._
import play.core.PlayVersion
import play.utils.Colors
import java.util.regex.Pattern

object PlayProject extends Plugin with PlayExceptions with PlayKeys with PlayReloader with PlayCommands with PlaySettings {

  val SnapshotVersion = "(.*-)SNAPSHOT"r

  Option(System.getProperty("play.version")).map {
    case SnapshotVersion(versionPrefix) if PlayVersion.current.matches(Pattern.quote(versionPrefix) + "[0-9]+") =>
    case badVersion if badVersion != PlayVersion.current => {
      println(
        Colors.red("""
          |This project uses Play %s!
          |Update the Play sbt-plugin version to %s (usually in project/plugins.sbt)
        """.stripMargin.format(PlayVersion.current, badVersion))
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

    lazy val playSettings =
      PlayProject.defaultSettings ++ eclipseCommandSettings(mainLang) ++ intellijCommandSettings(mainLang) ++ Seq(testListeners += testListener) ++ whichLang(mainLang) ++ Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions ++= Seq("-encoding", "utf8", "-g"),
        version := applicationVersion,
        libraryDependencies ++= dependencies
      )

    lazy val allSettings = settings ++ playSettings

    Project(name, path, settings = allSettings)

  }
}
