/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import scala.util.Try

import com.google.common.net.InetAddresses

/** Validates the RFC 7239 host parameter against the HTTP Host field grammar. */
private[common] object Rfc7239HostParser {

  /**
   * Return the original value when it matches `uri-host [ ":" port ]`.
   *
   * RFC 7239 requires this parameter to contain the original Host field value.
   * The URI grammar permits an empty host or port and requires IPv6 and IPvFuture
   * literals to be enclosed in brackets.
   */
  def parse(value: String): Option[String] = Option.when(isValid(value))(value)

  private def isValid(value: String): Boolean = {
    if (value.startsWith("[")) validIpLiteralHost(value)
    else validRegisteredHost(value)
  }

  private def validIpLiteralHost(value: String): Boolean = {
    val closingBracket = value.indexOf(']')
    closingBracket > 1 &&
    validIpLiteral(value.substring(1, closingBracket)) &&
    validPortSuffix(value.substring(closingBracket + 1))
  }

  private def validRegisteredHost(value: String): Boolean = {
    val colon = value.indexOf(':')
    if (colon < 0) validRegisteredName(value)
    else {
      value.indexOf(':', colon + 1) < 0 &&
      validRegisteredName(value.substring(0, colon)) &&
      validPortSuffix(value.substring(colon))
    }
  }

  private def validPortSuffix(value: String): Boolean = {
    value.isEmpty || (value.charAt(0) == ':' && value.substring(1).forall(isDigit))
  }

  private def validIpLiteral(value: String): Boolean = validIpv6Address(value) || validIpvFuture(value)

  private def validIpv6Address(value: String): Boolean = {
    value.contains(':') && Try(InetAddresses.forString(value)).isSuccess
  }

  private def validIpvFuture(value: String): Boolean = {
    if (value.length < 4 || (value.charAt(0) != 'v' && value.charAt(0) != 'V')) false
    else {
      val dot = value.indexOf('.')
      dot > 1 &&
      value.substring(1, dot).forall(isHexDigit) &&
      dot + 1 < value.length &&
      value.substring(dot + 1).forall(char => isUnreserved(char) || isSubDelimiter(char) || char == ':')
    }
  }

  private def validRegisteredName(value: String): Boolean = {
    var index = 0
    while (index < value.length) {
      val char = value.charAt(index)
      if (isUnreserved(char) || isSubDelimiter(char)) {
        index += 1
      } else if (
        char == '%' &&
        index + 2 < value.length &&
        isHexDigit(value.charAt(index + 1)) &&
        isHexDigit(value.charAt(index + 2))
      ) {
        index += 3
      } else {
        return false
      }
    }
    true
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
}
