/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.docs

import java.io.File
import java.util.concurrent.Callable

import play.api._
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird._
import play.core._
import play.core.server._

import scala.concurrent.Future

/**
 * Used to start the documentation server.
 */
class DocServerStart {

  def start(projectPath: File, buildDocHandler: BuildDocHandler, translationReport: Callable[File],
    forceTranslationReport: Callable[File], port: java.lang.Integer): ReloadableServer = {

    val components = {
      val environment = Environment(projectPath, this.getClass.getClassLoader, Mode.Test)
      val context = ApplicationLoader.Context.create(environment)
      new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
        lazy val router = Router.from {
          case GET(p"/@documentation/$file*") => Action { request =>
            buildDocHandler.maybeHandleDocRequest(request).asInstanceOf[Option[Result]].get
          }
          case GET(p"/@report") => Action { request =>
            if (request.getQueryString("force").isDefined) {
              forceTranslationReport.call()
              Results.Redirect("/@report")
            } else {
              Results.Ok.sendFile(translationReport.call(), inline = true, fileName = _ => "report.html")(executionContext, fileMimeTypes)
            }
          }
          case _ => Action {
            Results.Redirect("/@documentation/Home")
          }
        }
      }
    }
    val application: Application = components.application

    Play.start(application)

    val applicationProvider = ApplicationProvider(application)

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
