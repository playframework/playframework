/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.http

import javax.inject._

import akka.stream.Materializer
import play.api.i18n._
import play.api.inject.Injector
import play.api.mvc.{ EssentialFilter, Filter, RequestHeader, Result }
import play.api.{ Configuration, Environment }

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
private[play] class MessagesApiSystemFilter @Inject() (messagesApi: MessagesApi)(override implicit val mat: Materializer) extends Filter {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(rh.withAttr(RequestAttributes.MessagesApiAttr, messagesApi))
  }
}

class MessagesApiSystemFilterProvider @Inject() (injector: Injector, environment: Environment, configuration: Configuration)(implicit val mat: Materializer) extends Provider[MessagesApiSystemFilter] {

  lazy val get = {
    val messagesApi = try {
      injector.instanceOf[MessagesApi]
    } catch {
      case e: Exception =>
        val migrationUrl = "https://www.playframework.com/documentation/latest/I18nMigration26"
        val msg = s"No MessagesApi binding found, please see $migrationUrl"
        if (useFallback) {
          logger.warn(msg)
          generateDefaultMessagesApi
        } else {
          throw new IllegalStateException(msg)
        }
    }

    new MessagesApiSystemFilter(messagesApi)
  }

  // If we inject MessagesApi in the constructor, then it causes DI failure
  // without context for why it's there.  So, we want to defer MessagesApi
  // initialization until the last possible moment, and provide a decent
  // error message if it is not found, and provide a fallback.
  //
  // This means that already written tests that does not execute a request
  // will not explode for reasons they don't care about.
  //
  // Using the injector also means that compile time dependency injection does
  // not need to directly rely on MessagesApi, or tie BuiltinComponents with
  // I18nComponents, which has its own implementation of MessagesApi.  This is
  // fine, because SystemFilters have a def and a default implementation that
  // sets this up, so even compile time DI users will not have to touch this.
  //
  // This is because some existing apps / tests do not use I18nModule, and
  // Play's handling of MessagesApi has been inconsistent.  Let's go into
  // some more detail as to why MessagesApi is not optional.
  //
  // Play's error handling is tied directly to Messages and MessagesApi, and
  // anything that goes through validation returns a message key -- likewise,
  // anything involving REST resources involves the `Accept-Language` header and
  // so touches on messages as well.  If you want to use form helpers, you have
  // to use an implicit Messages.  If you want to render `form.errorsAsJson`,
  // you have to use an implicit Messages.
  //
  // It might seem that you can use Play without using MessagesApi, because the
  // I18nModule is broken out from BuiltInModule.  This isn't actually true:
  // while you can disable I18nModule from application.conf or from
  // GuiceApplicationBuilder and BuiltInComponents does not extend I18n,
  // it's useful for overriding, so that a binding doesn't already exist when
  // a custom implementation of MessagesApi is provided by a custom I18nModule.
  //
  // In 2.5.x, BuiltInComponentsFromContext does not extend I18nComponents:
  //
  // https://www.playframework.com/documentation/2.5.x/ScalaCompileTimeDependencyInjection
  //
  // This is actually a mistake, because runtime DI has I18nModule loaded
  // automatically in reference.conf:
  //
  // # The enabled modules that should be automatically loaded.
  // enabled += "play.api.inject.BuiltinModule"
  // enabled += "play.api.i18n.I18nModule"
  //
  // and BuiltInComponentsFromContext should match its runtime DI equivalent.
  //
  // However, the fact that Messages is primarily involved in front end rendering
  // has meant that it can be excluded from tests that only test for the presence
  // of FormError rather than the text value resulting from the message key...
  // meaning there are undoubtably tests that extend from BuiltInComponents directly.
  //
  // So, that's why MessageApi is not optional in a Play application.  Virtually
  // everything front-end needs it, and it shouldn't have to be added explicitly.
  //
  // So, why put the MessageApi in as a request attribute?
  //
  // That is a larger story, but is adequately expressed in 140 characters:
  //
  // "Trying to guide a Play beginner through all the steps needed to add a simple
  // form to a Play 2.4.x app is just embarrassing"
  //
  // -- https://twitter.com/cbirchall/status/679359109234339840
  //
  // This is described in more detail in https://github.com/playframework/playframework/pull/6438
  //
  // Form helpers require a Messages instance.
  // Form helpers can be defined inside several layers of helper Twirl templates.
  // An implicit Messages has to be passed through as an implicit through
  // every wrapping template, all the way from a controller.
  //
  // By hand, a controller using a form must mixin I18nSupport, inject MessagesApi, and
  // also pass in an implicit request through the template if CSRF is used.
  //
  // There are cases in template where Messages("foo") is used stand-alone, without a Form,
  // so it's not enough to simply tie a Messages to a Form instance.  If a message
  // needs to be rendered and we have the language available, it should be rendered.
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  /**
   * Defines a fallback MessagesApi object in the case that one is not explicitly bound.
   *
   * Set this using `-Dplay.i18n.fallback=true|false` or in application.conf as necessary.
   *
   * @return true if `play.i18n.fallback` is true, false otherwise.
   */
  private def useFallback: Boolean = {
    configuration.get[Boolean]("play.i18n.fallback")
  }

  /**
   * Generates a DefaultMessagesApi from configuration by hand.
   *
   * @return DefaultMessagesApi
   */
  private def generateDefaultMessagesApi: MessagesApi = {
    val attr = RequestAttributes.MessagesApiAttr
    val langs = new DefaultLangsProvider(configuration).get
    val messagesApi = new DefaultMessagesApiProvider(environment, configuration, langs).get
    logger.debug(s"Generating MessagesApi $messagesApi for request attribute $attr")
    messagesApi
  }

}
