/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import io.netty.handler.codec.http._
import play.api.mvc._
import scala.collection.JavaConverters._

/**
 * An implementation of Play `Headers` that wraps the raw Netty headers and
 * provides faster performance for some common read operations.
 */
private[server] class NettyHeadersWrapper(nettyHeaders: HttpHeaders) extends Headers(null) {

  override def headers: Seq[(String, String)] = {
    // Lazily initialize the header sequence using the Netty headers. It's OK
    // if we do this operation concurrently because the operation is idempotent.
    if (_headers == null) {
      _headers = nettyHeaders.entries.asScala.map(h => h.getKey -> h.getValue)
    }
    _headers
  }

  override def get(key: String): Option[String] = Option(nettyHeaders.get(key))
  override def apply(key: String): String = {
    val value = nettyHeaders.get(key)
    if (value == null) scala.sys.error("Header doesn't exist") else value
  }
  override def getAll(key: String): Seq[String] = nettyHeaders.getAll(key).asScala
}
