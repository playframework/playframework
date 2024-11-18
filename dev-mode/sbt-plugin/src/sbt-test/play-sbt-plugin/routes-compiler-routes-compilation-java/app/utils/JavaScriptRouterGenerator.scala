/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package utils

import java.nio.file.Files
import java.nio.file.Paths

object JavaScriptRouterGenerator {

  import controllers.routes.javascript._

  def main(args: Array[String]): Unit = {
    val jsFile = play.api.routing
      .JavaScriptReverseRouter(
        "jsRoutes",
        None,
        "localhost",
        Assets.versioned,
        Application.index,
        Application.post,
        Application.withParam,
        Application.takeBool,
        Application.takeOptionalInt,
        Application.takeOptionalIntWithDefault
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
