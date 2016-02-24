/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.openid;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * provides support for OpenID
 */
public class OpenID {

    private static OpenIdClient client() {
        return play.api.Play.current().injector().instanceOf(OpenIdClient.class);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     *
     * @deprecated Inject an OpenIdClient into your component.
     */
    @Deprecated
    public static CompletionStage<String> redirectURL(String openID, String callbackURL) {
        return client().redirectURL(openID, callbackURL);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     *
     * @deprecated Inject an OpenIdClient into your component.
     */
    @Deprecated
    public static CompletionStage<String> redirectURL(String openID, String callbackURL, Map<String, String> axRequired) {
        return client().redirectURL(openID, callbackURL, axRequired);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     *
     * @deprecated Inject an OpenIdClient into your component.
     */
    @Deprecated
    public static CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional) {
        return client().redirectURL(openID, callbackURL, axRequired, axOptional);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     *
     * @deprecated Inject an OpenIdClient into your component.
     */
    @Deprecated
    public static CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional, String realm) {
        return client().redirectURL(openID, callbackURL, axRequired, axOptional, realm);
    }

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     *
     * @deprecated Inject an OpenIdClient into your component.
     */
    @Deprecated
    public static CompletionStage<UserInfo> verifiedId() {
        return client().verifiedId();
    }

}
