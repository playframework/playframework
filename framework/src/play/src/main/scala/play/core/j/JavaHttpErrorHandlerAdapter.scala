/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import javax.inject.Inject

import play.api.http.{ HttpError, HttpErrorHandler }
import play.api.mvc.{ RequestHeader, Result }
import play.http.{ HttpErrorHandler => JHttpErrorHandler }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.compat.java8.FutureConverters
import scala.concurrent.Future

/**
 * Adapter from a Java HttpErrorHandler to a Scala HttpErrorHandler
 */
class JavaHttpErrorHandlerAdapter @Inject() (underlying: JHttpErrorHandler) extends HttpErrorHandler {

  override def onError(error: HttpError[_]): Future[Result] = {
    FutureConverters.toScala(underlying.onError(error.asJava)).map(_.asScala())
  }

}
