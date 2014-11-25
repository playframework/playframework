/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import javax.inject.{ Provider, Inject }
import play.api.mvc._
import play.filters.csrf.CSRF.{ Config, TokenProvider, ErrorHandler }

/**
 * A filter that provides CSRF protection.
 *
 * These must be by name parameters because the typical use case for instantiating the filter is in Global, which
 * happens before the application is started.  Since the default values for the parameters are loaded from config
 * and hence depend on a started application, they must be by name.
 *
 * @param conf A csrf configuration object
 * @param tokenProvider A token provider to use.
 * @param errorHandler handling failed token error.
 */
class CSRFFilter(
    conf: => Config = CSRFConf.defaultConfig,
    val tokenProvider: TokenProvider = CSRFConf.defaultTokenProvider,
    val errorHandler: ErrorHandler = CSRF.DefaultErrorHandler) extends EssentialFilter {

  def config: Config = conf

  @Inject
  def this(configProvider: Provider[Config], tokenProvider: TokenProvider, errorHandler: ErrorHandler) = {
    this(configProvider.get, tokenProvider, errorHandler)
  }

  /**
   * Default constructor, useful from Java
   */
  def this() = this(Config(), CSRFConf.defaultTokenProvider, CSRFConf.defaultJavaErrorHandler)

  def apply(next: EssentialAction): EssentialAction = new CSRFAction(next, conf, tokenProvider, errorHandler)
}

object CSRFFilter {
  def apply(
    conf: => Config = CSRFConf.defaultConfig,
    tokenProvider: TokenProvider = CSRFConf.defaultTokenProvider,
    errorHandler: ErrorHandler = CSRF.DefaultErrorHandler): CSRFFilter = {
    new CSRFFilter(conf, tokenProvider, errorHandler)
  }
}
