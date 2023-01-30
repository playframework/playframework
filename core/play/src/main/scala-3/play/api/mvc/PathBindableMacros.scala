/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import play.api.mvc.macros.BinderMacros

trait PathBindableMacros {

  /**
   * Path binder for AnyVal
   */
  inline implicit def anyValPathBindable[T <: AnyVal]: PathBindable[T] = ${ BinderMacros.anyValPathBindable }

}
