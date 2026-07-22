/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.Inet4Address
import java.net.Inet6Address
import java.util.Locale
import java.util.Optional

import scala.jdk.javaapi.OptionConverters

import com.google.common.net.InetAddresses

/** A normalized URI host. */
sealed trait AuthorityHost {

  /** Render this host in its normalized form, including brackets around IP literals. */
  def render: String

  /** Convert this host to the Java API. */
  def asJava: play.mvc.Http.AuthorityHost
}

object AuthorityHost {

  /** A registered name, including the empty registered name allowed by RFC 3986. */
  final case class RegName(value: String) extends AuthorityHost {
    require(
      RequestAuthority.normalizeRegName(value).contains(value),
      "A registered name must be valid RFC 3986 syntax in normalized form"
    )

    override def render: String = value

    override def asJava: play.mvc.Http.AuthorityHost = new play.mvc.Http.AuthorityHost.RegName(value)
  }

  /** An IPv4 address literal. */
  final case class IPv4(address: Inet4Address) extends AuthorityHost {
    require(address != null, "An IPv4 authority host must not be null")

    override val render: String = InetAddresses.toAddrString(address)

    override def asJava: play.mvc.Http.AuthorityHost = new play.mvc.Http.AuthorityHost.IPv4(address)
  }

  /** An IPv6 address literal. */
  final case class IPv6(address: Inet6Address) extends AuthorityHost {
    require(address != null, "An IPv6 authority host must not be null")
    require(!IpAddressSyntax.hasScope(address), "An IPv6 authority host must not have a scope identifier")

    override val render: String = s"[${InetAddresses.toAddrString(address)}]"

    override def asJava: play.mvc.Http.AuthorityHost = new play.mvc.Http.AuthorityHost.IPv6(address)
  }

  /** An RFC 3986 IPvFuture address literal. */
  final case class IPvFuture(value: String) extends AuthorityHost {
    require(
      RequestAuthority.normalizeIpvFuture(value).contains(value),
      "An IPvFuture authority host must be valid RFC 3986 syntax in normalized form"
    )

    override val render: String = s"[$value]"

    override def asJava: play.mvc.Http.AuthorityHost = new play.mvc.Http.AuthorityHost.IPvFuture(value)
  }

  /** Create and normalize an RFC 3986 registered name. */
  def regName(value: String): RegName =
    RequestAuthority
      .normalizeRegName(value)
      .map(RegName.apply)
      .getOrElse(throw new IllegalArgumentException(s"Invalid registered name: '$value'"))

  /** Create an IPv4 authority host from an already parsed address. */
  def ipv4(address: Inet4Address): IPv4 = IPv4(address)

  /** Parse an IPv4 authority host without performing DNS resolution. */
  def ipv4(value: String): IPv4 =
    IpAddressSyntax
      .parseIpv4(value)
      .map(IPv4.apply)
      .getOrElse(throw new IllegalArgumentException(s"Invalid IPv4 address: '$value'"))

  /** Create an IPv6 authority host from an already parsed address. */
  def ipv6(address: Inet6Address): IPv6 = IPv6(address)

  /** Parse an IPv6 authority host without performing DNS resolution. */
  def ipv6(value: String): IPv6 =
    IpAddressSyntax
      .parseIpv6(value)
      .map(IPv6.apply)
      .getOrElse(throw new IllegalArgumentException(s"Invalid IPv6 address: '$value'"))

  /** Create and normalize an IPvFuture authority host without surrounding brackets. */
  def ipvFuture(value: String): IPvFuture =
    RequestAuthority
      .normalizeIpvFuture(value)
      .map(IPvFuture.apply)
      .getOrElse(throw new IllegalArgumentException(s"Invalid IPvFuture address: '$value'"))
}

/** A URI port, whose grammar is an arbitrary non-negative decimal integer. */
final case class AuthorityPort(value: BigInt) {
  require(value != null && value >= 0, "An authority port must not be null or negative")

  /** Project this value to a TCP/UDP port when it is in the 0 through 65535 range. */
  def tcpPort: Option[Int] = Option.when(value <= AuthorityPort.MaxTcpPort)(value.toInt)

  /** Render this port without leading zeroes. */
  val render: String = value.toString

  /** Convert this port to the Java API. */
  def asJava: play.mvc.Http.AuthorityPort = new play.mvc.Http.AuthorityPort(value.bigInteger)

  override def toString: String = render
}

