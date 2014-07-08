/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import play.core.ApplicationProvider

/**
 * An object that knows how to obtain a server. Instantiating a
 * ServerProvider object should be fast and side-effect free. Any
 * actual work that a ServerProvider needs to do should be delayed
 * until the `createServer` method is called.
 */
trait ServerProvider {
  def createServer(config: ServerConfig, appProvider: ApplicationProvider): ServerWithStop
}