/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package utils

import java.nio.file.Files
import java.nio.file.Paths

object JavaScriptRouterGenerator {

  def main(args: Array[String]): Unit = {
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
    ()
  }

}
