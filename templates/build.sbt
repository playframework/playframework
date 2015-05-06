import play.sbt.activator.Templates._
import play.core.PlayVersion

templateSettings

scalaVersion := "2.10.5"

crossScalaVersions := Seq("2.10.5", "2.11.6")

templates := {
  val dir = baseDirectory.value
  Seq(
    "play-scala",
    "play-java",
// Disabled since it seems to be causing unexplainable errors :(
//    "play-scala-intro",
    "play-java-intro"
  ).map(template => dir / template)
}

lazy val playDocsUrl = {
  // Use a version like 2.4.x for the documentation
  val docVersion = PlayVersion.current.replaceAll("""(\d+)\.(\d+)\D(.*)""", "$1.$2.x")
  s"http://www.playframework.com/documentation/${docVersion}"
}

// Use different names for release and milestone templates
lazy val templateNameAndTitle = {
  val officialRelease = PlayVersion.current.matches("[0-9.]+") // Match final versions but not *-SNAPSHOT or *-RC1
  if (officialRelease) ("", "") else ("-preview", " (Preview)")
}

templateParameters := Map(
  "PLAY_VERSION" -> PlayVersion.current,
  "SCALA_VERSION" -> scalaVersion.value,
  "PLAY_DOCS_URL" -> playDocsUrl,
  "SBT_VERSION" -> "0.13.8",
  "COFFEESCRIPT_VERSION" -> "1.0.0",
  "LESS_VERSION" -> "1.0.6",
  "JSHINT_VERSION" -> "1.0.3",
  "DIGEST_VERSION" -> "1.1.0",
  "RJS_VERSION" -> "1.0.7",
  "MOCHA_VERSION" -> "1.0.2",
  "ENHANCER_VERSION" -> "1.1.0-RC2",
  "EBEAN_VERSION" -> "1.0.0-RC1",
  "TEMPLATE_NAME_SUFFIX" -> templateNameAndTitle._1,
  "TEMPLATE_TITLE_SUFFIX" -> templateNameAndTitle._2
)
