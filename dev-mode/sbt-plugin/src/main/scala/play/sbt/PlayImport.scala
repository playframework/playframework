/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._

import play.dev.filewatch.FileWatchService

/**
 * Declares the default imports for Play plugins.
 */
object PlayImport {
  val Production = config("production")

  def component(id: String) = "com.typesafe.play" %% id % play.core.PlayVersion.current

  def movedExternal(msg: String): ModuleID = {
    System.err.println(msg)
    class ComponentExternalisedException extends RuntimeException(msg) with FeedbackProvidedException
    throw new ComponentExternalisedException
  }

  val playCore = component("play")

  val nettyServer = component("play-netty-server")

  val akkaHttpServer = component("play-akka-http-server")

  val logback = component("play-logback")

  val jodaForms = component("play-joda-forms")

  val guice = component("play-guice")

  val ws = component("play-ahc-ws")

  object PlayKeys {
    val playDefaultPort    = SettingKey[Int]("playDefaultPort", "The default port that Play runs on")
    val playDefaultAddress = SettingKey[String]("playDefaultAddress", "The default address that Play runs on")

    val externalizeResources = SettingKey[Boolean](
      "playExternalizeResources",
      "Whether resources should be externalized into the conf directory when Play is packaged as a distribution."
    )
    val playExternalizedResources =
      TaskKey[Seq[(File, String)]]("playExternalizedResources", "The resources to externalize")
    val externalizeResourcesExcludes = SettingKey[Seq[File]](
      "externalizeResourcesExcludes",
      "Resources that should not be externalized but stay in the generated jar"
    )
    val playJarSansExternalized =
      TaskKey[File]("playJarSansExternalized", "Creates a jar file that has all the externalized resources excluded")

    val playPlugin = SettingKey[Boolean]("playPlugin")

    val devSettings = SettingKey[Seq[(String, String)]]("playDevSettings")

    val assetsPrefix      = SettingKey[String]("assetsPrefix")
  }
}
