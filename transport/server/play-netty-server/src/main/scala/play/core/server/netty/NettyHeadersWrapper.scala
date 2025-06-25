/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import scala.jdk.CollectionConverters._

import io.netty.handler.codec.http._
import play.api.mvc._

/**
 * An implementation of Play `Headers` that wraps the raw Netty headers and
 * provides faster performance for some common read operations.
 */
private[server] class NettyHeadersWrapper(nettyHeaders: HttpHeaders) extends Headers(null) {
  override def headers: Seq[(String, String)] = {
    // Lazily initialize the header sequence using the Netty headers. It's OK
    // if we do this operation concurrently because the operation is idempotent.
    if (_headers == null) {
      _headers = nettyHeaders.entries.asScala.toSeq.map(h => h.getKey -> h.getValue)
    }
    _headers
  }

  override def get(key: String): Option[String] = Option(nettyHeaders.get(key))
  override def apply(key: String): String       = {
    val value = nettyHeaders.get(key)
    if (value == null) scala.sys.error("Header doesn't exist") else value
  }
  override def getAll(key: String): Seq[String] = nettyHeaders.getAll(key).asScala.toSeq
}
