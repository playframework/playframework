/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import javax.inject.{ Provider, Inject }
import akka.stream.Materializer
import play.api.libs.Crypto
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
    crypto: => Crypto,
    val tokenProvider: TokenProvider = SignedTokenProvider,
    val errorHandler: ErrorHandler = CSRF.DefaultErrorHandler)(implicit mat: Materializer) extends EssentialFilter {

  @Inject
  def this(config: Provider[CSRFConfig], crypto: Provider[Crypto], tokenProvider: TokenProvider, errorHandler: ErrorHandler)(mat: Materializer) = {
    this(config.get, crypto.get, tokenProvider, errorHandler)(mat)
  }

  // Java constructor for manually constructing the filter
  def this(config: CSRFConfig, crypto: play.libs.Crypto, tokenProvider: TokenProvider, errorHandler: CSRFErrorHandler)(mat: Materializer) = {
    this(config, crypto.asScala, tokenProvider, new JavaCSRFErrorHandlerAdapter(errorHandler))(mat)
  }

  /**
   * Default constructor, useful from Java
   *
   * @deprecated in 2.5.0. This constructor uses global state.
   */
  @Deprecated
  def this()(implicit mat: Materializer) = this(CSRFConfig.global, Crypto.crypto, new ConfigTokenProvider(CSRFConfig.global), DefaultErrorHandler)

  def apply(next: EssentialAction): EssentialAction = new CSRFAction(next, config, crypto, tokenProvider, errorHandler)
}

object CSRFFilter {
  @deprecated("Use dependency injection", "2.5.0")
  def apply(
    config: => CSRFConfig = CSRFConfig.global,
    crypto: => Crypto = Crypto.crypto,
    tokenProvider: TokenProvider = new ConfigTokenProvider(CSRFConfig.global),
    errorHandler: ErrorHandler = DefaultErrorHandler)(implicit mat: Materializer): CSRFFilter = {
    new CSRFFilter(config, crypto, tokenProvider, errorHandler)
  }
}
