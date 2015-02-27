/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

import sbt.Keys._
import sbt._

/**
 * Any resolvers that we may be interested in
 */
object ResolverSettings {
  val typesafeReleases = "Typesafe Releases Repository" at "https://repo.typesafe.com/typesafe/releases/"
  val typesafeSnapshots = "Typesafe Snapshots Repository" at "https://repo.typesafe.com/typesafe/snapshots/"
  val typesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
  val typesafeIvySnapshots = Resolver.url("Typesafe Ivy Snapshots Repository", url("https://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns)
  val publishTypesafeMavenReleases = "Typesafe Maven Releases Repository for publishing" at "https://private-repo.typesafe.com/typesafe/maven-releases/"
  val publishTypesafeMavenSnapshots = "Typesafe Maven Snapshots Repository for publishing" at "https://private-repo.typesafe.com/typesafe/maven-snapshots/"
  val publishTypesafeIvyReleases = Resolver.url("Typesafe Ivy Releases Repository for publishing", url("https://private-repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
  val publishTypesafeIvySnapshots = Resolver.url("Typesafe Ivy Snapshots Repository for publishing", url("https://private-repo.typesafe.com/typesafe/ivy-snapshots/"))(Resolver.ivyStylePatterns)

  val publishSonatypeReleases = "Sonatype Maven Repository for publishing" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  val publishSonatypeSnapshots = "Sonatype Maven Snapshots Repository for publishing" at "https://oss.sonatype.org/content/repositories/snapshots"

  val sonatypeSnapshots = "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  val sbtPluginSnapshots = Resolver.url("sbt plugin snapshots", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)

  val playResolvers = Seq(typesafeReleases, typesafeIvyReleases)
}

object PublishSettings {

  private def commonPublishSettings: Seq[Setting[_]] = Seq(
    pomIncludeRepository := { _ => false },
    pomExtra := pomExtraXml

  )

  /**
   * We actually must publish when doing a publish-local in order to ensure the scala 2.11 build works, very strange
   * things happen if you set publishArtifact := false, since it still publishes an ivy file when you do a
   * publish-local, but that ivy file says there's no artifacts.
   *
   * So, to disable publishing for the 2.11 build, we simply publish to a dummy repo instead of to the real thing.
   */
  def dontPublishSettings: Seq[Setting[_]] = Seq(
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

  /**
   * Default publish settings
   */
  def publishSettings: Seq[Setting[_]] = commonPublishSettings ++ Seq(
    publishTo := {
      Some(if (isSnapshot.value) {
        ResolverSettings.publishSonatypeSnapshots
      } else {
        ResolverSettings.publishSonatypeReleases
      })
    },
    publishMavenStyle := true
  )

  /**
   * Publish settings for SBT plugins
   */
  def sbtPluginPublishSettings: Seq[Setting[_]] = {
    commonPublishSettings ++ Seq(
      publishTo := {
        Some(if (isSnapshot.value) {
          ResolverSettings.publishTypesafeIvySnapshots
        } else {
          ResolverSettings.publishTypesafeIvyReleases
        })
      },
      publishMavenStyle := false
    )
  }

  private val pomExtraXml =
    <scm>
      <url>git@github.com:playframework/playframework.git</url>
      <connection>scm:git:git@github.com:playframework/playframework.git</connection>
    </scm> ++ makeDevelopersXml(
      ("guillaumebort", "Guillaume Bort", "http://guillaume.bort.fr"),
      ("pk11", "Peter Hausel", "https://twitter.com/pk11"),
      ("sadache", "Sadek Drobi", "http://sadache.tumblr.com"),
      ("erwan", "Erwan Loisant", "http://caffeinelab.net/"),
      ("jroper", "James Roper", "https://jazzy.id.au"),
      ("huntc", "Christopher Hunt", "http://christopherhunt-software.blogspot.com.au/"),
      ("richdougherty", "Rich Dougherty", "http://www.richdougherty.com/"),
      ("pvlugter", "Peter Vlugter", "https://github.com/pvlugter"),
      ("wsargent", "Will Sargent", "https://github.com/wsargent"),
      ("julienrf", "Julien Richard-Foy", "http://julien.richard-foy.fr"),
      ("baloo", "Arthur Gautier", "https://twitter.com/baloose"),
      ("cchantep", "Cédric Chantepie", "https://twitter.com/cchantep"),
      ("benmccann", "Ben McCann", "http://www.benmccann.com"),
      ("mandubian", "Pascal Voitot", "http://www.mandubian.com"),
      ("nraychaudhuri", "Nilanjan Raychaudhuri", "http://www.manning.com/raychaudhuri/"),
      ("gmethvin", "Greg Methvin", "http://methvin.net")
    )


  private def makeDevelopersXml(developers: (String, String, String)*) =
    <developers>
      {
      for ((id, name, url) <- developers) yield
        <developer>
          <id>{id}</id>
          <name>{name}</name>
          <url>{url}</url>
        </developer>
      }
    </developers>
}
