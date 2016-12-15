/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.openid;

import play.libs.Scala;
import play.mvc.Http;
import scala.collection.JavaConverters;
import scala.compat.java8.FutureConverters;
import scala.concurrent.ExecutionContext;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class DefaultOpenIdClient implements OpenIdClient {

    private final play.api.libs.openid.OpenIdClient client;
    private final ExecutionContext executionContext;

    @Inject
    public DefaultOpenIdClient(play.api.libs.openid.OpenIdClient client, ExecutionContext executionContext) {
        this.client = client;
        this.executionContext = executionContext;
    }

    @Override
    public CompletionStage<String> redirectURL(String openID, String callbackURL) {
        return redirectURL(openID, callbackURL, null, null, null);
    }

    @Override
    public CompletionStage<String> redirectURL(String openID, String callbackURL, Map<String, String> axRequired) {
        return redirectURL(openID, callbackURL, axRequired, null, null);
    }

    @Override
    public CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional) {
        return redirectURL(openID, callbackURL, axRequired, axOptional, null);
    }

    @Override
    public CompletionStage<String> redirectURL(
            String openID, String callbackURL, Map<String, String> axRequired, Map<String, String> axOptional, String realm) {
        if (axRequired == null) axRequired = new HashMap<>();
        if (axOptional == null) axOptional = new HashMap<>();
        return FutureConverters.toJava(client.redirectURL(openID,
                callbackURL,
                JavaConverters.mapAsScalaMap(axRequired).toSeq(),
                JavaConverters.mapAsScalaMap(axOptional).toSeq(),
                Scala.Option(realm)));
    }

    @Override
    public CompletionStage<UserInfo> verifiedId(Http.RequestHeader request) {
        return FutureConverters
                .toJava(client.verifiedId(request.queryString()))
                .thenApply(userInfo -> new UserInfo(userInfo.id(), JavaConverters.mapAsJavaMap(userInfo.attributes())));
    }

    @Override
    public CompletionStage<UserInfo> verifiedId() {
        return verifiedId(Http.Context.current().request());
    }
}
