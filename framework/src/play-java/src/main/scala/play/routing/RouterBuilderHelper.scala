/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routing

import java.util.concurrent.CompletionStage

import play.api.Play
import play.api.http.{ JavaHttpErrorHandlerDelegate, HttpConfiguration }
import play.api.mvc.{ Results, Action }
import play.core.j.JavaHelpers
import play.core.routing.HandlerInvokerFactory
import play.mvc.Http.Context
import play.mvc.Result
import play.utils.UriEncoding
import scala.collection.JavaConversions._

import play.api.libs.iteratee.Execution.Implicits.trampoline

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

private[routing] object RouterBuilderHelper {
  def build(router: RoutingDsl): play.routing.Router = {
    val routes = router.routes.toList

    // Create the router
    play.api.routing.Router.from(Function.unlift { requestHeader =>

      // Find the first route that matches
      routes.collectFirst(Function.unlift(route =>

        // First check method
        if (requestHeader.method == route.method) {

          // Now match against the path pattern
          val matcher = route.pathPattern.matcher(requestHeader.path)
          if (matcher.matches()) {

            // Extract groups into a Seq
            val groups = for (i <- 1 to matcher.groupCount()) yield {
              matcher.group(i)
            }

            // Bind params if required
            val params = groups.zip(route.params).map {
              case (param, routeParam) =>
                val rawParam = if (routeParam.decode) {
                  UriEncoding.decodePathSegment(param, "utf-8")
                } else {
                  param
                }
                routeParam.pathBindable.bind(routeParam.name, rawParam)
            }

            val maybeParams = params.foldLeft[Either[String, Seq[AnyRef]]](Right(Nil)) {
              case (error @ Left(_), _) => error
              case (_, Left(error)) => Left(error)
              case (Right(values), Right(value: AnyRef)) => Right(values :+ value)
              case (values, _) => values
            }

            val action = maybeParams match {
              case Left(error) => Action(Results.BadRequest(error))
              case Right(params) =>

                // Convert to a Scala action
                val parser = HandlerInvokerFactory.javaBodyParserToScala {
                  // If testing an embedded application we may not have a Guice injector, therefore we can't rely on
                  // it to instantiate the default body parser, we have to instantiate it ourselves.
                  val app = Play.privateMaybeApplication.get // throw exception if no current app
                  new play.mvc.BodyParser.Default(new JavaHttpErrorHandlerDelegate(app.errorHandler),
                    app.injector.instanceOf[HttpConfiguration])
                }
                Action.async(parser) { request =>
                  val ctx = JavaHelpers.createJavaContext(request)
                  try {
                    Context.current.set(ctx)
                    route.actionMethod.invoke(route.action, params: _*) match {
                      case result: Result => Future.successful(result.asScala)
                      case promise: CompletionStage[Result] => FutureConverters.toScala(promise).map(_.asScala)
                    }
                  } finally {
                    Context.current.remove()
                  }
                }
            }

            Some(action)
          } else None
        } else None
      ))
    }).asJava
  }
}
