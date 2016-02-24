/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.openid;

import play.core.Execution;
import play.libs.Scala;
import play.mvc.Http;
import scala.collection.JavaConversions;
import scala.compat.java8.FutureConverters;
import scala.runtime.AbstractFunction1;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

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
    public CompletionStage<String> redirectURL(String openID, String callbackURL) {
        return redirectURL(openID, callbackURL, null, null, null);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    @Override
    public CompletionStage<String> redirectURL(String openID, String callbackURL, Map<String, String> axRequired) {
        return redirectURL(openID, callbackURL, axRequired, null, null);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    @Override
    public CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional) {
        return redirectURL(openID, callbackURL, axRequired, axOptional, null);
    }

    /**
     * Retrieve the URL where the user should be redirected to start the OpenID authentication process
     */
    @Override
    public CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional, String realm) {
        if (axRequired == null) axRequired = new HashMap<>();
        if (axOptional == null) axOptional = new HashMap<>();
        return FutureConverters.toJava(client.redirectURL(openID,
                callbackURL,
                JavaConversions.mapAsScalaMap(axRequired).toSeq(),
                JavaConversions.mapAsScalaMap(axOptional).toSeq(),
                Scala.Option(realm)));
    }

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    @Override
    public CompletionStage<UserInfo> verifiedId(Http.RequestHeader request) {
        scala.concurrent.Future<UserInfo> scalaPromise = client.verifiedId(request.queryString()).map(
                new AbstractFunction1<play.api.libs.openid.UserInfo, UserInfo>() {
                    @Override
                    public UserInfo apply(play.api.libs.openid.UserInfo scalaUserInfo) {
                        return new UserInfo(scalaUserInfo.id(), JavaConversions.mapAsJavaMap(scalaUserInfo.attributes()));
                    }
                }, Execution.internalContext());
        return FutureConverters.toJava(scalaPromise);
    }

    /**
     * Check the identity of the user from the current request, that should be the callback from the OpenID server
     */
    @Override
    public CompletionStage<UserInfo> verifiedId() {
        return verifiedId(Http.Context.current().request());
    }
}
