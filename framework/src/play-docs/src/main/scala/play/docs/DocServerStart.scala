/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.docs

import java.io.File
import java.util.concurrent.Callable
import play.api._
import play.api.mvc._
import play.api.routing.Router
import play.core._
import play.core.server._
import scala.concurrent.Future
import scala.util.Success

/**
 * Used to start the documentation server.
 */
class DocServerStart {

  def start(projectPath: File, buildDocHandler: BuildDocHandler, translationReport: Callable[File],
    forceTranslationReport: Callable[File], port: java.lang.Integer): ServerWithStop = {

    val application: Application = {
      val environment = Environment(projectPath, this.getClass.getClassLoader, Mode.Test)
      val context = ApplicationLoader.createContext(environment)
      val components = new BuiltInComponentsFromContext(context) {
        lazy val router = Router.empty
      }
      components.application
    }

    Play.start(application)

    val applicationProvider = new ApplicationProvider {

      override def get = Success(application)
      override def handleWebCommand(request: RequestHeader) =
        buildDocHandler.maybeHandleDocRequest(request).asInstanceOf[Option[Result]].orElse(
          if (request.path == "/@report") {
            if (request.getQueryString("force").isDefined) {
              forceTranslationReport.call()
              Some(Results.Redirect("/@report"))
            } else {
              Some(Results.Ok.sendFile(translationReport.call(), inline = true, fileName = _ => "report.html"))
            }
          } else None
        ).orElse(
            Some(Results.Redirect("/@documentation"))
          )
    }

    val config = ServerConfig(
      rootDir = projectPath,
      port = Some(port),
      mode = Mode.Test,
      properties = System.getProperties
    )
    val serverProvider: ServerProvider = ServerProvider.fromConfiguration(getClass.getClassLoader, config.configuration)
    val context = ServerProvider.Context(
      config,
      applicationProvider,
      application.actorSystem,
      application.materializer,
      stopHook = () => Future.successful(())
    )
    serverProvider.createServer(context)

  }

}
