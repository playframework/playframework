/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import play.core.j.PlayMagicForJava._

object ImplicitRequestInclude {
  def apply()(implicit request: play.api.mvc.RequestHeader): String = {
    request.cookies("location").value
  }
}
