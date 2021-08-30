/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package utils

import java.nio.file.Paths
import java.nio.file.Files

object JavaScriptRouterGenerator extends App {

  import controllers.routes.javascript._

  val host = if (args.length > 1) args(1) else "localhost"

  val jsFile = play.api.routing
    .JavaScriptReverseRouter(
      "jsRoutes",
      host,
      Assets.versioned,
      Application.index,
      Application.post,
      Application.withParam,
      Application.takeBool,
      Application.takeListTickedParam,
      Application.takeTickedParams
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
