/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.docs

import java.io.File
import java.util.concurrent.Callable
import play.api.mvc._
import play.api._
import play.api.routing.Router
import play.core._
import scala.util.Success

/**
 * Provides a very simple application that renders Play documentation.
 */
case class DocumentationApplication(projectPath: File, buildDocHandler: BuildDocHandler,
    translationReport: Callable[File],
    forceTranslationReport: Callable[File]) extends ApplicationProvider {

  private val environment = Environment(projectPath, this.getClass.getClassLoader, Mode.Dev)
  private val context = ApplicationLoader.createContext(environment)
  private val components = new BuiltInComponentsFromContext(context) {
    lazy val router = Router.empty
  }

  def application = components.application

  Play.start(application)

  override def path = projectPath
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
