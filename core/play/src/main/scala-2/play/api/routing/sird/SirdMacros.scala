/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing.sird

import scala.language.experimental.macros

trait SirdMacros {

  implicit class UrlContext(val sc: StringContext) {

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

    /**
     * String interpolator for extracting parameters out of URL paths.
     *
     * By default, any sub value extracted out by the interpolator will match a path segment, that is, any
     * String not containing a /, and its value will be decoded.  If however the sub value is suffixed with *,
     * then it will match any part of a path, and not be decoded.  Regular expressions are also supported, by
     * suffixing the sub value with a regular expression in angled brackets, and these are not decoded.
     */
    val p: PathExtractor = PathExtractor.cached(sc.parts)
  }
}
