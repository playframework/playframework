/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

import akka.stream.Materializer
import akka.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.Configuration

/**
 * This trait is used to give a CSP header to the result for a single action.
 *
 * To use this in a controller, add something like the following:
 *
 * {{{
 * class CSPActionController @Inject()(cspAction: CSPActionBuilder, cc: ControllerComponents)
 *  extends AbstractController(cc) {
 *   def index = cspAction { implicit request =>
 *     Ok("result containing CSP")
 *   }
 * }
 * }}}
 */
trait CSPActionBuilder extends ActionBuilder[Request, AnyContent] {
  protected def cspResultProcessor: CSPResultProcessor

  protected def mat: Materializer

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    @inline def action[R](request: Request[A], block: Request[A] => Future[Result])(
        implicit ev: R =:= Request[A]
    ) = {
      new EssentialAction {
        override def apply(req: RequestHeader): Accumulator[ByteString, Result] = {
          req match {
            case r: R => Accumulator.done(block(r))
            case _    => Accumulator.done(block(req.withBody(request.body)))
          }
        }
      }
    }

    cspResultProcessor(action(request, block), request).run()(mat)
  }
}

/**
 * This singleton object contains factory methods for creating new CSPActionBuilders.
 *
 * Useful in compile time dependency injection.
 */
object CSPActionBuilder {

  /**
   * Creates a new CSPActionBuilder using a Configuration and bodyParsers instance.
   */
  def apply(config: Configuration, bodyParsers: PlayBodyParsers)(
      implicit materializer: Materializer,
      ec: ExecutionContext
  ): CSPActionBuilder = {
    apply(CSPResultProcessor(CSPProcessor(CSPConfig.fromConfiguration(config))), bodyParsers)
  }

  /**
   * Creates a new CSPActionBuilder using a configured CSPProcessor and bodyParsers instance.
   */
  def apply(processor: CSPResultProcessor, bodyParsers: PlayBodyParsers)(
      implicit materializer: Materializer,
      ec: ExecutionContext
  ): CSPActionBuilder = {
    new DefaultCSPActionBuilder(processor, bodyParsers)
  }
}

/**
 * The default CSPActionBuilder.
 *
 * This is useful for runtime dependency injection.
 *
 * @param cspResultProcessor injected processor
 * @param bodyParsers injected body parsers
 * @param executionContext injected execution context
 * @param mat injected materializer.
 */
@Singleton
class DefaultCSPActionBuilder @Inject() (
    protected override val cspResultProcessor: CSPResultProcessor,
    bodyParsers: PlayBodyParsers
)(
    implicit protected override val executionContext: ExecutionContext,
    protected override val mat: Materializer
) extends CSPActionBuilder {
  override def parser: BodyParser[AnyContent] = bodyParsers.default
}
