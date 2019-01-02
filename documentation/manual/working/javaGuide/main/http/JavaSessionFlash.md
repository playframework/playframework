<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Session and Flash scopes

## How it is different in Play

If you have to keep data across multiple HTTP requests, you can save them in the Session or the Flash scope. Data stored in the Session are available during the whole user session, and data stored in the flash scope are only available to the next request.

It’s important to understand that Session and Flash data are not stored in the server but are added to each subsequent HTTP Request, using Cookies. This means that the data size is very limited (up to 4 KB) and that you can only store string values.

Cookies are signed with a secret key so the client can’t modify the cookie data (or it will be invalidated). The Play session is not intended to be used as a cache. If you need to cache some data related to a specific session, you can use the Play built-in cache mechanism and use the session to store a unique ID to associate the cached data with a specific user.

## Session Configuration

Please see [[Configuring Session Cookies|SettingsSession]] for more information for how to configure the session cookie parameters in `application.conf`.

### Session Timeout / Expiration

By default, there is no technical timeout for the Session. It expires when the user closes the web browser. If you need a functional timeout for a specific application, you set the maximum age of the session cookie by configuring the key `play.http.session.maxAge` in `application.conf`, and this will also set `play.http.session.jwt.expiresAfter` to the same value.  The `maxAge` property will remove the cookie from the browser, and the JWT `exp` claim will be set in the cookie, and will make it invalid after the given duration.  Please see [[Configuring Session Cookies|SettingsSession]] for more information.

## Storing data into the Session

As the Session is just a Cookie, it is also just an HTTP header, but Play provides a helper method to store a session value:

@[store-session](code/javaguide/http/JavaSessionFlash.java)

The same way, you can remove any value from the incoming session:

@[remove-from-session](code/javaguide/http/JavaSessionFlash.java)

## Reading a Session value

You can retrieve the incoming Session from the HTTP request:

@[read-session](code/javaguide/http/JavaSessionFlash.java)

## Discarding the whole session

If you want to discard the whole session, there is special operation:

@[discard-whole-session](code/javaguide/http/JavaSessionFlash.java)

## Flash scope

The Flash scope works exactly like the Session, but with two differences:

- data are kept for only one request
- the Flash cookie is not signed, making it possible for the user to modify it.

> **Important:** The flash scope should only be used to transport success/error messages on simple non-Ajax applications. As the data are just kept for the next request and because there are no guarantees to ensure the request order in a complex Web application, the Flash scope is subject to race conditions.

So for example, after saving an item, you might want to redirect the user back to the index page, and you might want to display a message on the index page saying that the save was successful.  In the save action, you would add the success message to the flash scope:

@[store-flash](code/javaguide/http/JavaSessionFlash.java)

Then in the index action, you could check if the success message exists in the flash scope, and if so, render it:

@[read-flash](code/javaguide/http/JavaSessionFlash.java)

A flash value is also automatically available in Twirl templates. For example:

@[flash-template](code/javaguide/http/views/index.scala.html)
