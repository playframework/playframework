/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import play.api.libs.typedmap.{ TypedKey, TypedMap }
import play.api.mvc.RequestHeader
import play.core.server.ServerProvider

/** An object attached to requests when server debugging is enabled. */
private[play] final case class ServerDebugInfo(
    serverProvider: ServerProvider,
    serverConfigCacheReloads: Int
)

private[play] object ServerDebugInfo {
  /** The attribute used to attach debug info to requests. */
  val Attr = TypedKey[ServerDebugInfo]("serverDebugInfo")

  /**
   * Helper method for use in server backends. Attaches the debug info the request if the info is defined.
   */
  def attachToRequestHeader(rh: RequestHeader, serverDebugInfo: Option[ServerDebugInfo]): RequestHeader = {
    serverDebugInfo match {
      case None => rh
      case Some(info) => rh.addAttr(Attr, info)
    }
  }
}