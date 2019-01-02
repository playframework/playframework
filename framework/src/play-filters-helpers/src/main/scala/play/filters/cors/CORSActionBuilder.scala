/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.cors

import akka.stream.Materializer
import akka.util.ByteString
import play.api.http.{ DefaultHttpErrorHandler, HttpErrorHandler, ParserConfiguration }
import play.api.libs.Files.{ SingletonTemporaryFileCreator, TemporaryFileCreator }
import play.api.libs.streams.Accumulator
import play.api.libs.typedmap.TypedMap
import play.api.mvc._
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.api.{ Configuration, Logger }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A play.api.mvc.ActionBuilder that implements Cross-Origin Resource Sharing (CORS)
 *
 * @see [[play.filters.cors.CORSFilter]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
trait CORSActionBuilder extends ActionBuilder[Request, AnyContent] with AbstractCORSPolicy {

  protected def mat: Materializer

  override protected val logger: Logger = Logger.apply(classOf[CORSActionBuilder])

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    val action = new EssentialAction {
      override def apply(req: RequestHeader): Accumulator[ByteString, Result] = {
        req match {
          case r: Request[A] => Accumulator.done(block(r))
          case _ => Accumulator.done(block(req.withBody(request.body)))
        }
      }
    }

    filterRequest(action, request).run()(mat)
  }
}

/**
 * A play.api.mvc.ActionBuilder that implements Cross-Origin Resource Sharing (CORS)
 *
 * It can be configured to...
 *
 *  - allow only requests with origins from a whitelist (by default all origins are allowed)
 *  - allow only HTTP methods from a whitelist for preflight requests (by default all methods are allowed)
 *  - allow only HTTP headers from a whitelist for preflight requests (by default all headers are allowed)
 *  - set custom HTTP headers to be exposed in the response (by default no headers are exposed)
 *  - disable/enable support for credentials (by default credentials support is enabled)
 *  - set how long (in seconds) the results of a preflight request can be cached in a preflight result cache (by default 3600 seconds, 1 hour)
 *
 * @example
 * {{{
 * CORSActionBuilder(configuration) { Ok } // an action that uses the application configuration
 *
 * CORSActionBuilder(configuration, "my-conf-path") { Ok } // an action that uses a subtree of the application configuration
 *
 * val corsConfig: CORSConfig = ...
 * CORSActionBuilder(conf) { Ok } // an action that uses a locally defined configuration
 * }}}
 *
 * @see [[play.filters.cors.CORSFilter]]
 * @see [[http://www.w3.org/TR/cors/ CORS specification]]
 */
object CORSActionBuilder {

  /**
   * Construct an action builder that uses a subtree of the application configuration.
   *
   * @param  config  The configuration to load the config from
   * @param  configPath  The path to the subtree of the application configuration.
   */
  def apply(
    config: Configuration,
    errorHandler: HttpErrorHandler = DefaultHttpErrorHandler,
    configPath: String = "play.filters.cors",
    parserConfig: ParserConfiguration = ParserConfiguration(),
    tempFileCreator: TemporaryFileCreator = SingletonTemporaryFileCreator)(implicit materializer: Materializer, ec: ExecutionContext): CORSActionBuilder = {
    val eh = errorHandler
    new CORSActionBuilder {
      override lazy val parser = new BodyParsers.Default(tempFileCreator, eh, parserConfig)(materializer)
      override protected def mat: Materializer = materializer
      override protected def executionContext: ExecutionContext = ec
      override protected def corsConfig: CORSConfig = {
        val prototype = config.get[Configuration]("play.filters.cors")
        val corsConfig = prototype ++ config.get[Configuration](configPath)
        CORSConfig.fromUnprefixedConfiguration(corsConfig)
      }
      override protected val errorHandler: HttpErrorHandler = eh
    }
  }

  /**
   * Construct an action builder that uses locally defined configuration.
   *
   * @param  config  The local configuration to use in place of the global configuration.
   * @see [[play.filters.cors.CORSConfig]]
   */
  def apply(
    config: CORSConfig,
    errorHandler: HttpErrorHandler,
    parserConfig: ParserConfiguration,
    tempFileCreator: TemporaryFileCreator)(implicit materializer: Materializer, ec: ExecutionContext): CORSActionBuilder = {
    val eh = errorHandler
    new CORSActionBuilder {
      override lazy val parser = new BodyParsers.Default(tempFileCreator, eh, parserConfig)(materializer)
      override protected def mat: Materializer = materializer
      override protected val executionContext: ExecutionContext = ec
      override protected val corsConfig: CORSConfig = config
      override protected val errorHandler: HttpErrorHandler = eh
    }
  }
}
