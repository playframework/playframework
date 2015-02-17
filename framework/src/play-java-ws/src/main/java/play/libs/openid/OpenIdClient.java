/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.openid;

import play.libs.F;
import play.mvc.Http;

import java.util.Map;

/**
 * Created by jroper on 29/08/14.
 */
public interface OpenIdClient {

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    F.Promise<String> redirectURL(String openID, String callbackURL);

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    F.Promise<String> redirectURL(String openID, String callbackURL, Map<String, String> axRequired);

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    F.Promise<String> redirectURL(String openID,
                                  String callbackURL,
                                  Map<String, String> axRequired,
                                  Map<String, String> axOptional);

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    F.Promise<String> redirectURL(String openID,
                                  String callbackURL,
                                  Map<String, String> axRequired,
                                  Map<String, String> axOptional,
                                  String realm);

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    F.Promise<UserInfo> verifiedId(Http.RequestHeader request);

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    F.Promise<UserInfo> verifiedId();
}
