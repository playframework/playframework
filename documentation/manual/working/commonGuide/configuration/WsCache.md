<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring WS Cache

[[Play WS|ScalaWS]] allows you to set up HTTP caching from configuration.

## Enabling Cache

You must have a [JSR 107](https://www.jcp.org/en/jsr/detail?id=107) cache implementation (aka JCache) available in your Play application to use Play WS's cache facility.  Play comes with an implementation that uses ehcache, so the easiest implementation is to add the following to `build.sbt`:

@[play-ws-cache-deps](code/build.sbt)

And enable the HTTP cache by adding the following to `application.conf`

```
play.ws.cache.enabled=true
```

If no JCache implementation is found, then Play WS will use an HTTP Cache with a stub cache that does not store anything.

## Enabling Freshness Heuristics

By default, Play WS does not cache HTTP responses when no explicit cache information is passed in.  However, HTTP caching does have an option to cache pages based off heuristics so that you can cache responses even without co-operation from the remote server.

To enable heuristics, set the following in `application.conf`:

```
play.ws.cache.heuristics.enabled=true
```

Play WS uses the [LM-Factor algorithm]( https://publicobject.com/2015/03/26/how-do-http-caching-heuristics-work/) to cache HTTP responses.

## Limiting Cache Size

Cache size is limited by the underlying cache implementation.  Play WS will create a generic cache if no cache was found, but you should bound the cache explicitly, as JCache does not provide many options.

> **NOTE**: If you do not limit the HTTP cache or expire elements in the cache, then you may cause the JVM to run out of memory.

In ehcache, you can specify an existing cache by specifying [CacheManager](https://static.javadoc.io/javax.cache/cache-api/1.0.0/javax/cache/CacheManager.html) resource explicitly, which is used in `cachingProvider.getCacheManager(uri, environment.classLoader)`:

```
play.ws.cache.cacheManagerResource="ehcache-play-ws-cache.xml"
```

and then adding a cache such as following into the `conf` directory:

```xml
<ehcache
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="http://ehcache.org/ehcache.xsd"
        name="play-ws-cache"
        updateCheck="false"
        >

	<cache name="play-ws-cache" maxEntriesLocalHeap="10000" eternal="false"
		timeToIdleSeconds="360" timeToLiveSeconds="1000" overflowToDisk="false" />

</ehcache>
```

> **NOTE**: `play.ws.cache.cacheManagerURI` is deprecated, use `play.ws.cache.cacheManagerResource` with a path on the classpath instead.

## Debugging the Cache

To see exactly what the HTTP caching layer in Play WS is doing, please add the following to `logback.xml`:

```
<logger name="play.api.libs.ws.ahc.cache" level="TRACE" />
```

## Defining a Caching Provider

You can define a specific [CachingProvider](https://static.javadoc.io/javax.cache/cache-api/1.0.0/javax/cache/spi/CachingProvider.html) for the WS cache, even if you are already using `ehcache` as a caching provider for Play Cache.  For example, you can load the [Caffeine](https://github.com/ben-manes/caffeine/wiki) library:

```
// https://mvnrepository.com/artifact/com.github.ben-manes.caffeine/jcache
libraryDependencies += "com.github.ben-manes.caffeine" % "jcache" % "2.4.0"
```

and then specify [Caffeine JCache](https://github.com/ben-manes/caffeine/wiki/JCache) as the caching provider:

```
play.ws.cache.cachingProviderName="<jcache caching provider class name>"
```

## Reference Configuration

The reference configuration shows the default settings for Play WS Caching:

@[](/confs/play-ahc-ws/reference.conf)
