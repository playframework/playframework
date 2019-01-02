/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import play.core.j.PlayMagicForJava._

object ImplicitMessagesInclude {
  def apply()(implicit messages: play.api.i18n.MessagesProvider): String = {
    messages.messages.apply("bye")
  }
}
