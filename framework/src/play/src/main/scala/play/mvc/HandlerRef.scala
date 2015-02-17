package play.mvc;

import play.core.Router.{ HandlerInvoker, HandlerInvokerFactory }
import play.api.mvc._;

/**
 * Reference to a Handler, useful for contructing handlers from Java code.
 */
class HandlerRef[T](call: => T, handlerDef: play.core.Router.HandlerDef)(implicit hif: play.core.Router.HandlerInvokerFactory[T]) {

  private lazy val invoker: HandlerInvoker[T] = hif.createInvoker(call, handlerDef)

  /**
   * Retrieve a real handler behind this ref.
   */
  def handler: play.api.mvc.Handler = {
    invoker.call(call)
  }

  /**
   * String representation of this Handler.
   */
  private lazy val sym = {
    handlerDef.controller + "." + handlerDef.method + "(" + handlerDef.parameterTypes.map(_.getName).mkString(", ") + ")"
  }

  override def toString = {
    "HandlerRef[" + sym + ")]"
  }

}
