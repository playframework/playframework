import play.sbt.activator.Templates._

templateSettings

scalaVersion := {
  // If we're a snapshot build, then default to 2.10.5, since this is what gets built by default for Play
  // If we're a production build, then we want 2.11.6
  sys.props.getOrElse("scala.version", if (isSnapshot.value) {
    "2.10.5"
  } else {
    "2.11.6"
  })
}

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
  "SBT_VERSION" -> "0.13.8",
  "COFFEESCRIPT_VERSION" -> "1.0.0",
  "LESS_VERSION" -> "1.0.6",
  "JSHINT_VERSION" -> "1.0.3",
  "DIGEST_VERSION" -> "1.1.0",
  "RJS_VERSION" -> "1.0.7",
  "MOCHA_VERSION" -> "1.1.0",
  "ENHANCER_VERSION" -> "1.1.0",
  "EBEAN_VERSION" -> "1.0.0",
  "TEMPLATE_NAME_SUFFIX" -> templateNameAndTitle(version.value)._1,
  "TEMPLATE_TITLE_SUFFIX" -> templateNameAndTitle(version.value)._2
)
