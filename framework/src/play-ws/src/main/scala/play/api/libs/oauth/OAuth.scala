/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.oauth

import _root_.oauth.signpost.basic.DefaultOAuthConsumer
import _root_.oauth.signpost.commonshttp.CommonsHttpOAuthProvider
import _root_.oauth.signpost.exception.OAuthException
import com.ning.http.client.oauth.OAuthSignatureCalculator
import com.ning.http.client.{ Request, RequestBuilderBase, SignatureCalculator }
import play.api.libs.ws.WSSignatureCalculator

/**
 * Library to access resources protected by OAuth 1.0a.
 *  @param info the service information, including the required URLs and the application id and secret
 *  @param use10a whether the service should use the 1.0 version of the spec, or the 1.0a version fixing a security issue.
 *  You must use the version corresponding to the
 */
case class OAuth(info: ServiceInfo, use10a: Boolean = true) {

  private val provider = {
    val p = new CommonsHttpOAuthProvider(info.requestTokenURL, info.accessTokenURL, info.authorizationURL)
    p.setOAuth10a(use10a)
    p
  }

  /**
   * Request the request token and secret.
   *
   * @param callbackURL the URL where the provider should redirect to (usually a URL on the current app)
   * @return A Right(RequestToken) in case of success, Left(OAuthException) otherwise
   */
  def retrieveRequestToken(callbackURL: String): Either[OAuthException, RequestToken] = {
    val consumer = new DefaultOAuthConsumer(info.key.key, info.key.secret)
    try {
      provider.retrieveRequestToken(consumer, callbackURL)
      Right(RequestToken(consumer.getToken(), consumer.getTokenSecret()))
    } catch {
      case e: OAuthException => Left(e)
    }
  }

  /**
   * Exchange a request token for an access token.
   *
   * @param token the token/secret pair obtained from a previous call
   * @param verifier a string you got through your user, with redirection
   * @return A Right(RequestToken) in case of success, Left(OAuthException) otherwise
   */
  def retrieveAccessToken(token: RequestToken, verifier: String): Either[OAuthException, RequestToken] = {
    val consumer = new DefaultOAuthConsumer(info.key.key, info.key.secret)
    consumer.setTokenWithSecret(token.token, token.secret)
    try {
      provider.retrieveAccessToken(consumer, verifier)
      Right(RequestToken(consumer.getToken(), consumer.getTokenSecret()))
    } catch {
      case e: OAuthException => Left(e)
    }
  }

  /**
   * The URL where the user needs to be redirected to grant authorization to your application.
   *
   * @param token request token
   */
  def redirectUrl(token: String): String = {
    _root_.oauth.signpost.OAuth.addQueryParameters(
      provider.getAuthorizationWebsiteUrl(),
      _root_.oauth.signpost.OAuth.OAUTH_TOKEN,
      token
    )
  }

}

/**
 * A consumer key / consumer secret pair that the OAuth provider gave you, to identify your application.
 */
case class ConsumerKey(key: String, secret: String)

/**
 * A request token / token secret pair, to be used for a specific user.
 */
case class RequestToken(token: String, secret: String)

/**
 * The information identifying a oauth provider: URLs and the consumer key / consumer secret pair.
 */
case class ServiceInfo(requestTokenURL: String, accessTokenURL: String, authorizationURL: String, key: ConsumerKey)

/**
 * The public AsyncHttpClient implementation of WSSignatureCalculator.
 */
class OAuthCalculator(consumerKey: ConsumerKey, requestToken: RequestToken) extends WSSignatureCalculator with SignatureCalculator {

  import com.ning.http.client.oauth.{ ConsumerKey => AHCConsumerKey, RequestToken => AHCRequestToken }

  private val ahcConsumerKey = new AHCConsumerKey(consumerKey.key, consumerKey.secret)
  private val ahcRequestToken = new AHCRequestToken(requestToken.token, requestToken.secret)
  private val calculator = new OAuthSignatureCalculator(ahcConsumerKey, ahcRequestToken)

  override def calculateAndAddSignature(request: Request, requestBuilder: RequestBuilderBase[_]): Unit = {
    calculator.calculateAndAddSignature(request, requestBuilder)
  }
}

/**
 * Object for creating signature calculator for the Play WS API.
 *
 * Example:
 * {{{
 * import play.api.libs.oauth._
 * val consumerKey: ConsumerKey = ConsumerKey(twitterConsumerKey, twitterConsumerSecret)
 * val requestToken: RequestToken = RequestToken(accessTokenKey, accessTokenSecret)
 * WS.url("http://example.com/protected").sign(OAuthCalculator(consumerKey, requestToken)).get()
 * }}}
 */
object OAuthCalculator {
  def apply(consumerKey: ConsumerKey, token: RequestToken): WSSignatureCalculator = {
    new OAuthCalculator(consumerKey, token)
  }
}
