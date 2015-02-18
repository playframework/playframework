/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.openid;

import play.core.Invoker;
import play.libs.F;
import play.libs.Scala;
import play.mvc.Http;
import scala.collection.JavaConversions;
import scala.runtime.AbstractFunction1;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class DefaultOpenIdClient implements OpenIdClient {

    private final play.api.libs.openid.OpenIdClient client;

    @Inject
    public DefaultOpenIdClient(play.api.libs.openid.OpenIdClient client) {
        this.client = client;
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    @Override
    public F.Promise<String> redirectURL(String openID, String callbackURL) {
        return redirectURL(openID, callbackURL, null, null, null);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    @Override
    public F.Promise<String> redirectURL(String openID, String callbackURL, Map<String, String> axRequired) {
        return redirectURL(openID, callbackURL, axRequired, null, null);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    @Override
    public F.Promise<String> redirectURL(String openID,
                                         String callbackURL,
                                         Map<String, String> axRequired,
                                         Map<String, String> axOptional) {
        return redirectURL(openID, callbackURL, axRequired, axOptional, null);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    @Override
    public F.Promise<String> redirectURL(String openID,
                                         String callbackURL,
                                         Map<String, String> axRequired,
                                         Map<String, String> axOptional,
                                         String realm) {
        if (axRequired == null) axRequired = new HashMap<String, String>();
        if (axOptional == null) axOptional = new HashMap<String, String>();
        return F.Promise.wrap(client.redirectURL(openID,
                callbackURL,
                JavaConversions.mapAsScalaMap(axRequired).toSeq(),
                JavaConversions.mapAsScalaMap(axOptional).toSeq(),
                Scala.Option(realm)));
    }

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    @Override
    public F.Promise<UserInfo> verifiedId(Http.RequestHeader request) {
        scala.concurrent.Future<UserInfo> scalaPromise = client.verifiedId(request.queryString()).map(
                new AbstractFunction1<play.api.libs.openid.UserInfo, UserInfo>() {
                    @Override
                    public UserInfo apply(play.api.libs.openid.UserInfo scalaUserInfo) {
                        return new UserInfo(scalaUserInfo.id(), JavaConversions.mapAsJavaMap(scalaUserInfo.attributes()));
                    }
                }, Invoker.executionContext());
        return F.Promise.wrap(scalaPromise);
    }

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    @Override
    public F.Promise<UserInfo> verifiedId() {
        return verifiedId(Http.Context.current().request());
    }
}
