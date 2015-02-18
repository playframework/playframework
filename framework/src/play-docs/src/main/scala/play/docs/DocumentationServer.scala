/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.docs

import java.io.File
import java.util.concurrent.Callable
import play.api.Mode
import play.core.BuildDocHandler
import play.core.server.{ NettyServer, ServerConfig }

/**
 * A simple Play server that serves documentation. Used by the Play documentation
 * project. This class is not used by the Play SBT plugin because the plugin needs
 * to create a server that embeds both the user application and the Play documentation
 * application.
 */
class DocumentationServer(projectPath: File, buildDocHandler: BuildDocHandler, translationReport: Callable[File],
  forceTranslationReport: Callable[File], port: java.lang.Integer) extends NettyServer(
  ServerConfig(
    rootDir = projectPath,
    port = Some(port),
    mode = Mode.Dev,
    properties = System.getProperties
  ),
  DocumentationApplication(projectPath, buildDocHandler, translationReport, forceTranslationReport))
