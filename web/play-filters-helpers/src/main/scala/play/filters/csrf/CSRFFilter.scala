/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csrf

import javax.inject.{ Inject, Provider }

import akka.stream.Materializer
import play.api.http.SessionConfiguration
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc._
import play.core.j.JavaContextComponents
import play.filters.csrf.CSRF._

/**
 * A filter that provides CSRF protection.
 *
 * These must be by name parameters because the typical use case for instantiating the filter is in Global, which
 * happens before the application is started.  Since the default values for the parameters are loaded from config
 * and hence depend on a started application, they must be by name.
 *
 * @param config A csrf configuration object
 * @param tokenSigner the CSRF token signer.
 * @param tokenProvider A token provider to use.
 * @param errorHandler handling failed token error.
 */
class CSRFFilter(
    config: => CSRFConfig,
    tokenSigner: => CSRFTokenSigner,
    sessionConfiguration: => SessionConfiguration,
    val tokenProvider: TokenProvider,
    val errorHandler: ErrorHandler = CSRF.DefaultErrorHandler)(implicit mat: Materializer) extends EssentialFilter {

  @Inject
  def this(config: Provider[CSRFConfig], tokenSignerProvider: Provider[CSRFTokenSigner], sessionConfiguration: SessionConfiguration, tokenProvider: TokenProvider, errorHandler: ErrorHandler)(mat: Materializer) = {
    this(config.get, tokenSignerProvider.get, sessionConfiguration, tokenProvider, errorHandler)(mat)
  }

  // Java constructor for manually constructing the filter
  def this(config: CSRFConfig, tokenSigner: play.libs.crypto.CSRFTokenSigner, sessionConfiguration: SessionConfiguration, tokenProvider: TokenProvider, errorHandler: CSRFErrorHandler, contextComponents: JavaContextComponents)(mat: Materializer) = {
    this(config, tokenSigner.asScala, sessionConfiguration, tokenProvider, new JavaCSRFErrorHandlerAdapter(errorHandler, contextComponents))(mat)
  }

  def apply(next: EssentialAction): EssentialAction = new CSRFAction(next, config, tokenSigner, tokenProvider, sessionConfiguration, errorHandler)

}
