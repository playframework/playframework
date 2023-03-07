/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import play.api.mvc.macros.BinderMacros

trait QueryStringBindableMacros {
  import scala.language.experimental.macros

  implicit def anyValQueryStringBindable[T <: AnyVal]: QueryStringBindable[T] =
    macro BinderMacros.anyValQueryStringBindable[T]
}
