<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring the session cookie

Play stores the session using a session cookie in the browser.  When you are programming, you will typically access the session through the [[Scala API|ScalaSessionFlash]] or [[Java API|JavaSessionFlash]], but there are useful configuration settings.

Session and flash cookies are stored in [JSON Web Token](https://tools.ietf.org/html/rfc7519) (JWT) format.  The encoding is transparent to Play, but there some useful properties of JWT which can be leveraged for session cookies, and can be configured through `application.conf`.  Note that JWT is typically used in an HTTP header value, which is not what is active here -- in addition, the JWT is signed using the secret, but is not encrypted by Play.

## Not Before Support

When a session cookie is created, the "issued at" `iat` and "not before" `nbf` claims in JWT will be set to the time of cookie creation, which prevents a cookie from being accepted before the current time.

## Session Timeout / Expiration

By default, there is no technical timeout for the Session. It expires when the user closes the web browser. If you need a functional timeout for a specific application, you set the maximum age of the session cookie by configuring the key `play.http.session.maxAge` in `application.conf`, and this will also set `play.http.session.jwt.expiresAfter` to the same value.  The `maxAge` property will remove the cookie from the browser, and the JWT `exp` claim will be set in the cookie, and will make it invalid after the given duration.

## URL Encoded Cookie Encoding

The session cookie uses the JWT cookie encoding.  If you want, you can revert back to URL encoded cookie encoding by switching to `play.api.mvc.LegacyCookiesModule` in the application.conf file:

```
play.modules.disabled+="play.api.mvc.CookiesModule"
play.modules.enabled+="play.api.mvc.LegacyCookiesModule"
```

## Session Configuration

The default session configuration is as follows:

@[session-configuration](/confs/play/reference.conf)


