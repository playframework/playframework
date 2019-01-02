//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

import java.net.URLClassLoader
import com.typesafe.sbt.packager.Keys.executableScriptName

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .dependsOn(module)
  .aggregate(module)
  .settings(
    name := "assets-sample",
    version := "1.0-SNAPSHOT",
    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8"),
    includeFilter in (Assets, LessKeys.less) := "*.less",
    excludeFilter in (Assets, LessKeys.less) := "_*.less"
  )

lazy val module = (project in file("module"))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)

TaskKey[Unit]("unzipAssetsJar") := {
  IO.unzip(target.value / "universal" / "stage" / "lib" / s"${organization.value}.${normalizedName.value}-${version.value}-assets.jar", target.value / "assetsJar")
}

InputKey[Unit]("checkOnClasspath") := {
  val args = Def.spaceDelimited("<resource>*").parsed
  val creator: ClassLoader => ClassLoader = play.sbt.PlayInternalKeys.playAssetsClassLoader.value
  val classloader = creator(null)
  args.foreach { resource =>
    if (classloader.getResource(resource) == null) {
      throw new RuntimeException("Could not find " + resource + "\n in assets classloader")
    } else {
      streams.value.log.info("Found " + resource + " in classloader")
    }
  }
}

InputKey[Unit]("checkOnTestClasspath") := {
  val args = Def.spaceDelimited("<resource>*").parsed
  val classpath: Classpath = (fullClasspath in Test).value
  val classloader = new URLClassLoader(classpath.map(_.data.toURI.toURL).toArray)
  args.foreach { resource =>
    if (classloader.getResource(resource) == null) {
      throw new RuntimeException("Could not find " + resource + "\nin test classpath: " + classpath)
    } else {
      streams.value.log.info("Found " + resource + " in classloader")
    }
  }
}

TaskKey[Unit]("check-assets-jar-on-classpath") := {
  val startScript = IO.read(target.value / "universal" / "stage" / "bin" / executableScriptName.value)
  val assetsJar = s"${organization.value}.${normalizedName.value}-${version.value}-assets.jar"
  if (startScript.contains(assetsJar)) {
    println("Found reference to " + assetsJar + " in start script")
  } else {
    throw new RuntimeException("Could not find " + assetsJar + " in start script")
  }
}
