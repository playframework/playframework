/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.mvc.Results._
import play.api.mvc._
import play.api.test._
import play.api.Application

class ScalaResultsSpec extends PlaySpecification {

  sequential

  def cookieHeaderEncoding(implicit app: Application): CookieHeaderEncoding = app.injector.instanceOf[CookieHeaderEncoding]
  def sessionBaker(implicit app: Application): SessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]
  def flashBaker(implicit app: Application): FlashCookieBaker = app.injector.instanceOf[FlashCookieBaker]

  def bake(result: Result)(implicit app: Application): Result = {
    result.bakeCookies(cookieHeaderEncoding, sessionBaker, flashBaker)
  }

  def cookies(result: Result)(implicit app: Application): Seq[Cookie] = {
    cookieHeaderEncoding.decodeSetCookieHeader(bake(result).header.headers("Set-Cookie"))
  }

  "support session helper" in withApplication() { implicit app =>

    sessionBaker.decode("  ").isEmpty must be_==(true)

    val data = Map("user" -> "kiki", "langs" -> "fr:en:de")
    val encodedSession = sessionBaker.encode(data)
    val decodedSession = sessionBaker.decode(encodedSession)

    decodedSession must_== Map("user" -> "kiki", "langs" -> "fr:en:de")
    val Result(ResponseHeader(_, headers, _), _, _, _, _) = bake {
      Ok("hello").as("text/html")
        .withSession("user" -> "kiki", "langs" -> "fr:en:de")
        .withCookies(Cookie("session", "items"), Cookie("preferences", "blue"))
        .discardingCookies(DiscardingCookie("logged"))
        .withSession("user" -> "kiki", "langs" -> "fr:en:de")
        .withCookies(Cookie("lang", "fr"), Cookie("session", "items2"))
    }

    val setCookies = cookieHeaderEncoding.decodeSetCookieHeader(headers("Set-Cookie")).map(c => c.name -> c).toMap
    setCookies.size must be_==(5)
    setCookies("session").value must be_==("items2")
    setCookies("preferences").value must be_==("blue")
    setCookies("lang").value must be_==("fr")
    setCookies("logged").maxAge must beSome
    setCookies("logged").maxAge must beSome(Cookie.DiscardedMaxAge)
    val playSession = sessionBaker.decodeFromCookie(setCookies.get(sessionBaker.COOKIE_NAME))
    playSession.data must_== Map("user" -> "kiki", "langs" -> "fr:en:de")
  }

  "bake cookies should not depends on global state" in withApplication("play.allowGlobalApplication" -> false) { implicit app =>
    Ok.bakeCookies(cookieHeaderEncoding, sessionBaker, flashBaker) must not(beNull) // we are interested just that it executes without global state
  }

  "support a custom application context" in {
    "set session on right path" in withFooPath { implicit app =>
      cookies(Ok.withSession("user" -> "alice")).head.path must_== "/foo"
    }

    "discard session on right path" in withFooPath { implicit app =>
      cookies(Ok.withNewSession).head.path must_== "/foo"
    }

    "set flash on right path" in withFooPath { implicit app =>
      cookies(Ok.flashing("user" -> "alice")).head.path must_== "/foo"
    }

    // flash cookie is discarded in PlayDefaultUpstreamHandler
  }

  "support a custom session domain" in {
    "set session on right domain" in withFooDomain { implicit app =>
      cookies(Ok.withSession("user" -> "alice")).head.domain must beSome(".foo.com")
    }

    "discard session on right domain" in withFooDomain { implicit app =>
      cookies(Ok.withNewSession).head.domain must beSome(".foo.com")
    }
  }

  "support a secure session" in {
    "set session as secure" in withSecureSession { implicit app =>
      cookies(Ok.withSession("user" -> "alice")).head.secure must_== true
    }

    "discard session as secure" in withSecureSession { implicit app =>
      cookies(Ok.withNewSession).head.secure must_== true
    }
  }

  "support legacy cookie bakers" in {
    "legacy session baker should work normally" in withLegacyCookiesModule { implicit app =>
      sessionBaker must beAnInstanceOf[LegacySessionCookieBaker]

      val data = Map("user" -> "kiki", "langs" -> "fr:en:de")
      val encodedSession = sessionBaker.encode(data)
      val decodedSession = sessionBaker.decode(encodedSession)
      decodedSession must_== Map("user" -> "kiki", "langs" -> "fr:en:de")
    }

    "legacy flash baker should work normally" in withLegacyCookiesModule { implicit app =>
      flashBaker must beAnInstanceOf[LegacyFlashCookieBaker]

      val data = Map("message" -> "success")
      val encodedSession = flashBaker.encode(data)
      val decodedSession = flashBaker.decode(encodedSession)
      decodedSession must_== Map("message" -> "success")
    }
  }

  def withApplication[T](config: (String, Any)*)(block: Application => T): T = running(
    _.configure(Map(config: _*) + ("play.http.secret.key" -> "ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"))
  )(block)

  def withFooDomain[T](block: Application => T) = withApplication("play.http.session.domain" -> ".foo.com")(block)

  def withSecureSession[T](block: Application => T) = withApplication("play.http.session.secure" -> true)(block)

  def withFooPath[T](block: Application => T) = {
    val path = "/foo"
    withApplication(
      "play.http.context" -> path,
      "play.http.session.path" -> path,
      "play.http.flash.path" -> path
    )(block)
  }

  def withLegacyCookiesModule[T](block: Application => T) = withApplication(
    "play.modules.disabled" -> Seq("play.api.mvc.CookiesModule"),
    "play.modules.enabled" -> Seq("play.api.i18n.I18nModule", "play.api.inject.BuiltinModule", "play.api.mvc.LegacyCookiesModule")
  )(block)
}
