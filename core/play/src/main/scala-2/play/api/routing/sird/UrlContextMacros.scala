/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing.sird

import scala.language.experimental.macros

trait UrlContextMacros {

  /**
   * String interpolator for required query parameters out of query strings.
   *
   * The format must match `q"paramName=\${param}"`.
   */
  def q: RequiredQueryStringParameter = macro macroimpl.QueryStringParameterMacros.required

  /**
   * String interpolator for optional query parameters out of query strings.
   *
   * The format must match `q_?"paramName=\${param}"`.
   */
  def q_? : OptionalQueryStringParameter = macro macroimpl.QueryStringParameterMacros.optional

  /**
   * String interpolator for multi valued query parameters out of query strings.
   *
   * The format must match `q_*"paramName=\${params}"`.
   */
  def q_* : SeqQueryStringParameter = macro macroimpl.QueryStringParameterMacros.seq

  /**
   * String interpolator for optional query parameters out of query strings.
   *
   * The format must match `qo"paramName=\${param}"`.
   *
   * The `q_?` interpolator is preferred, however Scala 2.10 does not support operator characters in String
   * interpolator methods.
   */
  def q_o: OptionalQueryStringParameter = macro macroimpl.QueryStringParameterMacros.optional

  /**
   * String interpolator for multi valued query parameters out of query strings.
   *
   * The format must match `qs"paramName=\${params}"`.
   *
   * The `q_*` interpolator is preferred, however Scala 2.10 does not support operator characters in String
   * interpolator methods.
   */
  def q_s: SeqQueryStringParameter = macro macroimpl.QueryStringParameterMacros.seq
}
