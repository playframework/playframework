//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

import play.sbt.activator.Templates._

templateSettings

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7")

templates := {
  def template(name: String, lang: String, includeDirNames: String*): TemplateSources = {
    val dir = baseDirectory.value
    TemplateSources(
      name = name,
      mainDir = dir / name,
      includeDirs = includeDirNames.map(dir / _),
      params = Map(
        "LANG_FILE_SUFFIX" -> lang,
        "LANG_TITLE_CASE" -> (lang.substring(0, 1).toUpperCase + lang.substring(1).toLowerCase)
      )
    )
  }
  Seq(
    template("play-scala", "scala", "play-common"),
    template("play-java", "java", "play-common"),
    template("play-scala-intro", "scala"),
    template("play-java-intro", "java")
  )
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
  "SBT_VERSION" -> "0.13.11",
  "COFFEESCRIPT_VERSION" -> "1.0.0",
  "LESS_VERSION" -> "1.1.0",
  "JSHINT_VERSION" -> "1.0.3",
  "DIGEST_VERSION" -> "1.1.0",
  "RJS_VERSION" -> "1.0.7",
  "MOCHA_VERSION" -> "1.1.0",
  "SASSIFY_VERSION" -> "1.4.2",
  "ENHANCER_VERSION" -> "1.1.0",
  "EBEAN_VERSION" -> "1.0.0",
  "PLAY_SLICK_VERSION" -> "2.0.0",
  "SCALATESTPLUS_PLAY_VERSION" -> "1.5.1",
  "TEMPLATE_NAME_SUFFIX" -> templateNameAndTitle(version.value)._1,
  "TEMPLATE_TITLE_SUFFIX" -> templateNameAndTitle(version.value)._2
)

// Ignore Mac OS files contained in templates
ignoreTemplateFiles += ".DS_Store"
