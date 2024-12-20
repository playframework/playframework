// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import java.net.URLClassLoader

import com.typesafe.sbt.packager.Keys.executableScriptName

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .dependsOn(module)
  .aggregate(module)
  .settings(
    name          := "assets-sample",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    Assets / LessKeys.less / includeFilter := "*.less",
    Assets / LessKeys.less / excludeFilter := "_*.less"
  )

lazy val module = (project in file("module")).enablePlugins(PlayScala)

TaskKey[Unit]("unzipAssetsJar") := {
  IO.unzip(
    target.value / "universal" / "stage" / "lib" / s"${organization.value}.${normalizedName.value}-${version.value}-assets.jar",
    target.value / "assetsJar"
  )
}

InputKey[Unit]("checkOnClasspath") := {
  val args                                = Def.spaceDelimited("<resource>*").parsed
  val creator: ClassLoader => ClassLoader = play.sbt.PlayInternalKeys.playAssetsClassLoader.value
  val classloader                         = creator(null)
  args.foreach { resource =>
    if (classloader.getResource(resource) == null) {
      sys.error(s"Could not find $resource\n in assets classloader")
    } else {
      streams.value.log.info(s"Found $resource in classloader")
    }
  }
}

InputKey[Unit]("checkOnTestClasspath") := {
  val args                 = Def.spaceDelimited("<resource>*").parsed
  val classpath: Classpath = (Test / fullClasspath).value
  val classloader          = new URLClassLoader(classpath.map(_.data.toURI.toURL).toArray)
  args.foreach { resource =>
    if (classloader.getResource(resource) == null) {
      sys.error(s"Could not find $resource\nin test classpath: $classpath")
    } else {
      streams.value.log.info(s"Found $resource in classloader")
    }
  }
}

TaskKey[Unit]("check-assets-jar-on-classpath") := {
  val startScript = IO.read(target.value / "universal" / "stage" / "bin" / executableScriptName.value)
  val assetsJar   = s"${organization.value}.${normalizedName.value}-${version.value}-assets.jar"
  if (startScript.contains(assetsJar)) {
    println(s"Found reference to $assetsJar in start script")
  } else {
    sys.error(s"Could not find $assetsJar in start script")
  }
}
Global / resolvers += "scala-integration".at("https://scala-ci.typesafe.com/artifactory/scala-integration/")
