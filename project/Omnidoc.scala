/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.Keys._
import sbt.Package.ManifestAttributes

/**
 * This AutoPlugin adds the `Omnidoc-Source-URL` key on the MANIFEST.MF of artifact-sources.jar so
 * later Omnidoc can use that value to link scaladocs to GitHub sources.
 */
object Omnidoc extends AutoPlugin {

  object autoImport {
    lazy val omnidocGithubRepo     = settingKey[String]("Github repository for source URL")
    lazy val omnidocSnapshotBranch = settingKey[String]("Git branch for development versions")
    lazy val omnidocTagPrefix      = settingKey[String]("Prefix before git tagged versions")
    lazy val omnidocPathPrefix     = settingKey[String]("Prefix before source directory paths")
    lazy val omnidocSourceUrl      = settingKey[Option[String]]("Source URL for scaladoc linking")
  }

  val SourceUrlKey = "Omnidoc-Source-URL"

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = noTrigger

  import autoImport._

  override def projectSettings = Seq(
    omnidocSourceUrl := omnidocGithubRepo.?.value.map { repo =>
      val development: String = (omnidocSnapshotBranch ?? "main").value
      val tagged: String      = (omnidocTagPrefix ?? "v").value + version.value
      val tree: String        = if (isSnapshot.value) development else tagged
      val prefix: String      = "/" + (omnidocPathPrefix ?? "").value
      val path: String = {
        val buildDir: File      = (ThisBuild / baseDirectory).value
        val projDir: File       = baseDirectory.value
        val rel: Option[String] = IO.relativize(buildDir, projDir)
        rel match {
          case None if buildDir == projDir => ""                // Same dir (sbt 0.13)
          case Some("")                    => ""                // Same dir (sbt 1.0)
          case Some(childDir)              => prefix + childDir // Child dir
          case None                        => ""                // Disjoint dirs (Rich: I'm not sure if this can happen)
        }
      }
      s"https://github.com/${repo}/tree/${tree}${path}"
    },
    Compile / packageSrc / packageOptions ++= omnidocSourceUrl.value.toSeq.map { url =>
      ManifestAttributes(SourceUrlKey -> url)
    }
  )

}
