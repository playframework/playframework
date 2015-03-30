/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package object play {

  @deprecated("Use play.sbt.Play instead", "2.4.0")
  val Play = sbt.Play
  @deprecated("Use play.sbt.Play instead", "2.4.0")
  val PlayJava = sbt.PlayJava
  @deprecated("Use play.sbt.Play instead", "2.4.0")
  val PlayScala = sbt.PlayScala

  @deprecated("Use play.sbt.PlayInteractionMode instead", "2.4.0")
  type PlayInteractionMode = sbt.PlayInteractionMode
  @deprecated("Use play.sbt.PlayConsoleInteractionMode instead", "2.4.0")
  val PlayConsoleInteractionMode = sbt.PlayConsoleInteractionMode

  @deprecated("Use play.sbt.PlayImport instead", "2.4.0")
  val PlayImport = sbt.PlayImport
  @deprecated("Use play.sbt.PlayInternalKeys instead", "2.4.0")
  val PlayInternalKeys = sbt.PlayInternalKeys

  @deprecated("Use play.sbt.PlayRunHook instead", "2.4.0")
  type PlayRunHook = sbt.PlayRunHook
  @deprecated("Use play.sbt.PlayRunHook instead", "2.4.0")
  val PlayRunHook = sbt.PlayRunHook

}
