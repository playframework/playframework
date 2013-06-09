# The Play cache API

The default implementation of the Cache API uses [EHCache](http://ehcache.org/). You can also provide your own implementation via a plug-in.

## Accessing the Cache API

The cache API is provided by the `play.api.cache.Cache` object. It requires a registered cache plug-in.

> **Note:** The API is intentionally minimal to allow several implementation to be plugged. If you need a more specific API, use the one provided by your Cache plugin.

Using this simple API you can either store data in cache:

@[set-value](code/ScalaCache.scala)


And then retrieve it later:

@[get-value](code/ScalaCache.scala)

There is also a convenient helper to retrieve from cache or set the value in cache if it was missing:

@[retrieve-missing](code/ScalaCache.scala)


To remove an item from the cache use the `remove` method:

@[remove-value](code/ScalaCache.scala)


## Caching HTTP responses

You can easily create smart cached actions using standard Action composition. 

> **Note:** Play HTTP `Result` instances are safe to cache and reuse later.

Play provides a default built-in helper for standard cases:

@[cached-action](code/ScalaCache.scala)


Or even:

@[composition-cached-action](code/ScalaCache.scala)


> **Next:** [[Calling web services | ScalaWS]]
