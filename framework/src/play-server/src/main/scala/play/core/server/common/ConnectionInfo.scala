/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.common

import java.net.InetAddress

/**
 * Basic information about an HTTP connection.
 */
final case class ConnectionInfo(address: InetAddress, secure: Boolean)