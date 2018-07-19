# What's new in Play 2.7

This page highlights the new features of Play 2.7. If you want to learn about the changes you need to make when you migrate to Play 2.7, check out the [[Play 2.7 Migration Guide|Migration27]].

## Scala 2.13 support

Play 2.7 is the first release of Play to have been cross built against Scala 2.12 and 2.13. Many dependencies were updated so that we can have support for both versions.

You can select which version of Scala you would like to use by setting the `scalaVersion` setting in your `build.sbt`.

For Scala 2.12:

```scala
scalaVersion := "2.12.6"
```

For Scala 2.13:

```scala
scalaVersion := "2.13.0-M3"
```

> **Note**: Keep in mind Scala 2.13 still does not have a final version.

## Lifecycle managed by Akka's Coordinated Shutdown

Play 2.6 introduced the usage of Akka's [Coordinated Shutdown](https://doc.akka.io/docs/akka/2.5/scala/actors.html#coordinated-shutdown) but still didn't use it all across the core framework or exposed it to the end user. Coordinated Shutdown is an Akka Extension with a registry of tasks that can be run in an ordered fashion during the shutdown of the Actor System.

Coordinated Shutdown internally handles Play 2.7 Play's lifecycle and an instance of `CoordinatedShutdown` is available for injection. Coordinated Shutdown gives you fine grained phases - organized as a [directed acyclic graph (DAG)](https://en.wikipedia.org/wiki/Directed_acyclic_graph) - where you can register tasks instead of just having a single phase like Play's application lifecycle. For example, you can add tasks to run before or after server binding, or after all the current requests finishes. Also, you will have better integration with [Akka Cluster](https://doc.akka.io/docs/akka/2.5/common/cluster.html).

You can find more details on the new section on [[Coordinated Shutdown on the Play manual|Shutdown]], or you can have a look at Akka's [reference docs on Coordinated Shutdown](https://doc.akka.io/docs/akka/2.5/scala/actors.html#coordinated-shutdown).

## Guice was upgraded to 4.2.0

Guice, the default dependency injection framework used by Play, was upgraded to 4.2.0 (from 4.1.0). Have a look at its [release notes](https://github.com/google/guice/wiki/Guice42). This new Guice version introduces breaking changes, so make sure you check the [[Play 2.7 Migration Guide|Migration27]].

## Constraint annotations offered for Play Java are now @Repeatable

All of the constraint annotations defined by `play.data.validation.Constraints` are now `@Repeatable`. This change lets you, for example, reuse the same annotation on the same element several times but each time with different `groups`. For some constraints however it makes sense to let them repeat itself anyway, like `@ValidateWith`:

```java
@Validate(groups={GroupA.class})
@Validate(groups={GroupB.class})
public class MyForm {

    @ValidateWith(MyValidator.class)
    @ValidateWith(MyOtherValidator.class)
    @Pattern(value="[a-k]", message="Should be a - k")
    @Pattern(value="[c-v]", message="Should be c - v")
    @MinLength(value=4, groups={GroupA.class})
    @MinLength(value=7, groups={GroupB.class})
    private String name;

    //...
}
```

You can of course also make your own custom constraints `@Repeatable` as well and Play will automatically recognise that.

## Support for Caffeine

Play now offers a CacheApi implementation based on [Caffeine](https://github.com/ben-manes/caffeine/). Caffeine is the recommended cache implementation for Play users.

To migrate from EhCache to Caffeine you will have to remove `ehcache` from your dependencies and replace it with `caffeine`. To customize the settings from the defaults, you will also need to update the configuration in application.conf as explained in the documentation.

Read the documentation for the [[Java cache API|JavaCache]] and [[Scala cache API|ScalaCache]] to learn more about configuring caching with Play.

## New Content Security Policy Filter

There is a new [[Content Security Policy filter|CspFilter]] available that supports CSP nonce and hashes for embedded content.

The previous setting of enabling CSP by default and setting it to `default-src 'self'` was too strict, and interfered with plugins.  The CSP filter is not enabled by default, and the `contentSecurityPolicy` in the [[SecurityHeaders filter|SecurityHeaders]] is now deprecated and set to `null` by default.

The CSP filter uses Google's [Strict CSP policy](https://csp.withgoogle.com/docs/strict-csp.html) by default, which is a nonce based policy.  It is recommended to use this as a starting point, and use the included CSPReport body parsers and actions to log CSP violations before enforcing CSP in production.

## HikariCP upgraded

[HikariCP](https://github.com/brettwooldridge/HikariCP) was updated to its latest major version. Have a look at the [[Migration Guide|Migration27#HikariCP]] to see what changed.

## Play WS `curl` filter for Java

Play WS enables you to create `play.libs.ws.WSRequestFilter` to inspect or enrich the requests made. Play provides a "log as `curl`" filter, but this was lacking for Java developers. You can now write something like:

```java
ws.url("https://www.playframework.com")
  .setRequestFilter(new AhcCurlRequestLogger())
  .addHeader("My-Header", "Header value")
  .get();
```

And then the following log will be printed:

```
curl \
  --verbose \
  --request GET \
  --header 'My-Header: Header Value' \\
  'https://www.playframework.com'
```

This can be specially useful if you want to reproduce the request in isolation and also change `curl` parameters to see how it goes.

## Gzip Filter now supports compression level configuration

When using [[gzip encoding|GzipEncoding]], you can now configure the compression level to use. You can configure it using `play.filters.gzip.compressionLevel`, for example:

```
play.filters.gzip.compressionLevel = 9
```

See more details at [[GzipEncoding]].