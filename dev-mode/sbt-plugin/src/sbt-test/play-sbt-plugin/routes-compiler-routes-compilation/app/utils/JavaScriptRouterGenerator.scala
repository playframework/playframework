/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package utils

import java.nio.file.Files
import java.nio.file.Paths

object JavaScriptRouterGenerator extends App {

  import controllers.routes.javascript._

  val jsFile = play.api.routing
    .JavaScriptReverseRouter(
      "jsRoutes",
      None,
      "localhost",
      Assets.versioned,
      Application.index,
      Application.post,
      Application.withParam,
      Application.takeBool
    )
    .body

  // Add module exports for node
  val jsModule = jsFile +
    """
      |module.exports = jsRoutes
    """.stripMargin

  val path = Paths.get(args(0))
  Files.createDirectories(path.getParent)
  Files.write(path, jsModule.getBytes("UTF-8"))

}