object AuthorityPort {
  private val MaxTcpPort = BigInt(65535)

  /** Parse an arbitrary non-negative decimal URI port. */
  def parse(value: String): Either[String, AuthorityPort] = {
    if (value == null) Left("Invalid authority port: null")
    else if (value.isEmpty || !value.forall(isDigit)) Left(s"Invalid authority port: '$value'")
    else Right(AuthorityPort(BigInt(value)))
  }

  /** Parse an authority port, throwing an `IllegalArgumentException` when it is invalid. */
  def parseOrThrow(value: String): AuthorityPort =
    parse(value).fold(error => throw new IllegalArgumentException(error), identity)

  /** Create an authority port from a Java arbitrary-precision integer. */
  def create(value: java.math.BigInteger): AuthorityPort = {
    require(value != null, "An authority port must not be null")
    AuthorityPort(BigInt(value))
  }

  private def isDigit(char: Char): Boolean = char >= '0' && char <= '9'
}

/** A normalized destination authority consisting of a typed host and optional port. */
final case class RequestAuthority(host: AuthorityHost, port: Option[AuthorityPort]) {
  require(host != null, "An authority host must not be null")
  require(port != null && port.forall(_ != null), "An authority port option must not be null or contain null")

  /** Render this authority in its normalized form. */
  val render: String = port.fold(host.render)(value => s"${host.render}:${value.render}")

  /** Return a copy using the given port. */
  def withPort(port: Option[AuthorityPort]): RequestAuthority = copy(port = port)

  /** Return a copy using the given port. */
  def withPort(port: AuthorityPort): RequestAuthority = copy(port = Some(port))

  /** Convert this authority to the Java API. */
  def asJava: play.mvc.Http.RequestAuthority =
    new play.mvc.Http.RequestAuthority(host.asJava, OptionConverters.toJava(port.map(_.asJava)))

  override def toString: String = render
}

object RequestAuthority {

  /** Create an authority from typed Scala values. */
  def create(host: AuthorityHost, port: Option[AuthorityPort]): RequestAuthority = RequestAuthority(host, port)

  /** Create an authority from typed values and a Java optional port. */
  def create(host: AuthorityHost, port: Optional[AuthorityPort]): RequestAuthority = {
    require(port != null, "An authority port optional must not be null")
    RequestAuthority(host, OptionConverters.toScala(port))
  }

  /**
   * Parse `host [ ":" port ]` using the RFC 3986 `host` and `port` grammar.
   *
   * An empty textual port is represented as no port. Host classification is
   * entirely syntactic and never performs DNS resolution.
   */
  def parse(value: String): Either[String, RequestAuthority] = {
    if (value == null) Left("Invalid authority: null")
    else if (value.startsWith("[")) parseIpLiteralAuthority(value)
    else parseRegisteredOrIpv4Authority(value)
  }

  /** Parse an authority, throwing an `IllegalArgumentException` when it is invalid. */
  def parseOrThrow(value: String): RequestAuthority =
    parse(value).fold(error => throw new IllegalArgumentException(error), identity)

  private def parseIpLiteralAuthority(value: String): Either[String, RequestAuthority] = {
    val closingBracket = value.indexOf(']')
    if (closingBracket <= 1) invalid(value, "invalid or missing IP-literal brackets")
    else {
      val literal = value.substring(1, closingBracket)
      val suffix  = value.substring(closingBracket + 1)

      for {
        host <- parseIpLiteral(literal).toRight(error(value, "invalid IP literal"))
        port <- parsePortSuffix(value, suffix)
      } yield RequestAuthority(host, port)
    }
  }

