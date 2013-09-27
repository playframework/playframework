package play

import sbt.{ Project => _, _ }
import sbt.Keys._

import play.console.Colors
import com.typesafe.sbt.SbtNativePackager.packageArchetype

object Project extends Plugin with PlayExceptions with play.Keys with PlayReloader with PlayCommands
    with PlayRun with play.Settings with PlayPositionMapper with PlaySourceGenerators {

  // ~~ Alerts  
  if (Option(System.getProperty("play.debug.classpath")).filter(_ == "true").isDefined) {
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

  private lazy val commonSettings: Seq[Setting[_]] =
    packageArchetype.java_application ++
      defaultSettings ++
      intellijCommandSettings ++
      Seq(testListeners += testListener) ++
      Seq(
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
        javacOptions in Compile ++= Seq("-encoding", "utf8", "-g")
      )

  lazy val playJavaSettings: Seq[Setting[_]] =
    commonSettings ++
      eclipseCommandSettings(JAVA) ++
      defaultJavaSettings ++
      Seq(libraryDependencies += javaCore)

  lazy val playScalaSettings: Seq[Setting[_]] =
    commonSettings ++
      eclipseCommandSettings(SCALA) ++
      defaultScalaSettings

  // Provided for backward compatibility because we now prefer sbt settings to be used directly.
  // FIXME: Deprecate this method in the future.
  def apply(name: String, applicationVersion: String = "1.0", dependencies: Seq[ModuleID] = Nil, path: File = file("."), settings: => Seq[Setting[_]] = Seq()): sbt.Project = {
    lazy val playSettings = if (dependencies.contains(javaCore)) playJavaSettings else playScalaSettings

    lazy val projectSettings: Seq[Setting[_]] = Seq(
      version := applicationVersion,
      libraryDependencies ++= dependencies
    )

    sbt.Project(name, path)
      .settings(playSettings: _*)
      .settings(projectSettings: _*)
      .settings(settings: _*)
  }
}