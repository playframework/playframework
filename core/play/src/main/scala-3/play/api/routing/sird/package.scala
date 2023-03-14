/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing

/**
 * The Play "String Interpolating Routing DSL", sird for short.
 *
 * This provides:
 * - Extractors for requests that extract requests by method, eg GET, POST etc.
 * - A string interpolating path extractor
 * - Extractors for binding parameters from paths to various types, eg int, long, double, bool.
 *
 * The request method extractors return the original request for further extraction.
 *
 * The path extractor supports three kinds of extracted values:
 * - Path segment values. This is the default, eg `p"/foo/\$id"`. The value will be URI decoded, and may not traverse /'s.
 * - Full path values. This can be indicated by post fixing the value with a *, eg `p"/assets/\$path*"`. The value will
 *   not be URI decoded, as that will make it impossible to distinguish between / and %2F.
 * - Regex values. This can be indicated by post fixing the value with a regular expression enclosed in angle brackets.
 *   For example, `p"/foo/\$id<[0-9]+>`. The value will not be URI decoded.
 *
 * The extractors for primitive types are merely provided for convenience, for example, `p"/foo/\${int(id)}"` will
 * extract `id` as an integer.  If `id` is not an integer, the match will simply fail.
 *
 * Example usage:
 *
 * {{{
 *  import play.api.routing.sird._
 *  import play.api.routing._
 *  import play.api.mvc._
 *
 *  Router.from {
 *    case GET(p"/hello/\$to") => Action {
 *      Results.Ok(s"Hello \$to")
 *    }
 *    case PUT(p"/api/items/\${int(id)}") => Action.async { req =>
 *      Items.save(id, req.body.json.as[Item]).map { _ =>
 *        Results.Ok(s"Saved item \$id")
 *      }
 *    }
 *  }
 * }}}
 */
package object sird extends RequestMethodExtractors with PathBindableExtractors {
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

  extension (inline sc: StringContext) {

    /**
     * String interpolator for required query parameters out of query strings.
     *
     * The format must match `q"paramName=\${param}"`.
     */
    inline def q: RequiredQueryStringParameter = ${ macroimpl.QueryStringParameterMacros.required('sc) }

    /**
     * String interpolator for optional query parameters out of query strings.
     *
     * The format must match `q_?"paramName=\${param}"`.
     */
    inline def q_? : OptionalQueryStringParameter = ${ macroimpl.QueryStringParameterMacros.optional('sc) }

    /**
     * String interpolator for multi valued query parameters out of query strings.
     *
     * The format must match `q_*"paramName=\${params}"`.
     */
    inline def q_* : SeqQueryStringParameter = ${ macroimpl.QueryStringParameterMacros.seq('sc) }

    /**
     * String interpolator for optional query parameters out of query strings.
     *
     * The format must match `q_o"paramName=\${param}"`.
     *
     * The `q_?` interpolator is preferred, however Scala 2.10 does not support operator characters in String
     * interpolator methods.
     */
    inline def q_o: OptionalQueryStringParameter = ${ macroimpl.QueryStringParameterMacros.optional('sc) }

    /**
     * String interpolator for multi valued query parameters out of query strings.
     *
     * The format must match `q_s"paramName=\${params}"`.
     *
     * The `q_*` interpolator is preferred, however Scala 2.10 does not support operator characters in String
     * interpolator methods.
     */
    inline def q_s: SeqQueryStringParameter = ${ macroimpl.QueryStringParameterMacros.seq('sc) }
  }

  /**
   * Allow multiple parameters to be extracted
   */
  object & {
    def unapply[A](a: A): Option[(A, A)] =
      Some((a, a))
  }

  /**
   * Same as &, but for convenience to make the dsl look nicer when extracting query strings
   */
  val ? = &

  /**
   * The query string type
   */
  type QueryString = Map[String, Seq[String]]
}
