package play.api

import _root_.oauth.signpost.{ OAuthConsumer, OAuthProvider }
import _root_.oauth.signpost.exception.OAuthException
import _root_.oauth.signpost.basic.{ DefaultOAuthConsumer, DefaultOAuthProvider }
import oauth._

/**
 * Library to access ressources protected by OAuth 1.0a.
 *
 */

case class OAuth(info: ServiceInfo, use10a: Boolean = true) {

  private val provider = new DefaultOAuthProvider(info.requestTokenURL, info.accessTokenURL, info.authorizationURL)
  provider.setOAuth10a(use10a)

  /**
   * Request the request token and secret.
   * @param callbackURL the URL where the provider should redirect to
   * @return a Response object holding either the result in case of a success or the error
   */
  def retrieveRequestToken(callbackURL: String): Either[RequestToken, OAuthException] = {
    val consumer = new DefaultOAuthConsumer(info.key.key, info.key.secret)
    try {
      provider.retrieveRequestToken(consumer, callbackURL)
      Left(RequestToken(consumer.getToken(), consumer.getTokenSecret()))
    } catch {
      case e: OAuthException => Right(e)
    }
  }

  /**
   * Exchange a request token for an access token.
   * @param the token/secret pair obtained from a previous call
   * @param verifier a string you got through your user, with redirection
   * @return a Response object holding either the result in case of a success or the error
   */
  def retrieveAccessToken(token: RequestToken, verifier: String): Either[RequestToken, OAuthException] = {
    val consumer = new DefaultOAuthConsumer(info.key.key, info.key.secret)
    consumer.setTokenWithSecret(token.token, token.secret)
    try {
      provider.retrieveAccessToken(consumer, verifier)
      Left(RequestToken(consumer.getToken(), consumer.getTokenSecret()))
    } catch {
      case e: OAuthException => Right(e)
    }
  }

  /**
   * The URL where the user needs to be redirected to grant authorization to your application
   */
  def redirectUrl(token: String): String =
    _root_.oauth.signpost.OAuth.addQueryParameters(provider.getAuthorizationWebsiteUrl(),
      _root_.oauth.signpost.OAuth.OAUTH_TOKEN, token);
}

package oauth {

  case class ConsumerKey(key: String, secret: String)
  case class RequestToken(token: String, secret: String)
  case class ServiceInfo(requestTokenURL: String, accessTokenURL: String, authorizationURL: String, key: ConsumerKey)

}

