import play.sbt.activator.Templates._

templateSettings

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7")

templates := {
  val dir = baseDirectory.value
  Seq(
    "play-scala",
    "play-java",
    "play-scala-intro",
    "play-java-intro"
  ).map(template => dir / template)
}

version := sys.props.getOrElse("play.version", version.value)

def playDocsUrl(version: String) = {
  // Use a version like 2.4.x for the documentation
  val docVersion = version.replaceAll("""(\d+)\.(\d+)\D(.*)""", "$1.$2.x")
  s"http://www.playframework.com/documentation/${docVersion}"
}

// Use different names for release and milestone templates
def templateNameAndTitle(version: String) = {
  val officialRelease = version.matches("[0-9.]+") // Match final versions but not *-SNAPSHOT or *-RC1
  if (officialRelease) ("", "") else ("-preview", " (Preview)")
}

templateParameters := Map(
  "PLAY_VERSION" -> version.value,
  "SCALA_VERSION" -> scalaVersion.value,
  "PLAY_DOCS_URL" -> playDocsUrl(version.value),
  "SBT_VERSION" -> "0.13.9",
  "COFFEESCRIPT_VERSION" -> "1.0.0",
  "LESS_VERSION" -> "1.1.0",
  "JSHINT_VERSION" -> "1.0.3",
  "DIGEST_VERSION" -> "1.1.0",
  "RJS_VERSION" -> "1.0.7",
  "MOCHA_VERSION" -> "1.1.0",
  "ENHANCER_VERSION" -> "1.1.0",
  "EBEAN_VERSION" -> "1.0.0",
  "PLAY_SLICK_VERSION" -> "1.0.0",
  "TEMPLATE_NAME_SUFFIX" -> templateNameAndTitle(version.value)._1,
  "TEMPLATE_TITLE_SUFFIX" -> templateNameAndTitle(version.value)._2
)
