# OAuth

[OAuth](http://oauth.net/) is a simple way to publish and interact with protected data. It's also a safer and more secure way for people to give you access. For example, it can be used to access your users' data on [Twitter](https://dev.twitter.com/docs/auth/using-oauth).

There are 2 very different versions of OAuth: [OAuth 1.0](http://tools.ietf.org/html/rfc5849) and [OAuth 2.0](http://oauth.net/2/). Version 2 is simple enough to be implemented easily without library or helpers, so Play only provides support for OAuth 1.0.

## Required Information

OAuth requires you to register your application to the service provider. Make sure to check the callback URL that you provide, because the service provider may reject your calls if they don't match. When working locally, you can use `/etc/hosts` to fake a domain on your local machine.

The service provider will give you:

* Application ID
* Secret key
* Request Token URL
* Access Token URL
* Authorize URL

## Authentication Flow

Most of the flow will be done by the Play library.

1. Get a request token from the server (in a server-to-server call)
2. Redirect the user to the service provider, where he will grant your application rights to use his data
3. The service provider will redirect the user back, giving you a /verifier/
4. With that verifier, exchange the /request token/ for an /access token/ (server-to-server call)

Now the /access token/ can be passed to any call to access protected data.

## Example

```java
public class Twitter extends Controller {

  static final ConsumerKey KEY = new ConsumerKey("...", "...");

  private static final ServiceInfo SERVICE_INFO = new ServiceInfo("https://api.twitter.com/oauth/request_token",
                                                                  "https://api.twitter.com/oauth/access_token",
                                                                  "https://api.twitter.com/oauth/authorize", 
                                                                  KEY);
  
  private static final OAuth TWITTER = new OAuth(SERVICE_INFO);
  
  public static Result auth() {
    String verifier = request().getQueryString("oauth_verifier");
    if (Strings.isNullOrEmpty(verifier)) {
      String url = routes.Twitter.auth().absoluteURL(request());
      RequestToken requestToken = TWITTER.retrieveRequestToken(url);
      saveSessionTokenPair(requestToken);
      return redirect(TWITTER.redirectUrl(requestToken.token));
    } else {
      RequestToken requestToken = getSessionTokenPair().get();
      RequestToken accessToken = TWITTER.retrieveAccessToken(requestToken, verifier);
      saveSessionTokenPair(accessToken);
      return redirect(routes.Application.index());
    }
  }

  private static void saveSessionTokenPair(RequestToken requestToken) {
    session("token", requestToken.token);
    session("secret", requestToken.secret);
  }

  static Option<RequestToken> getSessionTokenPair() {
    if (session().containsKey("token")) {
      return Option.Some(new RequestToken(session("token"), session("secret")));
    }
    return Option.None();
  }
}
```

```java
public class Application extends Controller {
  
  public static Result index() {
    Option<RequestToken> sessionTokenPair = Twitter.getSessionTokenPair();
    if (sessionTokenPair.isDefined()) {
      return async(WS.url("https://api.twitter.com/1.1/statuses/home_timeline.json")
          .sign(new OAuthCalculator(Twitter.KEY, sessionTokenPair.get()))
          .get()
          .map(new Function<Response, Result>(){
            @Override
            public Result apply(Response result) throws Throwable {
              return ok(result.asJson());
            }
       }));
    }
    return redirect(routes.Twitter.auth());
  }
}
```

> **Next:** [[Integrating with Akka| JavaAkka]]
