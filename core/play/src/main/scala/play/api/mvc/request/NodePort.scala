/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import scala.util.matching.Regex

/** A numeric or obfuscated port identifier attached to an RFC 7239 node. */
sealed trait NodePort {

  /**
   * The Java API representation of this node port.
   */
  def asJava: play.mvc.Http.NodePort = this match {
    case NodePort.Numeric(value)    => new play.mvc.Http.NodePort.Numeric(value)
    case NodePort.Obfuscated(value) => new play.mvc.Http.NodePort.Obfuscated(value)
  }
}

object NodePort {
  private val MaxNumericPort = 65535

  final case class Numeric(value: Int) extends NodePort {
    require(
      value >= 0 && value <= MaxNumericPort,
      s"A numeric node port must be between 0 and $MaxNumericPort: $value"
    )
  }

  final case class Obfuscated(value: String) extends NodePort {
    require(isObfuscatedIdentifier(value), s"Invalid obfuscated node port: '$value'")
  }

  def numeric(value: Int): NodePort       = Numeric(value)
  def obfuscated(value: String): NodePort = Obfuscated(value)

  private[play] val obfuscatedIdentifierPattern: Regex = "_[A-Za-z0-9._-]+".r

  private[play] def isObfuscatedIdentifier(value: String): Boolean =
    value != null && obfuscatedIdentifierPattern.pattern.matcher(value).matches()
}
