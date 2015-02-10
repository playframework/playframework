/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing

import java.net.{ URL, URI }
import java.util.regex.Pattern

import play.api.mvc.RequestHeader
import play.utils.UriEncoding

import scala.collection.concurrent.TrieMap
import scala.util.matching.Regex

/**
 * The Play "String Interpolating Router DSL", sird for short.
 *
 * Example usage:
 *
 * {{{
 *   import play.api.routing.sird._
 *
 *
 * }}}
 */
package object sird {

  // Memoizes all the routes, so that the route doesn't have to be parsed, and the resulting regex compiled,
  // on each invocation.
  // There is a possible memory leak here, especially if RouteContext is instantiated dynamically. But,
  // under normal usage, there will only be as many entries in this cache as there are usages of this
  // string interpolator in code - even in a very dynamic classloading environment with many different
  // strings being interpolated, the chances of this cache every causing an out of memory error are very
  // low.
  private val pathContextCache = TrieMap.empty[StringContext, PathExtractor]

  /**
   * A path part descriptor. Describes whether the path part should be decoded, or left as is.
   */
  private object PathPart extends Enumeration {
    val Decoded, Raw = Value
  }

  /**
   * The path extractor.
   *
   * Supported data types that can be extracted from:
   *   - play.api.mvc.RequestHeader
   *   - String
   *   - java.net.URI
   *   - java.net.URL
   *
   * @param regex The regex that is used to extract the raw parts.
   * @param partDescriptors Descriptors saying whether each part should be decoded or not.
   */
  class PathExtractor(regex: Regex, partDescriptors: Seq[PathPart.Value]) {
    def unapplySeq(obj: Any): Option[List[String]] = {
      obj match {
        case path: String => extract(path)
        case request: RequestHeader => extract(request.path)
        case uri: URI if uri.getRawPath != null => extract(uri.getRawPath)
        case url: URL if url.getPath != null => extract(url.getPath)
        case _ => None
      }
    }

    private def extract(path: String): Option[List[String]] = {
      regex.unapplySeq(path).map { parts =>
        parts.zip(partDescriptors).map {
          case (part, PathPart.Decoded) => UriEncoding.decodePathSegment(part, "utf-8")
          case (part, PathPart.Raw) => part
        }
      }
    }
  }

  /**
   * String interpolator for extracting parameters out of URL paths.
   *
   * By default, any sub value extracted out by the interpolator will match a path segment, that is, any
   * String not containing a /, and its value will be decoded.  If however the sub value is suffixed with *,
   * then it will match any part of a path, and not be decoded.  Regular expressions are also supported, by
   * suffixing the sub value with a regular expression in angled brackets, and these are not decoded.
   */
  implicit class PathContext(sc: StringContext) {
    val p: PathExtractor = pathContextCache.getOrElseUpdate(sc, {

      // "parse" the path
      val (regexParts, descs) = sc.parts.tail.map { part =>

        if (part.startsWith("*")) {
          // It's a .* matcher
          "(.*)" + Pattern.quote(part.drop(1)) -> PathPart.Raw

        } else if (part.startsWith("<") && part.contains(">")) {
          // It's a regex matcher
          val splitted = part.split(">", 2)
          val regex = splitted(0).drop(1)
          "(" + regex + ")" + Pattern.quote(splitted(1)) -> PathPart.Raw

        } else {
          // It's an ordinary path part matcher
          "([^/]*)" + Pattern.quote(part) -> PathPart.Decoded
        }
      }.unzip

      new PathExtractor(regexParts.mkString(Pattern.quote(sc.parts.head), "", "/?").r, descs)
    })
  }

  /**
   * An extractor that extracts requests by method.
   */
  class RequestMethodExtractor private[sird] (method: String) {
    def unapply(request: RequestHeader): Option[RequestHeader] =
      Some(request).filter(_.method.equalsIgnoreCase(method))
  }

  /**
   * Extracts a GET request.
   */
  val GET = new RequestMethodExtractor("GET")

  /**
   * Extracts a POST request.
   */
  val POST = new RequestMethodExtractor("POST")

  /**
   * Extracts a PUT request.
   */
  val PUT = new RequestMethodExtractor("PUT")

  /**
   * Extracts a DELETE request.
   */
  val DELETE = new RequestMethodExtractor("DELETE")

  /**
   * Extracts a PATCH request.
   */
  val PATCH = new RequestMethodExtractor("PATCH")

  /**
   * Extracts an OPTIONS request.
   */
  val OPTIONS = new RequestMethodExtractor("OPTIONS")

  /**
   * Extracts a HEAD request.
   */
  val HEAD = new RequestMethodExtractor("HEAD")

}
