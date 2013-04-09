import sbt._
import Keys._
import PlayKeys._
import play.core.PlayVersion
import PlaySourceGenerators._

object ApplicationBuild extends Build {

  val main = Project("Play-Documentation", file(".")).settings(
    version := PlayVersion.current,
    scalaVersion := PlayVersion.scalaVersion,
    libraryDependencies ++= Seq(
      component("play") % "test",
      component("play-test") % "test"
    ),

    javaManualSourceDirectories := (file("manual") / "javaGuide" ** "code").get,
    scalaManualSourceDirectories := (file("manual") / "scalaGuide" ** "code").get,

    unmanagedSourceDirectories in Test <++= javaManualSourceDirectories,
    unmanagedSourceDirectories in Test <++= scalaManualSourceDirectories,

    // Need to ensure that templates in the Java docs get Java imports, and in the Scala docs get Scala imports
    sourceGenerators in Test <+= (state, javaManualSourceDirectories, sourceManaged in Test, templatesTypes) map { (s, ds, g, t) =>
      ds.flatMap(d => ScalaTemplates(s, d, g, t, defaultTemplatesImport ++ defaultJavaTemplatesImport))
    },
    sourceGenerators in Test <+= (state, scalaManualSourceDirectories, sourceManaged in Test, templatesTypes) map { (s, ds, g, t) =>
      ds.flatMap(d => ScalaTemplates(s, d, g, t, defaultTemplatesImport ++ defaultScalaTemplatesImport))
    },

    templatesTypes := {
      case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
      case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
      case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
    }

  )

  lazy val javaManualSourceDirectories = SettingKey[Seq[File]]("java-manual-source-directories")
  lazy val scalaManualSourceDirectories = SettingKey[Seq[File]]("scala-manual-source-directories")

}
