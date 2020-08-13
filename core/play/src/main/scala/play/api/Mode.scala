/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

/**
 * Application mode, either `Dev`, `Test`, or `Prod`.
 *
 * @see [[play.Mode]]
 */
sealed abstract trait Mode

object Mode {
  case object Dev  extends Mode
  case object Test extends Mode
  case object Prod extends Mode

  lazy val values: Set[play.api.Mode] = Set(Dev, Test, Prod)
}
