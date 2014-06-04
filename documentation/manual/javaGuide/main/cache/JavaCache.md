# The Play cache API

Caching data is a typical optimization in modern applications, and so Play provides a global cache. An important point about the cache is that it behaves just like a cache should: the data you just stored may just go missing.

For any data stored in the cache, a regeneration strategy needs to be put in place in case the data goes missing. This philosophy is one of the fundamentals behind Play, and is different from Java EE, where the session is expected to retain values throughout its lifetime. 

The default implementation of the cache API uses [EHCache](http://www.ehcache.org/) and it's enabled by default. You can also provide your own implementation via a plugin. 

## Importing the Cache API

Add `cache` into your dependencies list. For example, in `build.sbt`:

```scala
libraryDependencies ++= Seq(
  cache,
  ...
)
```

## Accessing the Cache API

The cache API is provided by the `play.cache.Cache` object. This requires a cache plugin to be registered.

> **Note:** The API is intentionally minimal to allow various implementations to be plugged in. If you need a more specific API, use the one provided by your Cache plugin.

Using this simple API you can store data in the cache:

@[simple-set](code/javaguide/cache/JavaCache.java)

Optionally you can specify an expiration (in seconds) for the cache:

@[time-set](code/javaguide/cache/JavaCache.java)

You can retrieve the data later:

@[get](code/javaguide/cache/JavaCache.java)

To remove an item from the cache use the `remove` method:

@[remove](code/javaguide/cache/JavaCache.java)

## Caching HTTP responses

You can easily create a smart cached action using standard `Action` composition. 

> **Note:** Play HTTP `Result` instances are safe to cache and reuse later.

Play provides a default built-in helper for the standard case:

@[http](code/javaguide/cache/JavaCache.java)

> **Next:** [[Calling web services | JavaWS]]
