/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import play.api.mvc.macros.BinderMacros

trait PathBindableMacros {
  import scala.language.experimental.macros

  /**
   * Path binder for AnyVal
   */
  implicit def anyValPathBindable[T <: AnyVal]: PathBindable[T] =
    macro BinderMacros.anyValPathBindable[T]
}
