/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.routing

import java.net.URI

import scala.util.control.Exception

/**
 * A part of a path.
 */
trait PathPart

/**
 * A dynamically extracted part of the path.
 *
 * @param name The name of the part.
 * @param constraint The constraint - that is, the type.
 * @param encodeable Whether the path should be encoded/decoded.
 */
case class DynamicPart(name: String, constraint: String, encodeable: Boolean) extends PathPart {
  override def toString = """DynamicPart("""" + name + "\", \"\"\"" + constraint + "\"\"\")" // "
}

/**
 * A static part of the path.
 */
case class StaticPart(value: String) extends PathPart {
  override def toString = """StaticPart("""" + value + """")"""
}

/**
 * A pattern for match paths, consisting of a sequence of path parts.
 */
case class PathPattern(parts: Seq[PathPart]) {

  import java.util.regex._

  private def decodeIfEncoded(decode: Boolean, groupCount: Int): Matcher => Either[Throwable, String] = matcher =>
    Exception.allCatch[String].either {
      if (decode) {
        val group = matcher.group(groupCount)
        // If param is not correctly encoded, get path will return null, so we prepend a / to it
        new URI("/" + group).getPath.drop(1)
      } else
        matcher.group(groupCount)
    }

  private lazy val (regex, groups) = {
    Some(parts.foldLeft("", Map.empty[String, Matcher => Either[Throwable, String]], 0) { (s, e) =>
      e match {
        case StaticPart(p) => ((s._1 + Pattern.quote(p)), s._2, s._3)
        case DynamicPart(k, r, encodeable) => {
          ((s._1 + "(" + r + ")"),
            (s._2 + (k -> decodeIfEncoded(encodeable, s._3 + 1))),
            s._3 + 1 + Pattern.compile(r).matcher("").groupCount)
        }
      }
    }).map {
      case (r, g, _) => Pattern.compile("^" + r + "$") -> g
    }.get
  }

  /**
   * Apply the path pattern to a given candidate path to see if it matches.
   *
   * @param path The path to match against.
   * @return The map of extracted parameters, or none if the path didn't match.
   */
  def apply(path: String): Option[Map[String, Either[Throwable, String]]] = {
    val matcher = regex.matcher(path)
    if (matcher.matches) {
      Some(groups.map {
        case (name, g) => name -> g(matcher)
      }.toMap)
    } else {
      None
    }
  }

  override def toString = parts.map {
    case DynamicPart(name, constraint, _) => "$" + name + "<" + constraint + ">"
    case StaticPart(path) => path
  }.mkString

}
