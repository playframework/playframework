<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# Session and Flash scopes

## How it is different in Play

If you have to keep data across multiple HTTP requests, you can save them in the Session or the Flash scope. Data stored in the Session are available during the whole user session, and data stored in the flash scope are only available to the next request.

## Working with Cookies

It’s important to understand that Session and Flash data are not stored in the server but are added to each subsequent HTTP Request, using HTTP cookies.  

Because Session and Flash are implemented using cookies, there are some important implications.

* The data size is very limited (up to 4 KB).
* You can only store string values, although you can serialize JSON to the cookie.
* Information in a cookie is visible to the browser, and so can expose sensitive data.
* Cookie information is immutable to the original request, and only available to subsequent requests.

The last point can be a source of confusion.  When you modify the cookie, you are providing information to the response, and Play must parse it again to see the updated value.  If you would like to ensure the session information is current then you should always pair modification of a session with a Redirect.

## Session Configuration

The default name for the cookie is `PLAY_SESSION`. This can be changed by configuring the key `play.http.session.cookieName` in application.conf.

If the name of the cookie is changed, the earlier cookie can be discarded using the same methods mentioned in [[Setting and discarding cookies|ScalaResults]].

Please see [[Configuring Session Cookies|SettingsSession]] for more information for how to configure the session cookie parameters in `application.conf`.

### Session Timeout / Expiration

By default, there is no technical timeout for the Session. It expires when the user closes the web browser. If you need a functional timeout for a specific application, you set the maximum age of the session cookie by configuring the key `play.http.session.maxAge` in `application.conf`, and this will also set `play.http.session.jwt.expiresAfter` to the same value.  The `maxAge` property will remove the cookie from the browser, and the JWT `exp` claim will be set in the cookie, and will make it invalid after the given duration.  Please see [[Configuring Session Cookies|SettingsSession]] for more information.

## Storing data in the Session

As the Session is just a Cookie, it is also just an HTTP header. You can manipulate the session data the same way you manipulate other results properties:

@[store-session](code/ScalaSessionFlash.scala)

Note that this will replace the whole session. If you need to add an element to an existing Session, just add an element to the incoming session, and specify that as new session:

@[add-session](code/ScalaSessionFlash.scala)

You can remove any value from the incoming session the same way:

@[remove-session](code/ScalaSessionFlash.scala)

## Reading a Session value

You can retrieve the incoming Session from the HTTP request:

@[index-retrieve-incoming-session](code/ScalaSessionFlash.scala)

## Discarding the whole session

There is special operation that discards the whole session:

@[discarding-session](code/ScalaSessionFlash.scala)

## Flash scope

The Flash scope works exactly like the Session, but with two differences:

* data are kept for only one request
* the Flash cookie is not signed, making it possible for the user to modify it.

> **Important:** The Flash scope should only be used to transport success/error messages on simple non-Ajax applications. As the data are just kept for the next request and because there are no guarantees to ensure the request order in a complex Web application, the Flash scope is subject to race conditions.

Here are a few examples using the Flash scope:

@[using-flash](code/ScalaSessionFlash.scala)

To retrieve the Flash scope value in your view, add an implicit Flash parameter:

@[flash-template](code/scalaguide/http/scalasessionflash/views/index.scala.html)

And in your Action, specify an `implicit request =>` as shown below:

@[flash-implicit-request](code/ScalaSessionFlash.scala)

An implicit Flash will be provided to the view based on the implicit request.

If the error '_could not find implicit value for parameter flash: play.api.mvc.Flash_' is raised then this is because your Action didn't have an implicit request in scope.
