/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import javax.inject.{ Provider, Inject }
import play.api.mvc._
import play.filters.csrf.CSRF._

/**
 * A filter that provides CSRF protection.
 *
 * These must be by name parameters because the typical use case for instantiating the filter is in Global, which
 * happens before the application is started.  Since the default values for the parameters are loaded from config
 * and hence depend on a started application, they must be by name.
 *
 * @param config A csrf configuration object
 * @param tokenProvider A token provider to use.
 * @param errorHandler handling failed token error.
 */
class CSRFFilter(
    config: => CSRFConfig,
    val tokenProvider: TokenProvider = SignedTokenProvider,
    val errorHandler: ErrorHandler = CSRF.DefaultErrorHandler) extends EssentialFilter {

  @Inject
  def this(config: Provider[CSRFConfig], tokenProvider: TokenProvider, errorHandler: ErrorHandler) = {
    this(config.get, tokenProvider, errorHandler)
  }

  /**
   * Default constructor, useful from Java
   */
  def this() = this(CSRFConfig.global, new ConfigTokenProvider(CSRFConfig.global), DefaultErrorHandler)

  def apply(next: EssentialAction): EssentialAction = new CSRFAction(next, config, tokenProvider, errorHandler)
}

object CSRFFilter {
  def apply(
    config: => CSRFConfig = CSRFConfig.global,
    tokenProvider: TokenProvider = new ConfigTokenProvider(CSRFConfig.global),
    errorHandler: ErrorHandler = DefaultErrorHandler): CSRFFilter = {
    new CSRFFilter(config, tokenProvider, errorHandler)
  }
}
