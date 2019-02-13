/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import play.core.j.PlayMagicForJava._

object ImplicitLang {
  def apply(implicit lang: play.i18n.Lang): String = {
    ImplicitLangInclude()
  }
  def render(lang: play.i18n.Lang): String = apply(lang)
}
