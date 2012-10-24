# The Play cache API

Caching data is a typical optimization in modern applications, and so Play provides a global cache. An important point about the cache is that it behaves just like a cache should: the data you just stored may just go missing.

For any data stored in the cache, a regeneration strategy needs to be put in place in case the data goes missing. This philosophy is one of the fundamentals behind Play, and is different from Java EE, where the session is expected to retain values throughout its lifetime. 

The default implementation of the cache API uses [[EHCache| http://ehcache.org/]]. You can also provide your own implementation via a plugin.

## Accessing the Cache API

The cache API is provided by the `play.cache.Cache` object. This requires a cache plugin to be registered.

> **Note:** The API is intentionally minimal to allow various implementations to be plugged in. If you need a more specific API, use the one provided by your Cache plugin.

Using this simple API you can store data in the cache:

```
Cache.set("item.key", frontPageNews);
```

You can retrieve the data later:

```
News news = Cache.get("item.key");
```

How to remove the key is as follows.

```
// 2.0 final
Cache.set("item.key", null, 0)
// later
Cache.remove("item.key")

```

## Caching HTTP responses

You can easily create a smart cached action using standard `Action` composition. 

> **Note:** Play HTTP `Result` instances are safe to cache and reuse later.

Play provides a default built-in helper for the standard case:

```
@Cached("homePage")
public static Result index() {
  return ok("Hello world");
}
```

## Caching in templates

You may also access the cache from a view template.

```
@cache.Cache.getOrElse("cached-content", 3600) {
     <div>I’m cached for an hour</div>
}
```

## Session cache

Play provides a global cache, whose data are visible to anybody. How would one restrict visibility to a given user? For instance you may want to cache metrics that only apply to a given user.


```
// Generate a unique ID
String uuid=session("uuid");
if(uuid==null) {
	uuid=java.util.UUID.randomUUID().toString();
	session("uuid", uuid);
}

// Access the cache
News userNews = Cache.get(uuid+"item.key");
if(userNews==null) {
	userNews = generateNews(uuid);
	Cache.set(uuid+"item.key", userNews );
}
```


> **Next:** [[Calling web services | JavaWS]]
