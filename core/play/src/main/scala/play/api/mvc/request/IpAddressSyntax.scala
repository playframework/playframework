/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

import com.google.common.net.InetAddresses

/** Strict, non-resolving parsers for the ASCII IP-address productions used by URI and forwarding syntax. */
private[play] object IpAddressSyntax {

  def parseIpv4(value: String): Option[Inet4Address] =
    parseAddress(value, char => isAsciiDigit(char) || char == '.').collect { case address: Inet4Address => address }

  def parseIpv6(value: String): Option[Inet6Address] =
    parseAddress(value, isIpv6Character).flatMap {
      case address: Inet6Address => Some(address)
      // Guava deliberately represents IPv4-mapped IPv6 text as Inet4Address. URI authority
      // classification follows the wire syntax, so restore its 128-bit representation here.
      case address: Inet4Address if value.indexOf(':') >= 0 => Some(expandMappedIpv6(address))
      case _                                                => None
    }

  /** Preserve the historical Java/Guava representation used for forwarding identity and trust matching. */
  def collapseMappedIpv6(address: Inet6Address): InetAddress = {
    val bytes = address.getAddress
    if (bytes.take(10).forall(_ == 0) && bytes(10) == 0xff.toByte && bytes(11) == 0xff.toByte) {
      InetAddress.getByAddress(bytes.takeRight(4))
    } else address
  }

  def hasScope(address: Inet6Address): Boolean =
    address.getScopeId != 0 || address.getScopedInterface != null || address.getHostAddress.indexOf('%') >= 0

  private def parseAddress(value: String, isAllowed: Char => Boolean): Option[InetAddress] =
    Option(value)
      // Guava accepts Unicode digits and scoped IPv6 addresses. Request syntax admits neither,
      // and rejecting '%' also prevents any environment-dependent interface lookup.
      .filter(input => input.nonEmpty && input.forall(isAllowed))
      // Check first because malformed request input is ordinary and should not use exceptions for control flow.
      .filter(InetAddresses.isInetAddress)
      .map(InetAddresses.forString)

  private def expandMappedIpv6(address: Inet4Address): Inet6Address = {
    val bytes = new Array[Byte](16)
    bytes(10) = 0xff.toByte
    bytes(11) = 0xff.toByte
    Array.copy(address.getAddress, 0, bytes, 12, 4)
    Inet6Address.getByAddress(null, bytes, -1)
  }

  private def isIpv6Character(char: Char): Boolean =
    isAsciiHexDigit(char) || char == ':' || char == '.'

  private def isAsciiDigit(char: Char): Boolean = char >= '0' && char <= '9'

  private def isAsciiHexDigit(char: Char): Boolean =
    isAsciiDigit(char) || (char >= 'A' && char <= 'F') || (char >= 'a' && char <= 'f')
}
