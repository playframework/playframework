<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Cache APIs Migration

## New packages

Now `cache` has been split into a `cacheApi` component with just the API, and `ehcache` that contains the Ehcache implementation. If you are using the default Ehcache implementation, simply change `cache` to `ehcache` in your `build.sbt`:

```
libraryDependencies ++= Seq(
    ehcache
)
```

If you are defining a custom cache API, or are writing a cache implementation module, you can just depend on the API:

```
libraryDependencies ++= Seq(
    cacheApi
)
```

## Removed APIs

The deprecated Java class `play.cache.Cache` was removed and you now must inject an `play.cache.SyncCacheApi` or `play.cache.AsyncCacheApi`.

## New Sync and Async Cache APIs

The Cache API has been rewritten to have a synchronous and an asynchronous version. The old APIs will still work but they are now deprecated.

### Java API

The interface `play.cache.CacheApi` is now deprecated and should be replaced by `play.cache.SyncCacheApi` or `play.cache.AsyncCacheApi`.

To use, `play.cache.SyncCacheApi` just inject it:

```java
public class SomeController extends Controller {

    private SyncCacheApi cacheApi;

    @Inject
    public SomeController(SyncCacheApi cacheApi) {
        this.cacheApi = cacheApi;
    }
}
```

And then there is the asynchronous version of the API:

```java
public class SomeController extends Controller {

    private AsyncCacheApi cacheApi;

    @Inject
    public SomeController(AsyncCacheApi cacheApi) {
        this.cacheApi = cacheApi;
    }
}
```

See more details about how to use both APIs at [[specific documentation|JavaCache]].

### Scala API

The trait `play.api.cache.CacheApi` is now deprecated and should be replaced by `play.api.cache.SyncCacheApi` or `play.api.cache.AsyncCacheApi`.

To use `play.api.cache.SyncCacheApi`, just inject it:

```scala
class Application @Inject() (cache: SyncCacheApi) extends Controller {

}
```

Basically the same for `play.api.cache.AsyncCacheApi`:

```scala
class Application @Inject() (cache: AsyncCacheApi) extends Controller {

}
```

See more details about how to use both APIs at [[specific documentation|ScalaCache]].
