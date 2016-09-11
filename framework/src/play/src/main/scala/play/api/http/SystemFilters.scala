/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject._

import akka.stream.Materializer
import play.api.i18n._
import play.api.inject.Injector
import play.api.mvc.{ EssentialFilter, Filter, RequestHeader, Result }
import play.api.{ Configuration, Environment, Mode }

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
class MessagesApiSystemFilter @Inject() (messagesApi: MessagesApi)(override implicit val mat: Materializer) extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(rh.withAttr(RequestAttributes.MessagesApiAttr, messagesApi))
  }
}

/**
 * Creates MessagesApiSystemFilter using either a [[MessagesApi]] instance found from the injector, or,
 * if `play.i18n.fallback = true`) a fallback empty instance of [[DefaultMessagesApi]].
 *
 * If `play.i18n.fallback` is false and no binding is found, then an IllegalStateException is thrown.
 *
 * This is used in BuiltinModule, because BuiltInModule doesn't define MessagesApi itself.
 */
@Singleton
class MessagesApiSystemFilterProvider @Inject() (injector: Injector,
  environment: Environment,
  configuration: Configuration)(implicit val mat: Materializer)
    extends Provider[MessagesApiSystemFilter] {

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  lazy val get = {
    val messagesApi = try {
      injector.instanceOf[MessagesApi]
    } catch {
      case e: Exception =>
        val migrationUrl = "https://www.playframework.com/documentation/latest/I18nMigration26"
        val msg = s"No MessagesApi binding found, please see $migrationUrl"
        if (useFallback) {
          environment.mode match {
            case Mode.Prod =>
              logger.error(msg)
              logger.error("Using empty fallback MessagesApi instance...")
            case other =>
              logger.warn(msg)
              logger.warn("Using empty fallback MessagesApi instance...")
          }
          generateDefaultMessagesApi
        } else {
          throw new IllegalStateException(msg, e)
        }
    }

    new MessagesApiSystemFilter(messagesApi)
  }

  /**
   * Defines a fallback MessagesApi object in the case that one is not explicitly bound.
   *
   * Set this using `-Dplay.i18n.fallback=true|false` or in application.conf as necessary.
   *
   * @return true if `play.i18n.fallback` is true, false otherwise.
   */
  protected def useFallback: Boolean = {
    configuration.get[Boolean]("play.i18n.fallback")
  }

  /**
   * Generates an empty DefaultMessagesApi.
   *
   * @return DefaultMessagesApi
   */
  protected def generateDefaultMessagesApi: MessagesApi = {
    val attr = RequestAttributes.MessagesApiAttr
    val langs = new DefaultLangs()
    val messagesApi = new DefaultMessagesApi(langs = langs)
    logger.debug(s"Generating empty MessagesApi $messagesApi for request attribute $attr")
    messagesApi
  }

}
