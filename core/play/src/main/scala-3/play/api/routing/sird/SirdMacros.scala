/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing.sird

import scala.quoted._

trait SirdMacros {
  extension (inline sc: StringContext) {

    /**
     * String interpolator for required query parameters out of query strings.
     *
     * The format must match `q"paramName=\${param}"`.
     */
    inline def q: RequiredQueryStringParameter = ${macroimpl.QueryStringParameterMacros.required('sc)}

    /**
     * String interpolator for optional query parameters out of query strings.
     *
     * The format must match `q_?"paramName=\${param}"`.
     */
    inline def q_? : OptionalQueryStringParameter = ${macroimpl.QueryStringParameterMacros.optional('sc)}

    /**
     * String interpolator for multi valued query parameters out of query strings.
     *
     * The format must match `q_*"paramName=\${params}"`.
     */
    inline def q_* : SeqQueryStringParameter = ${macroimpl.QueryStringParameterMacros.seq('sc)}

    /**
     * String interpolator for optional query parameters out of query strings.
     *
     * The format must match `qo"paramName=\${param}"`.
     *
     * The `q_?` interpolator is preferred, however Scala 2.10 does not support operator characters in String
     * interpolator methods.
     */
    inline def q_o: OptionalQueryStringParameter = ${macroimpl.QueryStringParameterMacros.optional('sc)}

    /**
     * String interpolator for multi valued query parameters out of query strings.
     *
     * The format must match `qs"paramName=\${params}"`.
     *
     * The `q_*` interpolator is preferred, however Scala 2.10 does not support operator characters in String
     * interpolator methods.
     */
    inline def q_s: SeqQueryStringParameter = ${macroimpl.QueryStringParameterMacros.seq('sc)}
  }

  implicit class UrlContext(val sc: StringContext) {
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
