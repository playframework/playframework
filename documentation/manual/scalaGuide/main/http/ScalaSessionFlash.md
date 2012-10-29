# Session and Flash scopes

## How it is different in Play

If you have to keep data across multiple HTTP requests, you can save them in the Session or Flash scopes. Data stored in the Session are available during the whole user Session, and data stored in the Flash scope are available to the next request only.

It’s important to understand that Session and Flash data are not stored by the server but are added to each subsequent HTTP request, using the cookie mechanism. This means that the data size is very limited (up to 4 KB) and that you can only store string values.

Of course, cookie values are signed with a secret key so the client can’t modify the cookie data (or it will be invalidated).

The Play Session is not intended to be used as a cache. If you need to cache some data related to a specific Session, you can use the Play built-in cache mechanism and use store a unique ID in the user Session to keep them related to a specific user.

> There is no technical timeout for the Session. It expires when the user closes the web browser. If you need a functional timeout for a specific application, just store a timestamp into the user Session and use it however your application needs (e.g. for a maximum session duration, maximum inactivity duration, etc.).

## Reading a Session value

You can retrieve the incoming Session from the HTTP request:

```scala
def index = Action { request =>
  request.session.get("connected").map { user =>
    Ok("Hello " + user)
  }.getOrElse {
    Unauthorized("Oops, you are not connected")
  }
}
```

Alternatively you can retrieve the Session implicitly from a request:

```scala
def index = Action { implicit request =>
  session.get("connected").map { user =>
    Ok("Hello " + user)
  }.getOrElse {
    Unauthorized("Oops, you are not connected")
  }
}
```

## Storing data in the Session

As the Session is just a Cookie, it is also just an HTTP header. You can manipulate the session data the same way you manipulate other results properties:

```scala
Ok("Welcome!").withSession(
  "connected" -> "user@gmail.com"
)
```

Note that this will replace the whole session. If you need to add an element to an existing Session, just add an element to the incoming session, and specify that as new session:

```scala
Ok("Hello World!").withSession(
  session + ("saidHello" -> "yes")
)
```

You can remove any value from the incoming session the same way:

```scala
Ok("Theme reset!").withSession(
  session - "theme"
)
```

## Discarding the whole session

There is special operation that discards the whole session:

```scala
Ok("Bye").withNewSession
```

## Flash scope

The Flash scope works exactly like the Session, but with two differences:

- data are kept for only one request
- the Flash cookie is not signed, making it possible for the user to modify it.

> **Important:** The Flash scope should only be used to transport success/error messages on simple non-Ajax applications. As the data are just kept for the next request and because there are no guarantees to ensure the request order in a complex Web application, the Flash scope is subject to race conditions.

Here are a few examples using the Flash scope:

```scala
def index = Action { implicit request =>
  Ok {
    flash.get("success").getOrElse("Welcome!")
  }
}

def save = Action {
  Redirect("/home").flashing(
    "success" -> "The item has been created"
  )
}
```


To retrieve the Flash scope value in your view, just add an implicit with Flash:
```
@()(implicit flash: Flash)
...
@flash.get("success").getOrElse("Welcome!")
...
```

If the error '_could not find implicit value for parameter flash: play.api.mvc.Flash_' is raised then this is because your Action didn't import a request object. Add an "implicit request=>" as show below:

```scala
def index() = Action {   
  implicit request =>
    Ok(views.html.Application.index())
}
```

> **Next:** [[Body parsers | ScalaBodyParsers]]