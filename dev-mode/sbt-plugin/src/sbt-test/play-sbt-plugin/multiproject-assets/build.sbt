// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import java.net.URLClassLoader

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.{ stagingDirectory => universalStagingDirectory }
import com.typesafe.sbt.packager.Keys.executableScriptName

@transient val unzipAssetsJar            = taskKey[Unit]("Unzip the staged assets JAR")
@transient val checkOnClasspath          = inputKey[Unit]("Check resources on the run classpath")
@transient val checkOnTestClasspath      = inputKey[Unit]("Check resources on the test classpath")
@transient val checkCompiledAssets       = taskKey[Unit]("Check compiled assets")
@transient val checkCleanedAssets        = taskKey[Unit]("Check that compiled assets were cleaned")
@transient val checkAssetsJarOnClasspath = taskKey[Unit]("Check the staged assets JAR classpath")
@transient val checkReloaderClasspath    = inputKey[Unit]("Check resources on the reloader classpath")
@transient val checkInternalWebModules   = taskKey[Unit]("Check that internal web modules are unique")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .dependsOn(module, runtimeModule % Runtime)
  .aggregate(module, runtimeModule)
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

lazy val runtimeModule = (project in file("runtime-module"))
  .settings(
    name          := "runtime-module-sample",
    version       := "1.0-SNAPSHOT",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false)
  )

root / unzipAssetsJar := {
  IO.unzip(
    (root / Universal / universalStagingDirectory).value / "lib" / s"${(root / organization).value}.${(root / normalizedName).value}-${(root / version).value}-assets.jar",
    (root / baseDirectory).value / "target" / "assetsJar"
  )
}

root / checkOnClasspath := {
  val args                                = Def.spaceDelimited("<resource>*").parsed
  val creator: ClassLoader => ClassLoader = (root / play.sbt.PlayInternalKeys.playAssetsClassLoader).value
  val classloader                         = creator(null)
  args.foreach { resource =>
    if (classloader.getResource(resource) == null) {
      sys.error(s"Could not find $resource\n in assets classloader")
    } else {
      streams.value.log.info(s"Found $resource in classloader")
    }
  }
}

root / checkOnTestClasspath := {
  val args                             = Def.spaceDelimited("<resource>*").parsed
  val classpath: Classpath             = (root / Test / fullClasspath).value
  implicit val fc: xsbti.FileConverter = (root / fileConverter).value
  val classloader                      = new URLClassLoader(
    classpath.map(entry => play.sbt.PluginCompat.toNioPath(entry.data).toUri.toURL).toArray
  )
  args.foreach { resource =>
    if (classloader.getResource(resource) == null) {
      sys.error(s"Could not find $resource\nin test classpath: $classpath")
    } else {
      streams.value.log.info(s"Found $resource in classloader")
    }
  }
}

root / checkReloaderClasspath := {
  val args                             = Def.spaceDelimited("<resource>*").parsed
  val classpath: Classpath             = (root / play.sbt.PlayInternalKeys.playReloaderClasspath).value
  implicit val fc: xsbti.FileConverter = (root / fileConverter).value
  val paths                            = classpath.map { entry =>
    play.sbt.PluginCompat.toNioPath(entry.data).toAbsolutePath.normalize()
  }
  val ownArtifact = play.sbt.PluginCompat
    .toNioPath((root / Compile / packageBin / artifactPath).value)
    .toAbsolutePath
    .normalize()
  assert(!paths.contains(ownArtifact), s"Reloader classpath contains the current project's JAR: $ownArtifact")
  val classloader = new URLClassLoader(paths.map(_.toUri.toURL).toArray)
  args.foreach { resource =>
    if (classloader.getResource(resource) == null) {
      sys.error(s"Could not find $resource\nin reloader classpath: $classpath")
    } else {
      streams.value.log.info(s"Found $resource in reloader classloader")
    }
  }
}

root / checkInternalWebModules := {
  val modules = (root / Assets / WebKeys.internalWebModules).value
  assert(modules == modules.distinct, s"Found duplicate internal web modules: $modules")
}

root / checkCompiledAssets := {
  val files = Seq(
    (root / Assets / WebKeys.public).value / "main.css",
    (module / Assets / WebKeys.public).value / "module.css"
  )
  files.foreach(file => assert(file.exists(), s"Compiled asset does not exist: $file"))
}

root / checkCleanedAssets := {
  val files = Seq(
    (root / Assets / WebKeys.public).value / "main.css",
    (module / Assets / WebKeys.public).value / "module.css"
  )
  files.foreach(file => assert(!file.exists(), s"Compiled asset still exists: $file"))
}

root / checkAssetsJarOnClasspath := {
  val startScript = IO.read(
    (root / Universal / universalStagingDirectory).value / "bin" / (root / executableScriptName).value
  )
  val assetsJar =
    s"${(root / organization).value}.${(root / normalizedName).value}-${(root / version).value}-assets.jar"
  if (startScript.contains(assetsJar)) {
    println(s"Found reference to $assetsJar in start script")
  } else {
    sys.error(s"Could not find $assetsJar in start script")
  }
}
