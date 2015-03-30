/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing.sird

import java.net.{ URL, URI }
import java.util.regex.Pattern

import play.api.mvc.RequestHeader
import play.utils.UriEncoding

import scala.collection.concurrent.TrieMap
import scala.util.matching.Regex

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
  def unapplySeq(path: String): Option[List[String]] = extract(path)
  def unapplySeq(request: RequestHeader): Option[List[String]] = extract(request.path)
  def unapplySeq(url: URL): Option[List[String]] = Option(url.getPath).flatMap(extract)
  def unapplySeq(uri: URI): Option[List[String]] = Option(uri.getRawPath).flatMap(extract)

  private def extract(path: String): Option[List[String]] = {
    regex.unapplySeq(path).map { parts =>
      parts.zip(partDescriptors).map {
        case (part, PathPart.Decoded) => UriEncoding.decodePathSegment(part, "utf-8")
        case (part, PathPart.Raw) => part
      }
    }
  }
}

object PathExtractor {
  // Memoizes all the routes, so that the route doesn't have to be parsed, and the resulting regex compiled,
  // on each invocation.
  // There is a possible memory leak here, especially if RouteContext is instantiated dynamically. But,
  // under normal usage, there will only be as many entries in this cache as there are usages of this
  // string interpolator in code - even in a very dynamic classloading environment with many different
  // strings being interpolated, the chances of this cache every causing an out of memory error are very
  // low.
  private val cache = TrieMap.empty[Seq[String], PathExtractor]

  /**
   * Lookup the PathExtractor from the cache, or create and store a new one if not found.
   */
  def cached(parts: Seq[String]): PathExtractor = {
    cache.getOrElseUpdate(parts, {

      // "parse" the path
      val (regexParts, descs) = parts.tail.map { part =>

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

      new PathExtractor(regexParts.mkString(Pattern.quote(parts.head), "", "/?").r, descs)
    })
  }
}

/**
 * A path part descriptor. Describes whether the path part should be decoded, or left as is.
 */
private object PathPart extends Enumeration {
  val Decoded, Raw = Value
}