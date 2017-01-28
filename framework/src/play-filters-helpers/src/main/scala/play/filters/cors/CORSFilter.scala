/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.cors

import akka.stream.Materializer
import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler }
import play.core.j.{ JavaContextComponents, JavaHttpErrorHandlerAdapter }

import scala.concurrent.Future

import play.api.Logger
import play.api.mvc.{ Filter, RequestHeader, Result }

/**
 * A [[play.api.mvc.Filter]] that implements Cross-Origin Resource Sharing (CORS)
 *
 * It can be configured to...
 *
 *  - filter paths by a whitelist of path prefixes
 *  - allow only requests with origins from a whitelist (by default all origins are allowed)
 *  - allow only HTTP methods from a whitelist for preflight requests (by default all methods are allowed)
 *  - allow only HTTP headers from a whitelist for preflight requests (by default all headers are allowed)
 *  - set custom HTTP headers to be exposed in the response (by default no headers are exposed)
 *  - disable/enable support for credentials (by default credentials support is enabled)
 *  - set how long (in seconds) the results of a preflight request can be cached in a preflight result cache (by default 3600 seconds, 1 hour)
 *  - enable/disable serving requests with origins not in whitelist as non-CORS requests (by default they are forbidden)
 *
 * @param  corsConfig  configuration of the CORS policy
 * @param  pathPrefixes  whitelist of path prefixes to restrict the filter
 *
 * @see [[play.filters.cors.CORSConfig]]
 * @see [[play.filters.cors.AbstractCORSPolicy]]
 * @see [[play.filters.cors.CORSActionBuilder]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
class CORSFilter(
    override protected val corsConfig: CORSConfig = CORSConfig(),
    override protected val errorHandler: HttpErrorHandler = DefaultHttpErrorHandler,
    private val pathPrefixes: Seq[String] = Seq("/"))(override implicit val mat: Materializer) extends Filter with AbstractCORSPolicy {

  // Java constructor
  def this(corsConfig: CORSConfig, errorHandler: play.http.HttpErrorHandler, pathPrefixes: java.util.List[String], contextComponents: JavaContextComponents)(mat: Materializer) = {
    this(corsConfig, new JavaHttpErrorHandlerAdapter(errorHandler, contextComponents), Seq(pathPrefixes.toArray.asInstanceOf[Array[String]]: _*))(mat)
  }

  override protected val logger = Logger(classOf[CORSFilter])

  override def apply(f: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    if (pathPrefixes.exists(request.path.startsWith)) {
      filterRequest(f, request)
    } else {
      f(request)
    }
  }
}

object CORSFilter {

  val RequestTag = "CORS_REQUEST"

  def apply(corsConfig: CORSConfig = CORSConfig(), errorHandler: HttpErrorHandler = DefaultHttpErrorHandler,
    pathPrefixes: Seq[String] = Seq("/"))(implicit mat: Materializer) =
    new CORSFilter(corsConfig, errorHandler, pathPrefixes)

}
