<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# The Play cache API

Caching data is a typical optimization in modern applications, and so Play provides a global cache. An important point about the cache is that it behaves just like a cache should: the data you just stored may just go missing.

For any data stored in the cache, a regeneration strategy needs to be put in place in case the data goes missing. This philosophy is one of the fundamentals behind Play, and is different from Java EE, where the session is expected to retain values throughout its lifetime.

The default implementation of the cache API uses [Ehcache](http://www.ehcache.org/).

## Importing the Cache API

Play provides both an API and an default Ehcache implementation of that API. To get the full Ehcache implementation, add `ehcache` to your dependencies list:

@[ehcache-sbt-dependencies](code/cache.sbt)

This will also automatically set up the bindings for runtime DI so the components are injectable.

To add only the API, add `cacheApi` to your dependencies list.

@[cache-sbt-dependencies](code/cache.sbt)

The API dependency is useful if you'd like to define your own bindings for the `Cached` helper and `AsyncCacheApi`, etc., without having to depend on Ehcache. If you're writing a custom cache module you should use this.

## JCache Support

Ehcache implements the [JSR 107](https://github.com/jsr107/jsr107spec) specification, also known as JCache, but Play does not bind `javax.caching.CacheManager` by default.  To bind `javax.caching.CacheManager` to the default provider, add the following to your dependencies list:

@[jcache-sbt-dependencies](code/cache.sbt)

If you are using Guice, you can add the following for Java annotations:

@[jcache-guice-annotation-sbt-dependencies](code/cache.sbt)

## Accessing the Cache API

The cache API is defined by the [AsyncCacheApi](api/java/play/cache/AsyncCacheApi.html) and [SyncCacheApi](api/java/play/cache/SyncCacheApi.html) interfaces, depending on whether you want an asynchronous or synchronous implementation, and can be injected into your component like any other dependency.  For example:

@[inject](code/javaguide/cache/inject/Application.java)

> **Note:** The API is intentionally minimal to allow various implementations to be plugged in. If you need a more specific API, use the one provided by your Cache library.

Using this simple API you can store data in the cache:

@[simple-set](code/javaguide/cache/JavaCache.java)

Optionally you can specify an expiration (in seconds) for the cache:

@[time-set](code/javaguide/cache/JavaCache.java)

You can retrieve the data later:

@[get](code/javaguide/cache/JavaCache.java)

You can also supply a `Callable` that generates stores the value if no value is found in the cache:

@[get-or-else](code/javaguide/cache/JavaCache.java)

**Note**: `getOrElseUpdate` is not an atomic operation in Ehcache and is implemented as a `get` followed by computing the value from the `Callable`, then a `set`. This means it's possible for the value to be computed multiple times if multiple threads are calling `getOrElse` simultaneously.

To remove an item from the cache use the `remove` method:

@[remove](code/javaguide/cache/JavaCache.java)

To remove all items from the cache use the `removeAll` method:

@[removeAll](code/javaguide/cache/JavaCache.java)

`removeAll()` is only available on `AsyncCacheApi`, since removing all elements of the cache is rarely something you want to do sychronously. The expectation is that removing all items from the cache should only be needed as an admin operation in special cases, not part of the normal operation of your app.

Note that the [SyncCacheApi](api/java/play/cache/SyncCacheApi.html) has the same API, except it returns the values directly instead of using futures.

## Accessing different caches

It is possible to access different caches. In the default Ehcache implementation, the default cache is called `play`, and can be configured by creating a file called `ehcache.xml`. Additional caches may be configured with different configurations, or even implementations.

If you want to access multiple different ehcache caches, then you'll need to tell Play to bind them in `application.conf`, like so:

    play.cache.bindCaches = ["db-cache", "user-cache", "session-cache"]

By default, Play will try to create these caches for you. If you would like to define them yourself in `ehcache.xml`, you can set:

    play.cache.createBoundCaches = false

Now to access these different caches, when you inject them, use the [NamedCache](api/java/play/cache/NamedCache.html) qualifier on your dependency, for example:

@[qualified](code/javaguide/cache/qualified/Application.java)

## Setting the execution context

By default, all Ehcache operations are blocking, and async implementations will block threads in the default execution context. Usually this is okay if you are using Play's default configuration, which only stores elements in memory since reads should be relatively fast. However, depending on how EhCache was configured and [where the data is stored](https://www.ehcache.org/generated/2.10.4/html/ehc-all/#page/Ehcache_Documentation_Set%2Fco-store_storage_tiers.html), this blocking I/O might be too costly.
For such a case you can configure a different [Akka dispatcher](https://doc.akka.io/docs/akka/current/dispatchers.html?language=scala#looking-up-a-dispatcher) and set it via `play.cache.dispatcher` so the EhCache plugin makes use of it:

```
play.cache.dispatcher = "contexts.blockingCacheDispatcher"

contexts {
  blockingCacheDispatcher {
    fork-join-executor {
      parallelism-factor = 3.0
    }
  }
}
```

## Caching HTTP responses

You can easily create a smart cached action using standard `Action` composition.

> **Tip:** Play HTTP `Result` instances are safe to cache and reuse later.

Play provides a default built-in helper for the standard case:

@[http](code/javaguide/cache/JavaCache.java)

## Custom implementations

It is possible to provide a custom implementation of the cache API that either replaces or sits alongside the default implementation.

To replace the default implementation based on something other than Ehcache, you only need the `cacheApi` dependency rather than the `ehcache` dependency in your `build.sbt`. If you still need access to the Ehcache implementation classes, you can use `ehcache` and disable the module from automatically binding it in `application.conf`:

```
play.modules.disabled += "play.api.cache.ehcache.EhCacheModule"
```

You can then implement [AsyncCacheApi](api/java/play/cache/AsyncCacheApi.html) and bind it in the DI container. You can also bind [SyncCacheApi](api/java/play/cache/SyncCacheApi.html) to [DefaultSyncCacheApi](api/java/play/cache/DefaultSyncCacheApi.html), which simply wraps the async implementation.

Note that the `removeAll` method may not be supported by your cache implementation, either because it is not possible or because it would be unnecessarily inefficient. If that is the case, you can throw an `UnsupportedOperationException` in the `removeAll` method.

To provide an implementation of the cache API in addition to the default implementation, you can either create a custom qualifier, or reuse the `NamedCache` qualifier to bind the implementation.
