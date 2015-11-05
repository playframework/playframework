<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.5 Migration Guide

This is a guide for migrating from Play 2.4 to Play 2.5. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.4 Migration Guide|Migration24]].

## Scala 2.10 support discontinued

While Play 2.4 was cross compiled against both Scala 2.10 and Scala 2.11, this new release of Play is only available for Scala 2.11. The reason for dropping Scala 2.10 support is that Play has a new library dependency on [scala-java8-compat](https://github.com/scala/scala-java8-compat), which is only available for Scala 2.11. This library makes it easy to convert from and to common Scala and Java8 types, and hence it's valuable to simplify the Play core. Furthermore, you may also find it handy to use in your own Play project. For example, if you need to convert Scala `Future` instances into Java `CompletionStage` (or the other way around).

## Replaced functional types with Java8 functional types

All functional types have been replaced with their Java8 counterparts:

* `F.Callback0`        -> `java.lang.Runnable`
* `F.Callback<A>`      -> `java.util.function.Consumer<A>`
* `F.Callback2<A,B>`   -> `java.util.function.BiConsumer<A,B>`
* `F.Callback3<A,B,C>` -> no counterpart in Java8
* `F.Predicate<A>`     -> `java.util.function.Predicate<A>`
* `F.Function0<A>`     -> `java.util.function.Supplier<A>`
* `F.Function1<A,R>`   -> `java.util.function.Function<A,R>`
* `F.Function2<A,B,R>` -> `java.util.function.BiFunction<A,B,R>`

Besides the name change, the main difference is in the methods' signature. In fact, while the method in the Play types could throw an exception, the Java8 types don't. The consequence is that you will now have to catch exceptions in the lambda's body if it can throw a checked exception. Practically, this is easy to fix. Let's suppose you used to pass a `F.Callback0` instance to an `onClose` method:

```java
onClose(() -> {
    database.stop(); // <-- suppose this can throw an IOException
})
```

And further assume that the `onClose` method was changed and it now takes a `java.lang.Runnable` argument, instead of `F.Callback0`. Because `Runnable` cannot throw a checked exception, you must change your implementation using something like this:

```java
onClose(() -> {
    try {
        database.stop(); // <-- suppose this can throw an IOException
    }
    catch(IOException e) {
        throw new RuntimeException(e);   
    }
})
```

To avoid copy-pasting a repetitive try-catch construct all across your project, you might consider adopting the [Durian](https://github.com/diffplug/durian) library's [`Errors`](https://github.com/diffplug/durian/blob/master/test/com/diffplug/common/base/ErrorsExample.java?ts=4) class (either through [Maven](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.durian%22%20AND%20a%3A%22durian%22) or by copy-pasting [two](https://github.com/diffplug/durian/blob/master/src/com/diffplug/common/base/Errors.java) [classes](https://github.com/diffplug/durian/blob/master/src/com/diffplug/common/base/Throwing.java)).  It allows you to easily wrap lambdas which throw checked exceptions as their standard Java 8 functional interface, as such:

```java
onClose(Errors.rethrow().wrap(database::stop));
onClose(Errors.log().wrap(database::stop));
// You can also make your own error handler, see https://github.com/diffplug/durian/blob/master/test/com/diffplug/common/base/ErrorsExample.java?ts=4
```

If you are using the java8 lambda syntax and the lambda's body does not throw a checked exception, then you won't need to change anything in your code.

Last but not least, if you are using `F.Callback3<A,B,C>` in your application, since there is no Java8 replacement for it, you may want to use `akka.japi.function.Function3`.

## Replaced `F.Option` with Java8 `Optional`

`F.Option` has been discontinued and we embraced the Java8 `Optional` type. The two types are similar, but their API is different, so you will need to update your code. The main difference between the two types is that while `F.Option` inherited from `java.util.Collection`, `Optional` doesn't.

Here follows a short table that should ease the migration:

|  F.Option  |  Optional  |
| ---------- | ---------- |
| None       | empty      |
| Some       | ofNullable |
| isDefined  | isPresent  |
| isEmpty    | !isPresent |
| get        | get        |
| getOrElse  | ifPresent  |
| map        | map        |

`Optional` has a lot more combinators, so we highly encourage you to discover its API if you are not familiar with it already.

## Replaced static methods with dependency injection

If you are using `controllers.ExternalAssets` in your routes file you must either set `routesGenerator := InjectedRoutesGenerator` in your `build.sbt` or you must use the `@` symbol in front of the route like `GET /some/path @controllers.ExternalAssets.at`

## Refactored Logback as an optional dependency

The runtime dependency on Logback has been removed, and Play can now use any SLF4J compatible logging framework.  Logback is included by default, but because it exists as a separate module outside of Play (and is not part of the Logger class), the `play.api.Logger$ColoredLevel` converter in logback.xml has changed to `play.api.libs.logback.ColoredLevel`:

```
<conversionRule conversionWord="coloredLevel" converterClass="play.api.libs.logback.ColoredLevel" />
```

Details on how to set up Play with different logging frameworks are in [[Configuring logging|SettingsLogger]] section.
