# Cache Migration

## Changed default caching provider

The default Cache has been moved from EhCache to Caffeine Cache.

## Migrating to Caffeine Cache

To migrate from EhCache to Caffeine you will have to remove `ehcache` from your dependencies and replace it with `caffeine` and you will have to change your EhCache configuration to Caffeine Cache configuration.

You can read more about configuring Caffeine Cache in the [[cache documentation|JavaCache]].

If you added the full ehcache implementation to your project you might want to remove it and go with caffeine cache.

## Using EhCache

If you want to keep using EhCache you will have to add `ehcache` to your dependencies:

```

libraryDependencies ++= Seq(
  ehcache
)
```

You can find the full documentation for using EhCache with Java here [[ehcache documentation for java|JavaEhCache]]. If you are using Scala the documentation is [[here|ScalaEhCache]].