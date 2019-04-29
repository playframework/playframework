/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import play.core.j.PlayMagicForJava._

object ImplicitMessages {
  def apply(implicit messages: play.i18n.Messages): String = {
    ImplicitMessagesInclude()
  }
  def render(messages: play.i18n.Messages): String = apply(messages)
}
