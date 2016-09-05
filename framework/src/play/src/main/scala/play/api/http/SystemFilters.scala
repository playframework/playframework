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

private[play] trait JavaSystemFilters extends SystemFilters

object SystemFilters {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Reflect.bindingsFromConfiguration[SystemFilters, JavaSystemFilters, JavaSystemFiltersAdapter, JavaSystemFiltersDelegate, DefaultSystemFilters](environment, configuration, "play.http.systemfilters", "play.api.http.DefaultSystemFilters")
  }

  def apply(list: EssentialFilter*): SystemFilters = new SystemFilters {
    override val filters: Seq[EssentialFilter] = list
  }
}

abstract class AbstractSystemFilters(val filters: EssentialFilter*) extends SystemFilters

private class NoSystemFilters @Inject() () extends AbstractSystemFilters

private class JavaSystemFiltersAdapter @Inject() (underlying: JavaSystemFilters) extends AbstractSystemFilters(underlying.filters: _*)

private class JavaSystemFiltersDelegate @Inject() (delegate: SystemFilters) extends JavaSystemFilters {
  override def filters: Seq[EssentialFilter] = delegate.filters
}

private[play] class DefaultSystemFilters @Inject() (messagesApiSystemFilter: MessagesApiSystemFilter) extends AbstractSystemFilters(messagesApiSystemFilter)

/**
 * Adds a request attribute to the request with the key [[RequestAttributes.MessagesApiAttr]]
 * to the dependency injected messagesApi instance.
 *
 * This filter is required for Messages functionality.
 */
@Singleton
private[play] class MessagesApiSystemFilter @Inject() (injector: Injector)(override implicit val mat: Materializer) extends Filter {

  private lazy val messagesApi = {
    try {
      injector.instanceOf[MessagesApi]
    } catch {
      case e: Exception =>
        new DefaultMessagesApi()
    }
  }

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(rh.withAttr(RequestAttributes.MessagesApiAttr, messagesApi))
  }
}
