/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

/**
 * Application mode, either `Dev`, `Test`, or `Prod`.
 *
 * @see [[play.Mode]]
 */
sealed abstract class Mode(val asJava: play.Mode)

object Mode {
  case object Dev  extends Mode(play.Mode.DEV)
  case object Test extends Mode(play.Mode.TEST)
  case object Prod extends Mode(play.Mode.PROD)

  lazy val values: Set[play.api.Mode] = Set(Dev, Test, Prod)
}
