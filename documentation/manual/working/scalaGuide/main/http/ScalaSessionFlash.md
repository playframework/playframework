<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Session and Flash scopes

## How it is different in Play

If you have to keep data across multiple HTTP requests, you can save them in the Session or Flash scopes. Data stored in the Session are available during the whole user Session, and data stored in the Flash scope are available to the next request only.

It’s important to understand that Session and Flash data are not stored by the server but are added to each subsequent HTTP request, using the cookie mechanism. This means that the data size is very limited (up to 4 KB) and that you can only store string values. The default name for the cookie is `PLAY_SESSION`. This can be changed by configuring the key `session.cookieName` in application.conf.

> If the name of the cookie is changed, the earlier cookie can be discarded using the same methods mentioned in [[Setting and discarding cookies|ScalaResults]].

Of course, cookie values are signed with a secret key so the client can’t modify the cookie data (or it will be invalidated).

The Play Session is not intended to be used as a cache. If you need to cache some data related to a specific Session, you can use the Play built-in cache mechanism and store a unique ID in the user Session to keep them related to a specific user.

> By default, there is no technical timeout for the Session. It expires when the user closes the web browser. If you need a functional timeout for a specific application, just store a timestamp into the user Session and use it however your application needs (e.g. for a maximum session duration, maximum inactivity duration, etc.). You can also set the maximum age of the session cookie by configuring the key `session.maxAge` (in milliseconds) in application.conf.

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

- data are kept for only one request
- the Flash cookie is not signed, making it possible for the user to modify it.

> **Important:** The Flash scope should only be used to transport success/error messages on simple non-Ajax applications. As the data are just kept for the next request and because there are no guarantees to ensure the request order in a complex Web application, the Flash scope is subject to race conditions.

Here are a few examples using the Flash scope:

@[using-flash](code/ScalaSessionFlash.scala)

To retrieve the Flash scope value in your view, add an implicit Flash parameter:

@[flash-template](code/scalaguide/http/scalasessionflash/views/index.scala.html)

And in your Action, specify an `implicit request =>` as shown below:

@[flash-implicit-request](code/ScalaSessionFlash.scala)

An implicit Flash will be provided to the view based on the implicit request.

If the error '_could not find implicit value for parameter flash: play.api.mvc.Flash_' is raised then this is because your Action didn't have an implicit request in scope.
