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
   * @param the token/secret pair obtained from a previous call
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
   * The URL where the user needs to be redirected to grant authorization to your application
   * @param token request token
   */
  def redirectUrl(token: String): String =
    _root_.oauth.signpost.OAuth.addQueryParameters(provider.getAuthorizationWebsiteUrl(),
      _root_.oauth.signpost.OAuth.OAUTH_TOKEN, token);
}

package oauth {

  /**
   * A consumer key / consumer secret pair that the OAuth provider gave you, to identify your application
   */
  case class ConsumerKey(key: String, secret: String)

  /**
   * A request token / token secret pair, to be used for a specific user
   */
  case class RequestToken(token: String, secret: String)

  /**
   * The information identifying a oauth provider: URLs and the consumer key / consumer secret pair
   */
  case class ServiceInfo(requestTokenURL: String, accessTokenURL: String, authorizationURL: String, key: ConsumerKey)

}

