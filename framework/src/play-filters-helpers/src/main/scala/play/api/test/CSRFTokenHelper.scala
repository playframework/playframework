/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.test

import play.api.http.{ SecretConfiguration, SessionConfiguration }
import play.api.libs.crypto.{ CSRFTokenSigner, CSRFTokenSignerProvider, DefaultCookieSigner }
import play.api.mvc.{ Request }
import play.filters.csrf.{ CSRFActionHelper, CSRFConfig }

/**
 * Exposes methods to make using requests with CSRF tokens easier.
 */
object CSRFTokenHelper {

  private val sessionConfiguration = SessionConfiguration()

  private val csrfConfig = CSRFConfig()

  private val csrfTokenSigner: CSRFTokenSigner = new CSRFTokenSignerProvider(new DefaultCookieSigner(SecretConfiguration())).get

  private val csrfActionHelper = new CSRFActionHelper(
    sessionConfiguration = sessionConfiguration,
    csrfConfig = csrfConfig,
    tokenSigner = csrfTokenSigner
  )

  /**
   * Adds a CSRF token to the request, using the Scala Request API.
   *
   * @param request a request
   * @tparam A the body type
   * @return a request with a CSRF token attached.
   */
  def addCSRFToken[A](request: Request[A]): Request[A] = {
    csrfActionHelper.tagRequestWithNewToken(request)
  }

  /**
   * Adds a CSRF token to the request, using the Java RequestBuilder API.
   */
  def addCSRFToken(requestBuilder: play.mvc.Http.RequestBuilder): play.mvc.Http.RequestBuilder = {
    csrfActionHelper.tagRequestWithNewToken(requestBuilder)
  }

  /**
   * Implicit class for enriching fakeRequest
   *
   * @param fakeRequest the fake request
   * @tparam T
   */
  implicit class CSRFFakeRequest[T](fakeRequest: FakeRequest[T]) {
    def withCSRFToken: FakeRequest[T] = {
      CSRFTokenHelper.addCSRFToken(fakeRequest).asInstanceOf[FakeRequest[T]]
    }
  }
}
