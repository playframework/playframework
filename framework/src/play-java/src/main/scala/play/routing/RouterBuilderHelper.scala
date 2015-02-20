/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.routing

import play.api.mvc.{ Results, Action }
import play.core.j.{ JavaParsers, JavaHelpers }
import play.libs.F
import play.mvc.Http.Context
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
                Action.async(JavaParsers.default_(-1)) { request =>
                  val ctx = JavaHelpers.createJavaContext(request)
                  try {
                    Context.current.set(ctx)
                    route.actionMethod.invoke(route.action, params: _*) match {
                      case result: Result => Future.successful(result.toScala)
                      case promise: F.Promise[Result] => promise.wrapped.map(_.toScala)
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
