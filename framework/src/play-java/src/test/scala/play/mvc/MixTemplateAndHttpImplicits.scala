/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import play.core.j.PlayMagicForJava._
import play.mvc.Http.Context.Implicit._

/**
 * Test for GH #8866, just to makes sure it compiles
 */
object MixTemplateAndHttpImplicits {
  def apply() {
    flash
    session
    lang
  }
}
