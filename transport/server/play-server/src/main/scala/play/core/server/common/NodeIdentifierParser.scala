/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Locale

import scala.util.parsing.combinator.RegexParsers

import play.api.mvc.request.IpAddressSyntax
import play.api.mvc.request.NodePort
import play.core.server.common.ForwardedHeaderHandler.ForwardedHeaderVersion
import play.core.server.common.ForwardedHeaderHandler.Rfc7239
import play.core.server.common.ForwardedHeaderHandler.Xforwarded
import play.core.server.common.NodeIdentifierParser._

/**
 * The NodeIdentifierParser object can parse node identifiers described in RFC 7239.
 *
 * @param version The version of the forwarded headers that we want to parse nodes for.
 * The version is used to switch between IP address parsing behavior.
 */
private[common] class NodeIdentifierParser(version: ForwardedHeaderVersion) extends RegexParsers {
  // RFC 7239 requires the complete unescaped node value to match the node
  // grammar. X-Forwarded-For historically permits surrounding whitespace.
  override val skipWhitespace: Boolean = version == Xforwarded

  def parseNode(s: String): Either[String, (IpAddress, Option[Port])] = {
    parse(node, s) match {
      case Success(matched, _) => Right(matched)
      case Failure(msg, _)     => Left("failure: " + msg)
      case Error(msg, _)       => Left("error: " + msg)
    }
  }

  private lazy val node = phrase(nodename ~ opt(":" ~> nodeport)) ^^ {
    case x ~ y => x -> y
  }

  private lazy val nodename = version match {
    case Rfc7239 =>
      // RFC 7239 recognizes IPv4 addresses, escaped IPv6 addresses, unknown and obfuscated addresses
      (ipv4Address | "[" ~> ipv6Address <~ "]" | "(?i)unknown".r | obfnode) ^^ {
        case x: Inet4Address                                          => Ip(x)
        case x: Inet6Address                                          => Ip(x)
        case x if x.toString.toLowerCase(Locale.ENGLISH) == "unknown" => UnknownIp
        case x                                                        => ObfuscatedIp(x.toString)
      }
    case Xforwarded =>
      // X-Forwarded-For recognizes IPv4 and escaped or unescaped IPv6 addresses
      (ipv4Address | "[" ~> ipv6Address <~ "]" | ipv6Address) ^^ {
        case x: Inet4Address => Ip(x)
        case x: Inet6Address => Ip(x)
      }
  }

  private lazy val ipv4Address = regex("[0-9.]{7,15}".r) ^? parsedAddress(IpAddressSyntax.parseIpv4)

  private lazy val ipv6Address = regex("[0-9a-fA-F:\\.]+".r) ^? parsedAddress { value =>
    IpAddressSyntax.parseIpv6(value).map(IpAddressSyntax.collapseMappedIpv6)
  }

  private lazy val obfnode = regex(NodePort.obfuscatedIdentifierPattern)

  private lazy val nodeport = (port | obfport) ^^ {
    case x: Int => PortNumber(x)
    case x      => ObfuscatedPort(x.toString)
  }

  private lazy val port = regex("[0-9]{1,5}".r) ^? {
    case x if x.toInt <= 65535 => x.toInt
  }

  private def obfport = regex(NodePort.obfuscatedIdentifierPattern)

  private def parsedAddress[A <: InetAddress](parse: String => Option[A]) = new PartialFunction[String, A] {
    def isDefinedAt(s: String) = parse(s).isDefined
    def apply(s: String)       = parse(s).get
  }
}

private[common] object NodeIdentifierParser {
  sealed trait Port
  case class PortNumber(number: Int)   extends Port
  case class ObfuscatedPort(s: String) extends Port

  sealed trait IpAddress
  case class Ip(ip: InetAddress)     extends IpAddress
  case class ObfuscatedIp(s: String) extends IpAddress
  case object UnknownIp              extends IpAddress
}
