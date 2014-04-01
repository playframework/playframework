/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.docs

import java.io.File
import play.api.Mode
import play.core.BuildDocHandler
import play.core.server.NettyServer

/**
 * A simple Play server that serves documentation. Used by the Play documentation
 * project. This class is not used by the Play SBT plugin because the plugin needs
 * to create a server that embeds both the user application and the Play documentation
 * application.
 */
class DocumentationServer(projectPath: File, buildDocHandler: BuildDocHandler, port: java.lang.Integer) extends NettyServer(DocumentationApplication(projectPath, buildDocHandler), Some(port),
  mode = Mode.Dev
)
