/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing
import scala.language.experimental.macros

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
package object sird extends RequestMethodExtractors with PathBindableExtractors with SirdMacros {

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
