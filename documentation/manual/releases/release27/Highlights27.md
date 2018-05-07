# What's new in Play 2.7

This page highlights the new features of Play 2.7. If you want to learn about the changes you need to make when you migrate to Play 2.7, check out the [[Play 2.7 Migration Guide|Migration27]].

## Scala 2.13 support

Play 2.7 is the first release of Play to have been cross built against Scala 2.12 and 2.13. A number of dependencies we updated so that we can have support for both versions.

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

## Guice was upgraded to 4.2.0

Guice, the default dependency injection framework used by Play, was upgraded to 4.2.0 (from 4.1.0). Have a look at its [release notes](https://github.com/google/guice/wiki/Guice42). This new Guice version introduces breaking changes, so make sure you check the [[Play 2.7 Migration Guide|Migration27]].

## Constraint annotations offered for Play Java are now @Repeatable

All of the constraint annotations defined by `play.data.validation.Constraints` are now `@Repeatable`. This lets you, for example, reuse the same annotation on the same element several times but each time with different `groups`. For some constraints however it makes sense to let them repeat itself anyway, like `@ValidateWith`:

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

To migrate from EhCache to Caffeine you will have to remove `ehcache` from your dependencies and replace it with `caffeine`. To customize the settings from the defaults you will also need to update the configuration in application.conf as explained in the documentation.

Read the documentation for the [[Java cache API|JavaCache]] and [[Scala cache API|ScalaCache]] to learn more about configuring caching with Play.

## New Content Security Policy Filter

There is a new [[Content Security Policy filter|CspFilter]] available that supports CSP nonce and hashes for embedded content.

The previous setting of enabling CSP by default and setting it to `default-src 'self'` was too strict, and interfered with plugins.  The CSP filter is not enabled by default, and the `contentSecurityPolicy` in the [[SecurityHeaders filter|SecurityHeaders]] is now deprecated and set to `null` by default.

The CSP filter uses Google's [Strict CSP policy](https://csp.withgoogle.com/docs/strict-csp.html) by default, which is a nonce based policy.  It is recommended to use this as a starting point, and use the included CSPReport body parsers and actions to log CSP violations before enforcing CSP in production.

## HikariCP upgraded

[HikariCP](https://github.com/brettwooldridge/HikariCP) was updated to its latest major version. Have a look at the [[Migration Guide|Migration27#HikariCP]] to see what changed.
