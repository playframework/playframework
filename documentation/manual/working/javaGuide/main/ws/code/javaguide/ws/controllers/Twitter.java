/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws.controllers;

// #ws-oauth-controller
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.libs.oauth.OAuth;
import play.libs.oauth.OAuth.ConsumerKey;
import play.libs.oauth.OAuth.OAuthCalculator;
import play.libs.oauth.OAuth.RequestToken;
import play.libs.oauth.OAuth.ServiceInfo;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class Twitter extends Controller {
  static final ConsumerKey KEY = new ConsumerKey("...", "...");

  private static final ServiceInfo SERVICE_INFO =
      new ServiceInfo(
          "https://api.twitter.com/oauth/request_token",
          "https://api.twitter.com/oauth/access_token",
          "https://api.twitter.com/oauth/authorize",
          KEY);

  private static final OAuth TWITTER = new OAuth(SERVICE_INFO);

  private final WSClient ws;

  @Inject
  public Twitter(WSClient ws) {
    this.ws = ws;
  }

  public CompletionStage<Result> homeTimeline(Http.Request request) {
    Optional<RequestToken> sessionTokenPair = getSessionTokenPair(request);
    if (sessionTokenPair.isPresent()) {
      return ws.url("https://api.twitter.com/1.1/statuses/home_timeline.json")
          .sign(new OAuthCalculator(Twitter.KEY, sessionTokenPair.get()))
          .get()
          .thenApply(result -> ok(result.asJson()));
    }
    return CompletableFuture.completedFuture(redirect(routes.Twitter.auth()));
  }

  public Result auth(Http.Request request) {
    Optional<String> verifier = request.queryString("oauth_verifier");
    Result result =
        verifier
            .filter(s -> !s.isEmpty())
            .map(
                s -> {
                  RequestToken requestToken = getSessionTokenPair(request).get();
                  RequestToken accessToken = TWITTER.retrieveAccessToken(requestToken, s);
                  return redirect(routes.Twitter.homeTimeline())
                      .addingToSession(request, "token", accessToken.token)
                      .addingToSession(request, "secret", accessToken.secret);
                })
            .orElseGet(
                () -> {
                  String url = routes.Twitter.auth().absoluteURL(request);
                  RequestToken requestToken = TWITTER.retrieveRequestToken(url);
                  return redirect(TWITTER.redirectUrl(requestToken.token))
                      .addingToSession(request, "token", requestToken.token)
                      .addingToSession(request, "secret", requestToken.secret);
                });

    return result;
  }

  private Optional<RequestToken> getSessionTokenPair(Http.Request request) {
    return request
        .session()
        .get("token")
        .map(token -> new RequestToken(token, request.session().get("secret").get()));
  }
}
// #ws-oauth-controller
