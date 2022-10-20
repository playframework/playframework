/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import java.net.SocketAddress

/**
 * This exception is thrown when the server is unable to listen on a port
 */
class ServerListenException(protocol: String, address: SocketAddress) extends Exception {
  override def getMessage = s"Failed to listen for $protocol on $address!"
}
