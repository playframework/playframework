package play.core.server

import java.net.SocketAddress

/**
 * This execption is thrown when the server is unable to listen on a port
 */
class ServerListenException(protocol: String, address: SocketAddress) extends Exception {
  override def getMessage = s"Failed to listen for $protocol on $address!"
}
