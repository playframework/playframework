/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.util.concurrent.atomic.AtomicInteger

import play.api.Application
import play.api.http.HttpConfiguration
import play.api.libs.crypto.CookieSignerProvider
import play.api.mvc.{ DefaultCookieHeaderEncoding, DefaultFlashCookieBaker, DefaultSessionCookieBaker }
import play.api.mvc.request.DefaultRequestFactory
import play.core.server.ServerProvider
import play.utils.InlineCache

import scala.util.{ Failure, Success, Try }

/**
 * Helps a `Server` to cache objects that change when an `Application` is reloaded.
 *
 * Subclasses should override the `reloadValue` method, which will be called
 * when the `Application` changes, and then cached. (Caching is provided by `InlineCache`,
 * so read its docs for the threading semantics.) Users should call
 * `cachedValue` to get the cached value.
 */
private[play] abstract class ReloadCache[+T] {

  /**
   * The count of how many times the cache has been reloaded. Due to the semantics of InlineCache this value
   * could be called up to once per thread per application change.
   */
  private val reloadCounter = new AtomicInteger(0)

  private[play] final def reloadCount: Int = reloadCounter.get

  private val reloadCache: Try[Application] => T = new InlineCache[Try[Application], T]({ tryApp: Try[Application] =>
    reloadCounter.incrementAndGet()
    reloadValue(tryApp)
  })

  /**
   * Get the cached `T` for the given application. If the application has changed
   * then `reloadValue` will be called to calculate a fresh value.
   */
  final def cachedFrom(tryApp: Try[Application]): T = reloadCache(tryApp)

  /**
   * Calculate a fresh `T` for the given application.
   */
  protected def reloadValue(tryApp: Try[Application]): T

  /**
   * Helper to calculate the [[ServerDebugInfo]] after a reload.
   * @param tryApp The application being loaded.
   * @param serverProvider The server which embeds the application.
   */
  protected final def reloadDebugInfo(tryApp: Try[Application], serverProvider: ServerProvider): Option[ServerDebugInfo] = {
    val enabled: Boolean = tryApp match {
      case Success(app) => app.configuration.get[Boolean]("play.server.debug.addDebugInfoToRequests")
      case Failure(_) => true // Always enable debug info when the app fails to load
    }
    if (enabled) {
      Some(ServerDebugInfo(
        serverProvider = serverProvider,
        serverConfigCacheReloads = reloadCount
      ))
    } else None
  }

  /**
   * Helper to calculate a `ServerResultUtil`.
   */
  protected final def reloadServerResultUtils(tryApp: Try[Application]): ServerResultUtils = {
    val (sessionBaker, flashBaker, cookieHeaderEncoding) = tryApp match {
      case Success(app) =>
        val requestFactory: DefaultRequestFactory = app.requestFactory match {
          case drf: DefaultRequestFactory => drf
          case _ => new DefaultRequestFactory(app.httpConfiguration)
        }

        (
          requestFactory.sessionBaker,
          requestFactory.flashBaker,
          requestFactory.cookieHeaderEncoding
        )
      case Failure(_) =>
        val httpConfig = HttpConfiguration()
        val cookieSigner = new CookieSignerProvider(httpConfig.secret).get

        (
          new DefaultSessionCookieBaker(httpConfig.session, httpConfig.secret, cookieSigner),
          new DefaultFlashCookieBaker(httpConfig.flash, httpConfig.secret, cookieSigner),
          new DefaultCookieHeaderEncoding(httpConfig.cookies)
        )
    }
    new ServerResultUtils(sessionBaker, flashBaker, cookieHeaderEncoding)
  }

  /**
   * Helper to calculate a `ForwardedHeaderHandler`.
   */
  protected final def reloadForwardedHeaderHandler(tryApp: Try[Application]): ForwardedHeaderHandler = {
    val forwardedHeaderConfiguration =
      ForwardedHeaderHandler.ForwardedHeaderHandlerConfig(tryApp.toOption.map(_.configuration))
    new ForwardedHeaderHandler(forwardedHeaderConfiguration)
  }
}
