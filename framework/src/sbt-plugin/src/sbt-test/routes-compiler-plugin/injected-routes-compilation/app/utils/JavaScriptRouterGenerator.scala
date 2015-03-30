package utils

import java.io._

object JavaScriptRouterGenerator extends App {

  import controllers.routes.javascript._

  val jsFile = play.api.routing.JavaScriptReverseRouter("jsRoutes", None, "localhost",
    Application.index,
    Application.post,
    Application.withParam,
    Application.takeBool
  ).body

  // Add module exports for node
  val jsModule = jsFile +
    """
      |
      |module.exports = jsRoutes
    """.stripMargin

  val file = new File(args(0))
  file.getParentFile.mkdirs()
  val writer = new FileWriter(file)
  writer.write(jsModule)
  writer.close()

}
