/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.net.Inet6Address
import java.net.InetAddress

import play.api.mvc.request.IpAddressSyntax

private[common] case class Subnet(ip: InetAddress, cidr: Option[Int] = None) {
  require(ip != null, "A subnet IP address must not be null")
  require(
    ip match {
      case address: Inet6Address => !IpAddressSyntax.hasScope(address)
      case _                     => true
    },
    "A subnet IPv6 address must not have a scope identifier"
  )

  private val networkBytes = ip.getAddress
  private val addressBits  = networkBytes.length * 8

  require(
    cidr.forall(prefix => prefix >= 0 && prefix <= addressBits),
    s"CIDR prefix length must be between 0 and $addressBits for ${ip.getHostAddress}"
  )

  private val prefixLength  = cidr.getOrElse(addressBits)
  private val completeBytes = prefixLength / 8
  private val remainingBits = prefixLength % 8
  private val partialMask   = 0xff << (8 - remainingBits)

  def isInRange(otherIp: InetAddress): Boolean = {
    if (ip.getClass != otherIp.getClass) {
      false
    } else {
      val candidateBytes = otherIp.getAddress
      var index          = 0
      while (index < completeBytes && networkBytes(index) == candidateBytes(index)) {
        index += 1
      }

      index == completeBytes &&
      (remainingBits == 0 ||
        (networkBytes(completeBytes) & partialMask) == (candidateBytes(completeBytes) & partialMask))
    }
  }
}

private[common] object Subnet {
  def apply(s: String): Subnet = s.split("/", -1) match {
    case Array(ip, subnet) if subnet.matches("[0-9]+") =>
      val prefix = subnet.toIntOption.getOrElse(throw invalidCidrPrefix(s))
      Subnet(parseIpAddress(ip), Some(prefix))
    case Array(_, _) =>
      throw invalidCidrPrefix(s)
    case Array(ip) => Subnet(parseIpAddress(ip))
    case _         => throw new IllegalArgumentException(s"$s contains more than one '/'.")
  }

  private def invalidCidrPrefix(value: String): IllegalArgumentException =
    new IllegalArgumentException(s"$value has an invalid CIDR prefix length.")

  private def parseIpAddress(value: String): InetAddress =
    IpAddressSyntax
      .parseIpv4(value)
      .orElse(IpAddressSyntax.parseIpv6(value).map(IpAddressSyntax.collapseMappedIpv6))
      .getOrElse(throw new IllegalArgumentException(s"$value is not an ASCII IP address literal."))

  def toString(b: Int) = Integer.toBinaryString(b)
}
