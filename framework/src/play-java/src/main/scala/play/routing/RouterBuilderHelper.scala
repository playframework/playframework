/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.routing

import play.api.Play
import play.api.http.{ JavaHttpErrorHandlerDelegate, HttpConfiguration }
import play.api.mvc.{ BodyParser, Results, Action }
import play.core.j.{ JavaParsers, JavaHelpers }
import play.core.routing.HandlerInvokerFactory
import play.libs.F
import play.mvc.Http.{ RequestBody, Context }
import play.mvc.Result
import play.utils.UriEncoding
import scala.collection.JavaConversions._

import play.api.libs.iteratee.Execution.Implicits.trampoline

import scala.concurrent.Future

private[routing] object RouterBuilderHelper {
  def build(router: RoutingDsl): play.api.routing.Router = {
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
                val parser = HandlerInvokerFactory.javaBodyParserToScala(
                  // If testing an embedded application we may not have a Guice injector, therefore we can't rely on
                  // it to instantiate the default body parser, we have to instantiate it ourselves.
                  new play.mvc.BodyParser.Default(new JavaHttpErrorHandlerDelegate(Play.current.errorHandler),
                    Play.current.injector.instanceOf[HttpConfiguration])
                )
                Action.async(parser) { request =>
                  val ctx = JavaHelpers.createJavaContext(request)
                  try {
                    Context.current.set(ctx)
                    route.actionMethod.invoke(route.action, params: _*) match {
                      case result: Result => Future.successful(result.asScala)
                      case promise: F.Promise[Result] => promise.wrapped.map(_.asScala)
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
    })
  }
}
