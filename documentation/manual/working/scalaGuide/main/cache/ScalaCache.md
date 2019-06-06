<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# The Play cache API

Caching data is a typical optimization in modern applications, and so Play provides a global cache.

> An important point about the cache is that it behaves just like a cache should: the data you just stored may just go missing.

For any data stored in the cache, a regeneration strategy needs to be put in place in case the data goes missing. This philosophy is one of the fundamentals behind Play, and is different from Java EE, where the session is expected to retain values throughout its lifetime.

The default implementation of the Cache API uses [Ehcache](http://ehcache.org/).

## Importing the Cache API

Play provides both an API and an default Ehcache implementation of that API. To get the full Ehcache implementation, add `ehcache` to your dependencies list:

@[ehcache-sbt-dependencies](code/cache.sbt)

This will also automatically set up the bindings for runtime DI so the components are injectable. If you are using compile-time DI, mix [EhCacheComponents](api/scala/play/api/cache/ehcache/EhCacheComponents.html) into your components cake to get access to the `defaultCacheApi` and the `cacheApi` method for getting a cache by name.

To add only the API, add `cacheApi` to your dependencies list.

@[cache-sbt-dependencies](code/cache.sbt)

The API dependency is useful if you'd like to define your own bindings for the `Cached` helper and `AsyncCacheApi`, etc., without having to depend on Ehcache. If you're writing a custom cache module you should use this.

## JCache Support

Ehcache implements the [JSR 107](https://github.com/jsr107/jsr107spec) specification, also known as JCache, but Play does not bind `javax.caching.CacheManager` by default.  To bind `javax.caching.CacheManager` to the default provider, add the following to your dependencies list:

@[jcache-sbt-dependencies](code/cache.sbt)

If you are using Guice, you can add the following for Java annotations:

@[jcache-guice-annotation-sbt-dependencies](code/cache.sbt)

## Accessing the Cache API

The cache API is defined by the [AsyncCacheApi](api/scala/play/api/cache/AsyncCacheApi.html) and [SyncCacheApi](api/scala/play/api/cache/SyncCacheApi.html) traits, depending on whether you want an asynchronous or synchronous implementation, and can be injected into your component like any other dependency.  For example:

@[inject](code/ScalaCache.scala)

> **Note:** The API is intentionally minimal to allow several implementation to be plugged in. If you need a more specific API, use the one provided by your Cache plugin.

Using this simple API you can store data in cache:

@[set-value](code/ScalaCache.scala)

And then retrieve it later:

@[get-value](code/ScalaCache.scala)

There is also a convenient helper to retrieve from cache or set the value in cache if it was missing:

@[retrieve-missing](code/ScalaCache.scala)

**Note**: `getOrElseUpdate` is not an atomic operation in Ehcache and is implemented as a `get` followed by computing the value, then a `set`. This means it's possible for the value to be computed multiple times if multiple threads are calling `getOrElse` simultaneously.

You can specify an expiry duration by passing a duration, by default the duration is infinite:

@[set-value-expiration](code/ScalaCache.scala)

To remove an item from the cache use the `remove` method:

@[remove-value](code/ScalaCache.scala)

To remove all items from the cache use the `removeAll` method:

@[removeAll-values](code/ScalaCache.scala)

`removeAll()` is only available on `AsyncCacheApi`, since removing all elements of the cache is rarely something you want to do sychronously. The expectation is that removing all items from the cache should only be needed as an admin operation in special cases, not part of the normal operation of your app.

Note that the [SyncCacheApi](api/scala/play/api/cache/SyncCacheApi.html) has the same API, except it returns the values directly instead of using futures.

## Accessing different caches

It is possible to access different caches. In the default Ehcache implementation, the default cache is called `play`, and can be configured by creating a file called `ehcache.xml`. Additional caches may be configured with different configurations, or even implementations.

If you want to access multiple different ehcache caches, then you'll need to tell Play to bind them in `application.conf`, like so:

    play.cache.bindCaches = ["db-cache", "user-cache", "session-cache"]

By default, Play will try to create these caches for you. If you would like to define them yourself in `ehcache.xml`, you can set:

    play.cache.createBoundCaches = false

Now to access these different caches, when you inject them, use the [NamedCache](api/java/play/cache/NamedCache.html) qualifier on your dependency, for example:

@[qualified](code/ScalaCache.scala)

## Setting the execution context

By default, all Ehcache operations are blocking, and async implementations will block threads in the default execution context. Usually this is okay if you are using Play's default configuration, which only stores elements in memory since reads should be relatively fast. However, depending on how EhCache was configured and [where the data is stored](https://www.ehcache.org/generated/2.10.4/html/ehc-all/#page/Ehcache_Documentation_Set%2Fco-store_storage_tiers.html), this blocking I/O might be too costly. For such a case you can configure a different [Akka dispatcher](https://doc.akka.io/docs/akka/current/dispatchers.html?language=scala#looking-up-a-dispatcher) and set it via `play.cache.dispatcher` so the EhCache plugin makes use of it:

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

You can easily create smart cached actions using standard Action composition.

> **Note:** Play HTTP `Result` instances are safe to cache and reuse later.

The [Cached](api/scala/play/api/cache/Cached.html) class helps you build cached actions.

@[cached-action-app](code/ScalaCache.scala)

You can cache the result of an action using a fixed key like `"homePage"`.

@[cached-action](code/ScalaCache.scala)

If results vary, you can cache each result using a different key. In this example, each user has a different cached result.

@[composition-cached-action](code/ScalaCache.scala)

### Control caching

You can easily control what you want to cache or what you want to exclude from the cache.

You may want to only cache 200 Ok results.

@[cached-action-control](code/ScalaCache.scala)

Or cache 404 Not Found only for a couple of minutes

@[cached-action-control-404](code/ScalaCache.scala)

## Custom implementations

It is possible to provide a custom implementation of the cache API that either replaces, or sits along side the default implementation.

To replace the default implementation based on something other than Ehcache, you only need the `cacheApi` dependency rather than the `ehcache` dependency in your `build.sbt`. If you still need access to the Ehcache implementation classes, you can use `ehcache` and disable the module from automatically binding it in `application.conf`:

```
play.modules.disabled += "play.api.cache.ehcache.EhCacheModule"
```

You can then implement [AsyncCacheApi](api/java/play/cache/AsyncCacheApi.html) and bind it in the DI container. You can also bind [SyncCacheApi](api/java/play/cache/SyncCacheApi.html) to [DefaultSyncCacheApi](api/java/play/cache/DefaultSyncCacheApi.html), which simply wraps the async implementation.

Note that the `removeAll` method may not be supported by your cache implementation, either because it is not possible or because it would be unnecessarily inefficient. If that is the case, you can throw an `UnsupportedOperationException` in the `removeAll` method.

To provide an implementation of the cache API in addition to the default implementation, you can either create a custom qualifier, or reuse the `NamedCache` qualifier to bind the implementation.
