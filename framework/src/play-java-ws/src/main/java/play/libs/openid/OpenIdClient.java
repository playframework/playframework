/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.openid;

import play.mvc.Http;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * A client for performing OpenID authentication.
 */
public interface OpenIdClient {

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    CompletionStage<String> redirectURL(String openID, String callbackURL);

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    CompletionStage<String> redirectURL(String openID, String callbackURL, Map<String, String> axRequired);

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional);

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional, String realm);

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    CompletionStage<UserInfo> verifiedId(Http.RequestHeader request);

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    CompletionStage<UserInfo> verifiedId();
}
