/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.openid;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import play.mvc.Http;

/** A client for performing OpenID authentication. */
public interface OpenIdClient {

  /**
   * Retrieve the URL where the user should be redirected to start the OpenID authentication
   * process.
   *
   * @param openID the open ID
   * @param callbackURL the callback url.
   * @return A completion stage of the URL as a string.
   */
  CompletionStage<String> redirectURL(String openID, String callbackURL);

  /**
   * Retrieve the URL where the user should be redirected to start the OpenID authentication process
   *
   * @param openID the open ID
   * @param callbackURL the callback url.
   * @param axRequired the required ax
   * @return A completion stage of the URL as a string.
   */
  CompletionStage<String> redirectURL(
      String openID, String callbackURL, Map<String, String> axRequired);

  /**
   * Retrieve the URL where the user should be redirected to start the OpenID authentication
   * process.
   *
   * @param openID the open ID
   * @param callbackURL the callback url.
   * @param axRequired the required ax
   * @param axOptional the optional ax
   * @return A completion stage of the URL as a string.
   */
  CompletionStage<String> redirectURL(
      String openID,
      String callbackURL,
      Map<String, String> axRequired,
      Map<String, String> axOptional);

  /**
   * Retrieve the URL where the user should be redirected to start the OpenID authentication
   * process.
   *
   * @param openID the open ID
   * @param callbackURL the callback url.
   * @param axRequired the required ax
   * @param axOptional the optional ax
   * @param realm the HTTP realm
   * @return A completion stage of the URL as a string.
   */
  CompletionStage<String> redirectURL(
      String openID,
      String callbackURL,
      Map<String, String> axRequired,
      Map<String, String> axOptional,
      String realm);

  /**
   * Check the identity of the user from the current request, that should be the callback from the
   * OpenID server
   *
   * @param request the request header
   * @return A completion stage of the user's identity.
   */
  CompletionStage<UserInfo> verifiedId(Http.RequestHeader request);
}
