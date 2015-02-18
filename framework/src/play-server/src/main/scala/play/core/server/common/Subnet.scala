/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import java.net.InetAddress

private[common] case class Subnet(ip: InetAddress, cidr: Option[Int] = None) {

  private def remainderOfMask = for {
    m <- cidr
    result <- maskBits(m % 8)
  } yield result

  private def maskBits(leadingBits: Int) = leadingBits match {
    case i if i < 1 || i > 7 => None
    case i => Some(~(0xff >>> leadingBits))
  }

  def isInRange(otherIp: InetAddress) = {
    val mask = cidr.getOrElse(ip.getAddress.length * 8)
    ip.getClass == otherIp.getClass &&
      ip.getAddress.take(mask / 8).toList.equals(otherIp.getAddress.take(mask / 8).toList) &&
      (for {
        a <- ip.getAddress.drop(mask / 8).headOption
        b <- otherIp.getAddress.drop(mask / 8).headOption
        c <- remainderOfMask
      } yield (a & c) == (b & c)).getOrElse(true)
  }
}

private[common] object Subnet {
  def apply(s: String): Subnet = s.split("/") match {
    case Array(ip, subnet) => Subnet(InetAddress.getByName(ip), Some(subnet.toInt))
    case Array(ip) => Subnet(InetAddress.getByName(ip))
    case _ => throw new IllegalArgumentException(s"$s contains more than one '/'.")
  }

  def toString(b: Int) = Integer.toBinaryString(b)
}
