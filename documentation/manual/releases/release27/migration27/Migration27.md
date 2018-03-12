<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.7 Migration Guide

This is a guide for migrating from Play 2.6 to Play 2.7. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.6 Migration Guide|Migration26]].

### `play.Logger` deprecated

`play.Logger` has been deprecated in favor of using SLF4J directly. You can create an SLF4J logger with `private static final Logger logger = LoggerFactory.getLogger(YourClass.class);`. If you'd like a more concise solution, you may also consider [Project Lombok's `@Slf4j` annotation](https://projectlombok.org/features/log).

If you have a in your logback.xml referencing the `application` logger, you may remove it.

    <logger name="application" level="DEBUG" />

Each logger should have a unique name matching the name of the class it is in. In this way, you can configure a different log level for each class. You can also set the log level for a given package. E.g. to set the log level for all of Play's internal classes to the info level, you can set:

    <logger name="play" level="INFO" />

### Evolutions comment syntax changes

Play Evolutions now properly supports SQL92 comment syntax. This means you can write evolutions using `--` at the beginning of a line instead of `#` wherever you choose. Newly generated evolutions using the Evolutions API will now also use SQL92-style comment syntax in all areas. Documentation has also been updated accordingly to prefer the SQL92 style, though the older comment style is still fully supported.

### StaticRoutesGenerator removed

The `StaticRoutesGenerator`, which was deprecated in 2.6.0, has been removed. If you are still using it, you will likely have to remove a line like this so your build compiles:

```scala
routesGenerator := StaticRoutesGenerator
```

Then you should migrate your static controllers to use classes with instance methods.

If you were using the `StaticRoutesGenerator` with dependency-injected controllers, you likely want to remove the `@` prefix from the controller names. The `@` is only needed if you wish to have a new controller instance created on each request using a `Provider`, instead of having a single instance injected into the router.


### `application/javascript` as default content type for JavaScript
`application/javascript` is now the default content-type returned for JavaScript instead of `text/javascript`.

### `Router#withPrefix` should always add prefix

Previously, `router.withPrefix(prefix)` was meant to add a prefix to a router, but still allowed "legacy implementations" to update their existing prefix. Play's `SimpleRouter` and other classes followed this behavior. Now all implementations have been updated to add the prefix, so `router.withPrefix(prefix)` should always return a router that routes `s"$prefix/$path"` the same way `router` routes `path`.

By default routers are unprefixed, so this will only cause a change in behavior if you are calling `withPrefix` on a router that has already been returned by `withPrefix`. To replace a prefix that has already been set on a router, you must call `withPrefix` on the original unprefixed router rather than the prefixed version.

### BoneCP removed

BoneCP is removed. If your application is configured to use BoneCP, you need to switch to [HikariCP](http://brettwooldridge.github.io/HikariCP/) which is the default JDBC connection pool.

```
play.db.pool = "default"  # Use the default connection pool provided by the platform (HikariCP)
play.db.pool = "hikaricp" # Use HikariCP

```

You may need to reconfigure the pool to use HikariCP. For example, if you want to configure maximum number of connections for HikariCP, it would be as follows.

```
play.db.prototype.hikaricp.maximumPoolSize = 15
```

For more details, see [[JDBC configuration section|SettingsJDBC]].

Also, you can use your own pool that implements `play.api.db.ConnectionPool` by specifying the fully-qualified class name.

```
play.db.pool=your.own.ConnectionPool
```

### Java `Http.Context` changed

Request tags, which [[have been deprecated|Migration26#Request-tags-deprecation]] in Play 2.6, have finally been removed in Play 2.7.
Therefore the `args` map of a `Http.Context` instance no longer contains these removed request tags as well.
Instead you can use the `contextObj.request().attrs()` method now, which provides you the equivalent request attributes.
