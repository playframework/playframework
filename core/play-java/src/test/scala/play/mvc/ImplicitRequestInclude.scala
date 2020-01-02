/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import play.core.j.PlayMagicForJava._

object ImplicitRequestInclude {
  def apply()(implicit request: play.api.mvc.RequestHeader): String = {
    request.cookies("location").value
  }
}
