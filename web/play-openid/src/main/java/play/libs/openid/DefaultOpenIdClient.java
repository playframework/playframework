/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.openid;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import play.libs.Scala;
import play.mvc.Http;
import scala.concurrent.ExecutionContext;
import scala.jdk.javaapi.FutureConverters;
import scala.runtime.AbstractFunction1;

public class DefaultOpenIdClient implements OpenIdClient {

  private final play.api.libs.openid.OpenIdClient client;
  private final ExecutionContext executionContext;

  @Inject
  public DefaultOpenIdClient(
      play.api.libs.openid.OpenIdClient client, ExecutionContext executionContext) {
    this.client = client;
    this.executionContext = executionContext;
  }

  @Override
  public CompletionStage<String> redirectURL(String openID, String callbackURL) {
    return redirectURL(openID, callbackURL, null, null, null);
  }

  @Override
  public CompletionStage<String> redirectURL(
      String openID, String callbackURL, Map<String, String> axRequired) {
    return redirectURL(openID, callbackURL, axRequired, null, null);
  }

  @Override
  public CompletionStage<String> redirectURL(
      String openID,
      String callbackURL,
      Map<String, String> axRequired,
      Map<String, String> axOptional) {
    return redirectURL(openID, callbackURL, axRequired, axOptional, null);
  }

  @Override
  public CompletionStage<String> redirectURL(
      String openID,
      String callbackURL,
      Map<String, String> axRequired,
      Map<String, String> axOptional,
      String realm) {
    if (axRequired == null) axRequired = new HashMap<>();
    if (axOptional == null) axOptional = new HashMap<>();
    return FutureConverters.asJava(
        client.redirectURL(
            openID,
            callbackURL,
            Scala.asScala(axRequired).toSeq(),
            Scala.asScala(axOptional).toSeq(),
            Scala.Option(realm)));
  }

  @Override
  public CompletionStage<UserInfo> verifiedId(Http.RequestHeader request) {
    scala.concurrent.Future<UserInfo> scalaPromise =
        client
            .verifiedId(request.queryString())
            .map(
                new AbstractFunction1<play.api.libs.openid.UserInfo, UserInfo>() {
                  @Override
                  public UserInfo apply(play.api.libs.openid.UserInfo scalaUserInfo) {
                    return new UserInfo(
                        scalaUserInfo.id(), Scala.asJava(scalaUserInfo.attributes()));
                  }
                },
                executionContext);
    return FutureConverters.asJava(scalaPromise);
  }
}
