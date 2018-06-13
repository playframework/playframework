/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.Keys._

/**
 * Helper to set Automatic-Module-Name in project manifests.
 *
 * The names are intentionally explicit. They carry compatibility implications and
 * should not be derived mechanically from project names.
 */
object AutomaticModuleName {
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
    "Play-Integration-Test"      -> "org.playframework.integration.test",
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

  def settings(projectName: String): Seq[Def.Setting[Task[Seq[PackageOption]]]] =
    names.get(projectName).toSeq.map { moduleName =>
      Compile / packageBin / packageOptions += Package.ManifestAttributes(Header -> moduleName)
    }
}
