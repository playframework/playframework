# Session and Flash scopes

## How it is different in Play

If you have to keep data across multiple HTTP requests, you can save them in the Session or the Flash scope. Data stored in the Session are available during the whole user session, and data stored in the flash scope are only available to the next request.

It’s important to understand that Session and Flash data are not stored in the server but are added to each subsequent HTTP Request, using Cookies. This means that the data size is very limited (up to 4 KB) and that you can only store string values.

Cookies are signed with a secret key so the client can’t modify the cookie data (or it will be invalidated). The Play session is not intended to be used as a cache. If you need to cache some data related to a specific session, you can use the Play built-in cache mechanism and use store a unique ID in the user session to associate the cached data with a specific user.

> There is no technical timeout for the session, which expires when the user closes the web browser. If you need a functional timeout for a specific application, just store a timestamp into the user Session and use it however your application needs (e.g. for a maximum session duration, maxmimum inactivity duration, etc.).

## Reading a Session value

You can retrieve the incoming Session from the HTTP request:

```
public static Result index() {
  String user = session("connected");
  if(user != null) {
    return ok("Hello " + user);
  } else {
    return unauthorized("Oops, you are not connected");
  }
}
```

## Storing data into the Session

As the Session is just a Cookie, it is also just an HTTP header, but Play provides a helper method to store a session value:

```
public static Result index() {
  session("connected", "user@gmail.com");
  return ok("Welcome!");
}
```

The same way, you can remove any value from the incoming session:

```
public static Result index() {
  session.remove("connected");
  return ok("Bye");
}
```

## Discarding the whole session

If you want to discard the whole session, there is special operation:

```
public static Result index() {
  session().clear();
  return ok("Bye");
}
```

## Flash scope

The Flash scope works exactly like the Session, but with two differences:

- data are kept for only one request
- the Flash cookie is not signed, making it possible for the user to modify it.

> **Important:** The flash scope should only be used to transport success/error messages on simple non-Ajax applications. As the data are just kept for the next request and because there are no guarantees to ensure the request order in a complex Web application, the Flash scope is subject to race conditions.

Here are a few examples using the Flash scope:

```
public static Result index() {
  String message = flash("success");
  if(message == null) {
    message = "Welcome!";
  }
  return ok(message);
}

public static Result save() {
  flash("success", "The item has been created");
  return redirect("/home");
}
```

> **Next:** [[Body parsers | JavaBodyParsers]]