/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import java.net.{ Inet6Address, InetAddress, Inet4Address }

import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

/**
 * The NodeIdentifierParser object can parse node identifiers described in RFC 7239.
 */
private[common] object NodeIdentifierParser extends RegexParsers {

  sealed trait Port
  case class PortNumber(number: Int) extends Port
  case class ObfuscatedPort(s: String) extends Port

  sealed trait IpAddress
  case class Ip(ip: InetAddress) extends IpAddress
  case class ObfuscatedIp(s: String) extends IpAddress
  case object UnknownIp extends IpAddress

  def parseNode(s: String): Either[String, (IpAddress, Option[Port])] = {
    parse(node, s) match {
      case Success(matched, _) => Right(matched)
      case Failure(msg, _) => Left("failure: " + msg)
      case Error(msg, _) => Left("error: " + msg)
    }
  }

  private lazy val node = phrase(nodename ~ opt(":" ~> nodeport)) ^^ {
    case x ~ y => x -> y
  }

  private lazy val nodename = ("[" ~> ipv6Address <~ "]" | ipv4Address | "unknown" | obfnode) ^^ {
    case x: Inet4Address => Ip(x)
    case x: Inet6Address => Ip(x)
    case "unknown" => UnknownIp
    case x => ObfuscatedIp(x.toString)
  }

  private lazy val ipv4Address = regex("[\\d\\.]{7,15}".r) ^? inetAddress

  private lazy val ipv6Address = regex("[\\da-fA-F:]+".r) ^? inetAddress

  private lazy val obfnode = regex("_[\\p{Alnum}\\._-]+".r)

  private lazy val nodeport = (port | obfport) ^^ {
    case x: Int => PortNumber(x)
    case x => ObfuscatedPort(x.toString)
  }

  private lazy val port = regex("\\d{1,5}".r) ^? {
    case x if x.toInt <= 65535 => x.toInt
  }

  private def obfport = regex("_[\\p{Alnum}\\._-]+".r)

  private def inetAddress = new PartialFunction[String, InetAddress] {
    def isDefinedAt(s: String) = Try { InetAddress.getByName(s) }.isSuccess
    def apply(s: String) = Try { InetAddress.getByName(s) }.get
  }
}
