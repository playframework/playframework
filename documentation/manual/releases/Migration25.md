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

And further assume that the `onClose` method was changed and it now takes a `java.lang.Runnable` in argument, instead of a `F.Callback0`. Here is how your implementation should be changed:

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

Wrapping the checked exception into a generic `RuntimeException` is not exactly considered a best practice. Therefore, you may want to create a specific exception type that is more appropriate to the abstraction. Having said that, if a lambda's body does not throw a checked exception, and you are using the java8 lambda syntax as we did above, then you won't need to change anything in your code.

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

You should `@Inject ActorSystem system` instead of using `play.libs.Akka.system()`
