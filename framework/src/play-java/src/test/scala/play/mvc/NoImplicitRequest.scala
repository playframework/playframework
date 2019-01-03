/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import play.core.j.PlayMagicForJava._

object NoImplicitRequest {
  def apply(request: play.mvc.Http.Request): String = {
    ImplicitRequestInclude()
  }
  def render(request: play.mvc.Http.Request): String = apply(request)
}
