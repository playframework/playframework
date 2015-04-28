/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

import sbt.Keys._
import sbt._
import bintray.BintrayKeys._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.SonatypeKeys._

object PublishSettings {

  private def commonPublishSettings: Seq[Setting[_]] = Seq(
    pomIncludeRepository := { _ => false },
    pomExtra := pomExtraXml,
    bintrayRepository := "sbt-plugin-releases",
    bintrayOrganization := Option("playframework"),
    bintrayPackage := "play-sbt-plugin",
    bintrayReleaseOnPublish := false,
    profileName := "com.typesafe",
    // Do not aggregate the release commands
    aggregate in bintrayRelease := false,
    aggregate in sonatypeRelease := false
  )

  /**
   * We actually must publish when doing a publish-local in order to ensure the scala 2.11 build works, very strange
   * things happen if you set publishArtifact := false, since it still publishes an ivy file when you do a
   * publish-local, but that ivy file says there's no artifacts.
   *
   * So, to disable publishing for the 2.11 build, we simply publish to a dummy repo instead of to the real thing.
   */
  def dontPublishSettings: Seq[Setting[_]] = Sonatype.sonatypeSettings ++ commonPublishSettings ++ Seq(
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

  /**
   * Default publish settings
   */
  def publishSettings: Seq[Setting[_]] = Sonatype.sonatypeSettings ++ commonPublishSettings

  /**
   * Publish settings for SBT plugins.
   *
   * SBT plugins get published to bintray for releases, or sonatype for snapshots.  This is because sonatype doesn't
   * handle sbt plugins well, primarily due to it's indexing, but that's not a problem for snapshots, meanwhile bintray
   * doesn't allow publishing snapshots.
   *
   * Also note, bintray settings are
   */
  def sbtPluginPublishSettings: Seq[Setting[_]] = {
    commonPublishSettings ++ Seq(
      publishTo := {
        if (isSnapshot.value) {
          Some(Opts.resolver.sonatypeSnapshots)
        } else {
          publishTo.value
        }
      },
      publishMavenStyle := !isSnapshot.value
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
