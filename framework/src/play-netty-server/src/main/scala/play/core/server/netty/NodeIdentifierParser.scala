/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.netty

import java.net.{ Inet6Address, InetAddress, Inet4Address }

import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

/**
 * The NodeIdentifierParser object can parse node identifiers described in RFC 7239.
 */
private[play] object NodeIdentifierParser extends RegexParsers {

  def parseNode(s: String): Either[String, (Either[String, InetAddress], Option[Either[String, Int]])] = {
    parse(node, s) match {
      case Success(matched, _) => Right(matched)
      case Failure(msg, _) => Left("failure: " + msg)
      case Error(msg, _) => Left("error: " + msg)
    }
  }

  private def node = phrase(nodename ~ opt(":" ~> nodeport)) ^^ {
    case x ~ y => x -> y
  }

  private def nodename = ("[" ~> ipv6Address <~ "]" | ipv4Address | "unknown" | obfnode) ^^ {
    case x: Inet4Address => Right(x)
    case x: Inet6Address => Right(x)
    case x => Left(x.toString)
  }

  private def ipv4Address = regex("[\\d\\.]{7,15}".r) ^? inetAddress

  private def ipv6Address = regex("[\\da-fA-F:]+".r) ^? inetAddress

  private def obfnode = regex("_[\\p{Alnum}\\._-]+".r)

  private def nodeport = (port | obfport) ^^ {
    case x: Int => Right(x)
    case x => Left(x.toString)
  }

  private def port = regex("\\d{1,5}".r) ^? {
    case x if x.toInt <= 65535 => x.toInt
  }

  private def obfport = regex("_[\\p{Alnum}\\._-]+".r)

  private def inetAddress = new PartialFunction[String, InetAddress] {
    def isDefinedAt(s: String) = Try { InetAddress.getByName(s) }.isSuccess
    def apply(s: String) = Try { InetAddress.getByName(s) }.get
  }
}
