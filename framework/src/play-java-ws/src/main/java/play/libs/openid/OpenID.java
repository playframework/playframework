/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.openid;

import java.util.Map;
import java.util.HashMap;

import play.Play;
import play.libs.Scala;
import scala.runtime.AbstractFunction1;
import scala.collection.JavaConversions;

import play.libs.F;
import play.mvc.Http;
import play.mvc.Http.Request;

import play.core.Invoker;

/**
 * provides support for OpenID
 */
public class OpenID {

    private static OpenIdClient client() {
        return Play.application().injector().instanceOf(OpenIdClient.class);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    public static F.Promise<String> redirectURL(String openID, String callbackURL) {
        return client().redirectURL(openID, callbackURL);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    public static F.Promise<String> redirectURL(String openID, String callbackURL, Map<String, String> axRequired) {
        return client().redirectURL(openID, callbackURL, axRequired);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    public static F.Promise<String> redirectURL(String openID,
            String callbackURL,
            Map<String, String> axRequired,
            Map<String, String> axOptional) {
        return client().redirectURL(openID, callbackURL, axRequired, axOptional);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    public static F.Promise<String> redirectURL(String openID,
            String callbackURL,
            Map<String, String> axRequired,
            Map<String, String> axOptional,
            String realm) {
        return client().redirectURL(openID, callbackURL, axRequired, axOptional, realm);
    }

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    public static F.Promise<UserInfo> verifiedId() {
        return client().verifiedId();
    }

}
