/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.docs

import java.io.File
import play.api.inject.{ NewInstanceInjector, DefaultApplicationLifecycle }
import play.api.mvc._
import play.api._
import play.core._
import scala.util.Success

/**
 * Provides a very simple application that renders Play documentation.
 */
case class DocumentationApplication(projectPath: File, buildDocHandler: BuildDocHandler) extends ApplicationProvider {

  private val environment = Environment(projectPath, this.getClass.getClassLoader, Mode.Dev)
  private val configuration = Configuration.load(environment.rootPath, environment.mode, Map.empty)
  val application = new DefaultApplication(environment, new OptionalSourceMapper(None),
    new DefaultApplicationLifecycle(), NewInstanceInjector, configuration, DefaultGlobal
  ) {
    override lazy val routes = None
    override lazy val plugins = Nil
  }

  Play.start(application)

  override def path = projectPath
  override def get = Success(application)
  override def handleWebCommand(request: RequestHeader) =
    buildDocHandler.maybeHandleDocRequest(request).asInstanceOf[Option[Result]].orElse(
      Some(Results.Redirect("/@documentation"))
    )
}
