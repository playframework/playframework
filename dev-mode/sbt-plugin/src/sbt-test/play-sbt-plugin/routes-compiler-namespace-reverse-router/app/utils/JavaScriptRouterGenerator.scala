/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package utils

import java.nio.file.Files
import java.nio.file.Paths

object JavaScriptRouterGenerator extends App {

  val jsFile = play.api.routing
    .JavaScriptReverseRouter(
      "jsRoutes",
      None,
      "localhost",
      router.controllers.routes.javascript.Assets.versioned,
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
