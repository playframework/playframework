<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# The Play cache API

Caching data is a typical optimization in modern applications, and so Play provides a global cache.

**Note:** An important point about the cache is that it behaves just like a cache should: the data you just stored may just go missing.

For any data stored in the cache, a regeneration strategy needs to be put in place in case the data goes missing. This philosophy is one of the fundamentals behind Play, and is different from Java EE, where the session is expected to retain values throughout its lifetime.

Play provides a CacheApi implementation based on [Caffeine](https://github.com/ben-manes/caffeine/) and a legacy implementation based on [Ehcache 2.x](http://www.ehcache.org). For in-process caching Caffeine is typically the best choice. If you need distributed caching, there are third-party plugins for [memcached](https://github.com/mumoshu/play2-memcached) and [redis](https://github.com/KarelCemus/play-redis).

## Importing the Cache API

Play provides separate dependencies for the Cache API and for the Caffeine and Ehcache implementations.

### Caffeine

To get the Caffeine implementation, add `caffeine` to your dependencies list:

@[caffeine-sbt-dependencies](code/cache.sbt)

This will also automatically set up the bindings for runtime DI so the components are injectable.

### EhCache

To get the EhCache implementation, add `ehcache` to your dependencies list:

@[ehcache-sbt-dependencies](code/ehcache.sbt)

This will also automatically set up the bindings for runtime DI so the components are injectable.

### Custom Cache Implementation

To add only the API, add `cacheApi` to your dependencies list.

@[cache-sbt-dependencies](code/cache.sbt)

The API dependency is useful if you'd like to define your own bindings for the [`Cached`](api/scala/play/api/cache/Cached.html) helper and [`AsyncCacheApi`](api/scala/play/api/cache/AsyncCacheApi.html), etc., without having to depend on any specific cache implementation. If you're writing a custom cache module you should use this.

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

**Note**: `getOrElseUpdate` is not an atomic operation in Caffeine or EhCache and is implemented as a `get` followed by computing the value from the `Callable`, then a `set`. This means it's possible for the value to be computed multiple times if multiple threads are calling `getOrElse` simultaneously.

To remove an item from the cache use the `remove` method:

@[remove](code/javaguide/cache/JavaCache.java)

To remove all items from the cache use the `removeAll` method:

@[removeAll](code/javaguide/cache/JavaCache.java)

`removeAll()` is only available on `AsyncCacheApi`, since removing all elements of the cache is rarely something you want to do sychronously. The expectation is that removing all items from the cache should only be needed as an admin operation in special cases, not part of the normal operation of your app.

Note that the [SyncCacheApi](api/java/play/cache/SyncCacheApi.html) has the same API, except it returns the values directly instead of using futures.

## Accessing different caches

It is possible to define and use different caches with different configurations by their name. To access different caches, when you inject them, use the [NamedCache](api/java/play/cache/NamedCache.html) qualifier on your dependency, for example:

@[qualified](code/javaguide/cache/qualified/Application.java)

If you want to access multiple different caches, then you'll need to tell Play to bind them in `application.conf`, like so:

    play.cache.bindCaches = ["db-cache", "user-cache", "session-cache"]

Defining and configuring named caches depends on the cache implementation you are using, examples of configuring named caches with Caffeine are given below.

### Configuring named caches with Caffeine

If you want to pass a default custom configuration that will be used as a fallback for all your caches you can do it by specifying:

```
    play.cache.caffeine.defaults = {
        initial-capacity = 200
        ...
    }
```


You can also pass custom configuration data for specific caches by doing:

```
    play.cache.caffeine.user-cache = {
        initial-capacity = 200
        ...
    }
```

### Configuring named caches with EhCache

With EhCache implementation, the default cache is called play, and can be configured by creating a file called ehcache.xml. Additional caches may be configured with different configurations, or even implementations.

By default, Play will try to create caches with names from `play.cache.bindCaches` for you. If you would like to define them yourself in `ehcache.xml`, you can set:

    play.cache.createBoundCaches = false


## Setting the execution context

By default, all Caffeine and EhCache operations are blocking, and async implementations will block threads in the default execution context.
Usually this is okay if you are using Play's default configuration, which only stores elements in memory since reads should be relatively fast.
However, depending on how cache was configured, this blocking I/O might be too costly.
For such a case you can configure a different [Akka dispatcher](https://doc.akka.io/docs/akka/current/dispatchers.html?language=scala#looking-up-a-dispatcher) and set it via `play.cache.dispatcher` so the cache plugin makes use of it:

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

It is possible to provide a custom implementation of the cache API. Make sure that you have the `cacheApi` dependency.

You can then implement [AsyncCacheApi](api/java/play/cache/AsyncCacheApi.html) and bind it in the DI container. You can also bind [SyncCacheApi](api/java/play/cache/SyncCacheApi.html) to [DefaultSyncCacheApi](api/java/play/cache/DefaultSyncCacheApi.html), which simply wraps the async implementation.

Note that the `removeAll` method may not be supported by your cache implementation, either because it is not possible or because it would be unnecessarily inefficient. If that is the case, you can throw an `UnsupportedOperationException` in the `removeAll` method.

To provide an implementation of the cache API in addition to the default implementation, you can either create a custom qualifier, or reuse the `NamedCache` qualifier to bind the implementation.


### Using Caffeine alongside your custom implementation

To use the default implementations of Caffeine you will need the `caffeine` dependency and you will have to disable Caffeine module from automatically binding it in `application.conf`:

```
play.modules.disabled += "play.api.cache.caffeine.CaffeineCacheModule"
```

### Using EhCache alongside your custom implementation

To use the default implementations of EhCache you will need the `ehcache` dependency and you will have to disable EhCache module from automatically binding it in `application.conf`:

```
play.modules.disabled += "play.api.cache.ehcache.EhCacheModule"
```
