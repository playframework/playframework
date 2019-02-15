/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing

import java.util.concurrent.CompletionStage

import play.api.mvc._
import play.core.j.{ JavaContextComponents, JavaHelpers }
import play.mvc.Http.{ Context, RequestBody }
import play.mvc.Result
import play.utils.UriEncoding

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters
import scala.concurrent.{ ExecutionContext, Future }

private[routing] class RouterBuilderHelper(bodyParser: BodyParser[RequestBody], contextComponents: JavaContextComponents) {

  def build(router: RoutingDsl): play.routing.Router = {
    val routes = router.routes.asScala

    // Create the router
    play.api.routing.Router.from(Function.unlift { requestHeader =>

      // Find the first route that matches
      routes.collectFirst(Function.unlift(route => {

        def handleUsingRequest(parameters: Seq[AnyRef], request: Request[RequestBody])(implicit executionContext: ExecutionContext) = {
          val actionParameters = request.asJava +: parameters
          val javaResultFuture = route.actionMethod.invoke(route.action, actionParameters: _*) match {
            case result: Result => Future.successful(result)
            case promise: CompletionStage[_] =>
              val p = promise.asInstanceOf[CompletionStage[Result]]
              FutureConverters.toScala(p)
          }
          javaResultFuture.map(_.asScala())
        }

        def handleUsingHttpContext(parameters: Seq[AnyRef], request: Request[RequestBody])(implicit executionContext: ExecutionContext) = {
          val ctx = JavaHelpers.createJavaContext(request, contextComponents)
          try {
            Context.setCurrent(ctx)
            val javaResultFuture = route.actionMethod.invoke(route.action, parameters: _*) match {
              case result: Result => Future.successful(result)
              case promise: CompletionStage[_] =>
                val p = promise.asInstanceOf[CompletionStage[Result]]
                FutureConverters.toScala(p)
            }
            javaResultFuture.map(JavaHelpers.createResult(ctx, _))
          } finally {
            Context.clear()
          }
        }

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
            val params = groups.zip(route.params.asScala).map {
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
              case Left(error) => ActionBuilder.ignoringBody(Results.BadRequest(error))
              case Right(parameters) =>
                import play.core.Execution.Implicits.trampoline
                ActionBuilder.ignoringBody.async(bodyParser) { request: Request[RequestBody] =>
                  route.action match {
                    case _: RequestFunctions.RequestFunction => handleUsingRequest(parameters, request)
                    case _ => handleUsingHttpContext(parameters, request)
                  }
                }
            }

            Some(action)
          } else None
        } else None
      }))
    }).asJava
  }
}

object RouterBuilderHelper {
  def toRequestBodyParser(bodyParser: BodyParser[AnyContent]): BodyParser[RequestBody] = {
    import play.core.Execution.Implicits.trampoline
    bodyParser.map(ac => new RequestBody(ac))
  }
}