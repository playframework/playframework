/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.util.Locale

/** A normalized URI scheme. */
final case class Scheme(value: String) {
  require(Scheme.isNormalized(value), "A scheme must be valid RFC 3986 syntax normalized to lower case")

  /** Whether this is the HTTPS scheme. */
  def isSecure: Boolean = this == Scheme.Https

  /** Render this scheme in its normalized form. */
  def render: String = value

  /** Convert this scheme to the Java API. */
  def asJava: play.mvc.Http.Scheme = new play.mvc.Http.Scheme(value)

  override def toString: String = render
}

object Scheme {
  val Http: Scheme  = new Scheme("http")
  val Https: Scheme = new Scheme("https")

  /** Create a normalized scheme or throw when the value is invalid. */
  def create(value: String): Scheme = parseOrThrow(value)

  /**
   * Parse an RFC 3986 scheme and normalize it to lower case.
   *
   * A scheme starts with an ASCII letter and may then contain ASCII letters,
   * digits, `+`, `-`, or `.`.
   */
  def parse(value: String): Either[String, Scheme] = {
    if (value == null) Left("Invalid scheme: null")
    else if (!isValid(value)) Left(s"Invalid scheme: '$value'")
    else {
      value.toLowerCase(Locale.ROOT) match {
        case "http"  => Right(Http)
        case "https" => Right(Https)
        case scheme  => Right(new Scheme(scheme))
      }
    }
  }

  /** Parse a normalized scheme, throwing an `IllegalArgumentException` when it is invalid. */
  def parseOrThrow(value: String): Scheme =
    parse(value).fold(error => throw new IllegalArgumentException(error), identity)

  private def isNormalized(value: String): Boolean =
    value != null && isValid(value) && value == value.toLowerCase(Locale.ROOT)

  private def isValid(value: String): Boolean = {
    value.nonEmpty && isAlpha(value.charAt(0)) && value.substring(1).forall { char =>
      isAlpha(char) || isDigit(char) || char == '+' || char == '-' || char == '.'
    }
  }

  private def isAlpha(char: Char): Boolean =
    (char >= 'A' && char <= 'Z') || (char >= 'a' && char <= 'z')

  private def isDigit(char: Char): Boolean = char >= '0' && char <= '9'
}
