/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.lang.module.ModuleDescriptor

import sbt._
import sbt.Keys._

/**
 * Helper to set Automatic-Module-Name in project manifests.
 *
 * The names are intentionally explicit. They carry compatibility implications and
 * should not be derived mechanically from project names. This stabilizes automatic
 * module identity; it does not turn the artifacts into explicit JPMS modules.
 */
object AutomaticModuleName extends AutoPlugin {
  object autoImport {
    val automaticModuleName      = settingKey[Option[String]]("The stable Java module name to write to the manifest")
    val automaticModuleNameCheck = taskKey[Unit]("Checks each project has exactly its configured Java module name")
  }

  import autoImport._

  override def trigger = allRequirements

  private val Header = "Automatic-Module-Name"

  private val names = Map(
    "Play"                       -> "org.playframework.play",
    "Play-AHC-WS"                -> "org.playframework.ahc.ws",
    "Play-Build-Link"            -> "org.playframework.build.link",
    "Play-Cache"                 -> "org.playframework.cache",
    "Play-Caffeine-Cache"        -> "org.playframework.caffeine.cache",
    "Play-Cluster-Sharding"      -> "org.playframework.cluster.sharding",
    "Play-Configuration"         -> "org.playframework.configuration",
    "Play-Docs"                  -> "org.playframework.docs",
    "Play-Docs-Sbt-Plugin"       -> "org.playframework.docs.sbt.plugin",
    "Play-Ehcache"               -> "org.playframework.ehcache",
    "Play-Exceptions"            -> "org.playframework.exceptions",
    "Play-Filters-Helpers"       -> "org.playframework.filters.helpers",
    "Play-Guice"                 -> "org.playframework.guice",
    "Play-JCache"                -> "org.playframework.jcache",
    "Play-JDBC"                  -> "org.playframework.jdbc",
    "Play-JDBC-Api"              -> "org.playframework.jdbc.api",
    "Play-JDBC-Evolutions"       -> "org.playframework.jdbc.evolutions",
    "Play-Java"                  -> "org.playframework.java",
    "Play-Java-Cluster-Sharding" -> "org.playframework.java.cluster.sharding",
    "Play-Java-Forms"            -> "org.playframework.java.forms",
    "Play-Java-JPA"              -> "org.playframework.java.jpa",
    "Play-Java-JDBC"             -> "org.playframework.java.jdbc",
    "Play-Joda-Forms"            -> "org.playframework.joda.forms",
    "Play-Logback"               -> "org.playframework.logback",
    "Play-Netty-Server"          -> "org.playframework.netty.server",
    "Play-OpenID"                -> "org.playframework.openid",
    "Play-Pekko-Http-Server"     -> "org.playframework.pekko.http.server",
    "Play-Pekko-Http2-Support"   -> "org.playframework.pekko.http2.support",
    "Play-Routes-Compiler"       -> "org.playframework.routes.compiler",
    "Play-Run-Support"           -> "org.playframework.run.support",
    "Play-Server"                -> "org.playframework.server",
    "Play-Specs2"                -> "org.playframework.specs2",
    "Play-Streams"               -> "org.playframework.streams",
    "Play-Test"                  -> "org.playframework.test",
    "Play-WS"                    -> "org.playframework.ws",
    "Sbt-Routes-Compiler"        -> "org.playframework.sbt.routes.compiler"
  )

  private val excluded = Set(
    "Play-Bom",
    "Play-Framework",
    "Play-Integration-Test",
    "Play-Microbenchmark",
    "Sbt-Plugin",
    "Sbt-Scripted-Tools"
  )

  private val validatedNames: Unit = {
    val conflictingProjects = names.keySet.intersect(excluded)
    if (conflictingProjects.nonEmpty) {
      throw new MessageOnlyException(
        s"Projects cannot have an Automatic-Module-Name and be excluded: ${conflictingProjects.toSeq.sorted.mkString(", ")}"
      )
    }

    val duplicates = names.groupMap(_._2)(_._1).filter(_._2.size > 1)
    if (duplicates.nonEmpty) {
      val details = duplicates.toSeq.sortBy(_._1).map {
        case (moduleName, projects) =>
          s"$moduleName: ${projects.toSeq.sorted.mkString(", ")}"
      }
      throw new MessageOnlyException(s"Duplicate Automatic-Module-Name values:\n${details.mkString("\n")}")
    }

    names.foreach {
      case (projectName, moduleName) =>
        try ModuleDescriptor.newAutomaticModule(moduleName).build()
        catch {
          case cause: IllegalArgumentException =>
            throw new MessageOnlyException(
              s"Invalid Automatic-Module-Name for $projectName: $moduleName (${cause.getMessage})"
            )
        }
    }
  }

  private def configuredName(projectName: String): Option[String] = {
    names.get(projectName) match {
      case configured @ Some(_)          => configured
      case None if excluded(projectName) => None
      case None                          =>
        throw new MessageOnlyException(s"Missing Automatic-Module-Name for project $projectName")
    }
  }

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    automaticModuleName := configuredName(name.value),
    Compile / packageBin / packageOptions ++= automaticModuleName.value.toSeq.map(moduleName =>
      Package.ManifestAttributes(Header -> moduleName)
    ),
    automaticModuleNameCheck := {
      val projectName = name.value
      val configured  = configuredName(projectName)
      if (automaticModuleName.value != configured) {
        throw new MessageOnlyException(
          s"Automatic-Module-Name setting for $projectName is ${automaticModuleName.value}, expected $configured"
        )
      }
      val expected = configured.toSeq.map(moduleName => Package.ManifestAttributes(Header -> moduleName))
      val actual   = (Compile / packageBin / packageOptions).value.filter(_.toString.contains(s"($Header,"))

      if (actual != expected) {
        throw new MessageOnlyException(
          s"Expected Automatic-Module-Name for $projectName to be ${expected.mkString("[", ", ", "]")}, " +
            s"but found ${actual.mkString("[", ", ", "]")}"
        )
      }
    }
  )
}
