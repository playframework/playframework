/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.docs.sbtplugin

private[sbtplugin] object Version {
  private val versionRegex = """(\d+)\.(\d+)\.(\d+)(-\S+)?""".r
  def from(version: String): Version = version match {
    case versionRegex(era, major, minor, qualifier) =>
      // if qualifier is not null, drop the leading "-"
      val qual = Option(qualifier).map(_.tail)
      Version(era.toInt, major.toInt, minor.toInt, qual)
    case _ => throw new IllegalArgumentException(s"$version is not a valid version string")
  }
}
private[sbtplugin] case class Version(era: Int, major: Int, minor: Int, qualifier: Option[String])
