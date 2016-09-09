/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http
import javax.inject._

import akka.stream.Materializer
import play.api.i18n.{ DefaultMessagesApi, MessagesApi, RequestAttributes }
import play.api.inject.{ Binding, Injector }
import play.api.mvc.{ EssentialFilter, Filter, RequestHeader, Result }
import play.api.{ Configuration, Environment }
import play.utils.Reflect

import scala.concurrent.Future

/**
 * System filters are an internal part of Play API, and should not be
 * extended or used by modules or applications.
 */
trait SystemFilters {
  def filters: Seq[EssentialFilter]
}

private[play] class DefaultSystemFilters @Inject() (messagesApiSystemFilter: MessagesApiSystemFilter) extends SystemFilters {
  override val filters: Seq[EssentialFilter] = Seq(messagesApiSystemFilter)
}

/**
 * Adds a request attribute to the request with the key [[RequestAttributes.MessagesApiAttr]]
 * to the dependency injected messagesApi instance.
 *
 * This filter is required for Messages functionality.
 */
@Singleton
private[play] class MessagesApiSystemFilter @Inject() (injector: Injector)(override implicit val mat: Materializer) extends Filter {

  private val inlineCache: (Injector => MessagesApi) = {
    new play.utils.InlineCache((inj: Injector) =>
      try {
        inj.instanceOf[MessagesApi]
      } catch {
        case e: Exception =>
          new DefaultMessagesApi()
      })
  }

  private val messagesApi: MessagesApi = inlineCache(injector)

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(rh.withAttr(RequestAttributes.MessagesApiAttr, messagesApi))
  }
}