  private def parseRegisteredOrIpv4Authority(value: String): Either[String, RequestAuthority] = {
    val colon               = value.indexOf(':')
    val (hostValue, suffix) =
      if (colon < 0) (value, "")
      else (value.substring(0, colon), value.substring(colon))

    if (colon >= 0 && value.indexOf(':', colon + 1) >= 0) {
      invalid(value, "an IPv6 or IPvFuture literal must be enclosed in brackets")
    } else {
      for {
        host <- parseIpv4(hostValue)
          .orElse(parseRegName(hostValue))
          .toRight(error(value, "invalid registered name or IPv4 address"))
        port <- parsePortSuffix(value, suffix)
      } yield RequestAuthority(host, port)
    }
  }

  private def parseIpLiteral(value: String): Option[AuthorityHost] = {
    parseIpv6(value).orElse(parseIpvFuture(value))
  }

  private def parseIpv4(value: String): Option[AuthorityHost.IPv4] = {
    IpAddressSyntax.parseIpv4(value).map(AuthorityHost.IPv4.apply)
  }

  private def parseIpv6(value: String): Option[AuthorityHost.IPv6] = {
    IpAddressSyntax.parseIpv6(value).map(AuthorityHost.IPv6.apply)
  }

  private def parseIpvFuture(value: String): Option[AuthorityHost.IPvFuture] =
    normalizeIpvFuture(value).map(AuthorityHost.IPvFuture.apply)

  private[request] def normalizeIpvFuture(value: String): Option[String] = {
    if (value == null || value.length < 4 || (value.charAt(0) != 'v' && value.charAt(0) != 'V')) None
    else {
      val dot = value.indexOf('.')
      Option.when(
        dot > 1 &&
          value.substring(1, dot).forall(isHexDigit) &&
          dot + 1 < value.length &&
          value.substring(dot + 1).forall(char => isUnreserved(char) || isSubDelimiter(char) || char == ':')
      )(value.toLowerCase(Locale.ROOT))
    }
  }

  private def parseRegName(value: String): Option[AuthorityHost.RegName] =
    normalizeRegName(value).map(AuthorityHost.RegName.apply)

  private[request] def normalizeRegName(value: String): Option[String] = {
    if (value == null) return None

    val normalized = new StringBuilder(value.length)
    var index      = 0
    while (index < value.length) {
      val char = value.charAt(index)
      if (isUnreserved(char) || isSubDelimiter(char)) {
        normalized.append(toLowerAscii(char))
        index += 1
      } else if (
        char == '%' &&
        index + 2 < value.length &&
        isHexDigit(value.charAt(index + 1)) &&
        isHexDigit(value.charAt(index + 2))
      ) {
        val decoded = Integer.parseInt(value.substring(index + 1, index + 3), 16).toChar
        if (isUnreserved(decoded)) normalized.append(toLowerAscii(decoded))
        else {
          normalized.append('%')
          normalized.append(value.charAt(index + 1).toUpper)
          normalized.append(value.charAt(index + 2).toUpper)
        }
        index += 3
      } else {
        return None
      }
    }
    Some(normalized.result())
  }

  private def parsePortSuffix(authority: String, suffix: String): Either[String, Option[AuthorityPort]] = {
    if (suffix.isEmpty || suffix == ":") Right(None)
    else if (suffix.charAt(0) != ':') invalid(authority, "unexpected data after host")
    else AuthorityPort.parse(suffix.substring(1)).left.map(_ => error(authority, "invalid port")).map(Some(_))
  }

  private def isUnreserved(char: Char): Boolean = {
    isAlpha(char) || isDigit(char) || "-._~".indexOf(char) >= 0
  }

  private def isSubDelimiter(char: Char): Boolean = "!$&'()*+,;=".indexOf(char) >= 0

  private def isAlpha(char: Char): Boolean =
    (char >= 'A' && char <= 'Z') || (char >= 'a' && char <= 'z')

  private def isDigit(char: Char): Boolean = char >= '0' && char <= '9'

  private def isHexDigit(char: Char): Boolean =
    isDigit(char) || (char >= 'A' && char <= 'F') || (char >= 'a' && char <= 'f')

  private def toLowerAscii(char: Char): Char = {
    if (char >= 'A' && char <= 'Z') (char + ('a' - 'A')).toChar else char
  }

  private def invalid[A](value: String, reason: String): Left[String, A] = Left(error(value, reason))

  private def error(value: String, reason: String): String = s"Invalid authority '$value': $reason"
}
