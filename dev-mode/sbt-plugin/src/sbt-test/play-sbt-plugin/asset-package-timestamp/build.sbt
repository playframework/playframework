// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import java.time.{ Instant, ZoneId, ZoneOffset }
import java.util.zip.ZipFile

val checkAssetTimestamp = InputKey[Unit]("checkAssetTimestamp")

ThisBuild / packageTimestamp := Some(946684800000L)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name         := "asset-package-timestamp",
    version      := "1.0-SNAPSHOT",
    scalaVersion := ScriptedTools.scalaVersionFromJavaProperties(),
    checkAssetTimestamp := {
      val expected = Def.spaceDelimited().parsed match {
        case Seq(value) => value.toLong
        case values     => sys.error(s"Expected one timestamp argument, got ${values.size}")
      }
      val timestampOptionCount = (Assets / packageBin / packageOptions).value.count(
        _.isInstanceOf[play.sbt.PluginCompat.FixedTimestamp]
      )
      if (timestampOptionCount != 1) {
        sys.error(s"Expected one asset timestamp option, got $timestampOptionCount")
      }

      implicit val converter: xsbti.FileConverter = fileConverter.value
      val assetJar = play.sbt.PluginCompat.toNioPath((Assets / packageBin).value).toFile
      val archive  = new ZipFile(assetJar)
      try {
        val entry = Option(archive.getEntry("public/asset.txt"))
          .getOrElse(sys.error("Asset archive does not contain public/asset.txt"))
        // ZIP timestamps have no zone. sbt 1 and sbt 2 may expose either the epoch value
        // or its UTC wall-clock value.
        val expectedDateTime = Instant.ofEpochMilli(expected).atZone(ZoneOffset.UTC).toLocalDateTime
        val actualDateTime   = Instant.ofEpochMilli(entry.getTime).atZone(ZoneId.systemDefault()).toLocalDateTime
        if (entry.getTime != expected && actualDateTime != expectedDateTime) {
          sys.error(s"Expected asset timestamp $expected, got ${entry.getTime}")
        }
      } finally {
        archive.close()
      }
    }
  )
