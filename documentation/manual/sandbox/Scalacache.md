# The Play cache API

The default implementation of the Cache API uses EHCache. You can also provide your own implementation via a plug-in.

# Accessing the Cache API
The cache API is provided by the play.api.cache.Cache object. It requires a registered cache plug-in.

> **Note:** The API is intentionally minimal to allow several implementation to be plugged. If you need a more specific API, use the one provided by your Cache plugin.

Using this simple API you can either store data in cache:

```
Cache.set("item.key", connectedUser)
```

And then retrieve it later:

```
val maybeUser: Option[User] = Cache.getAs[User]("item.key")
```

There is also a convenient helper to retrieve from cache or set the value in cache if it was missing:

```scala
val user: User = Cache.getOrElse[User]("item.key") {
  User.findById(connectedUser)
}
```

How to remove the key is as follows.

```
// 2.0 final
Cache.set("item.key", null, 0)
// later
Cache.remove("item.key")

```

# Caching HTTP responses

You can easily create smart cached actions using standard Action composition.

> **Note:** Play HTTP Result instances are safe to cache and reuse later.

Play provides a default built-in helper for standard cases:

```scala
def index = Cached("homePage") {
  Action {
    Ok("Hello world")
  }
}
```

Or even:

```scala
def userProfile = Authenticated { user =>
  Cached(req => "profile." + user) {      
    Action { 
      Ok(views.html.profile(User.find(user)))
    }   
  }
}
```

> **Next:** [[Calling WebServices | ScalaWS]]