<!--- Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.5 Migration Guide

This is a guide for migrating from Play 2.4 to Play 2.5. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.4 Migration Guide|Migration24]].


**TODO: Review all headings to make them clear, succinct and consistent. If heading names change, check that backlinks from the Higlights still work.**

**TODO: Review all sections and make sure they have the following structure: a high level description at the start and then a practical *How to migrate* section explaining concrete steps that are needed.**

**TODO: Arrange sections into priority order so that the bits that most people need to know about come first. Migration that's necessary can come first, migration that's optional (e.g. deprecation) can come later. Consider putting large sections (e.g. Java 8 migration, Akka streams migration) onto separate pages.**

## Scala 2.10 support discontinued

Play 2.3 and 2.4 supported both Scala 2.10 and 2.11. Play 2.5 has dropped support for Scala 2.10 and now only supports Scala 2.11. There are a couple of reasons for this:

1. Play 2.5's internal code makes extensive use of the [scala-java8-compat](https://github.com/scala/scala-java8-compat) library, which only supports Scala 2.11. The *scala-java8-compat* has conversions between many Scala and Java 8 types, such as Scala `Future`s and Java `CompletionStage`s. (You might find this library useful for your code too.)

2. The next version of Play will probably add support for Scala 2.12. It's time for Play to move to Scala 2.11 so that the upcoming transition to 2.12 will be easier.

### How to migrate

**TODO: Describe changes to build.sbt needed to upgrade a Play project from Scala 2.10 to Scala 2.11, emphasize that both Java and Scala users may need to change this setting, link to any docs that describe what's new in Scala 2.11**


## Replaced `F.Promise` with Java 8's `CompletionStage`

**TODO: Fill in intro and rationale for this change. Might want to take some text from the section on 'replacing functional types'.**

### How to migrate

**TODO: Fill in concrete migration steps. Explain how `F.Promise` extends `CompletionStage` and what that means for code that creates a promise, code that takes a promise as an argument, etc**


## Replaced functional types with Java 8 functional types

A big change in Scala 2.5 is the change to use standard Java 8 classes where possible. All functional types have been replaced with their Java 8 counterparts, for example `F.Function1<A,R>` has been replaced with `java.util.function.Function<A,R>`.

### How to migrate

**Step 1:** Change all code that references Play functional interfaces to reference Java 8 interfaces instead.

You need to change code that explicitly mentions a type like `F.Function1`. For example:

```java
void myMethod(F.Callback0 block) { ... }
```

becomes

```java
void myMethod(Runnable block) { ... }
```

The table below shows all the changes:

* `F.Callback0`        -> `java.lang.Runnable`
* `F.Callback<A>`      -> `java.util.function.Consumer<A>`
* `F.Callback2<A,B>`   -> `java.util.function.BiConsumer<A,B>`
* `F.Callback3<A,B,C>` -> No counterpart in Java 8, consider using `akka.japi.function.Function3`
* `F.Predicate<A>`     -> `java.util.function.Predicate<A>`
* `F.Function0<A>`     -> `java.util.function.Supplier<A>`
* `F.Function1<A,R>`   -> `java.util.function.Function<A,R>`
* `F.Function2<A,B,R>` -> `java.util.function.BiFunction<A,B,R>`

**TODO: Change this to a table, link to javadoc for each class**

**Step 2:** Fix any errors caused by checked exceptions that are thrown inside your lambdas.

Unlike the Play functional interfaces, the Java 8 functional interfaces don't permit checked exceptions to be thrown. If your lambda expressions throw a checked exception then you'll need to change the code. (If you don't throw checked exceptions then you can leave the code unchanged.)

You may get a lot of compiler errors but it's pretty easy to get your code working again. Let's suppose your Play 2.4 code uses a `F.Callback0` lambda to stop a database:

```java
onClose(() -> {
    database.stop(); // <-- can throw an IOException
})
```

In Play 2.5 the `onClose` method maybe have been changed to take a `java.lang.Runnable` argument instead of `F.Callback0`. Because `Runnable`s can't throw checked exceptions the code above won't compile in Play 2.5.

To get the code to compile you can change your lambda code to catch the checked exception (`IOException`) and wrap it in an unchecked exception (`RuntimeException`). It's OK for a `Runnable` to throw an unchecked exception so the code will now compile.

```java
onClose(() -> {
    try {
        database.stop(); // <-- can throw an IOException
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
})
```

If you don't like adding *try-catch* blocks into your code you can use the [Durian](https://github.com/diffplug/durian) library's [`Errors`](https://diffplug.github.io/durian/javadoc/2.0/com/diffplug/common/base/Errors.html) class to handle exceptions for you.

For example, you can get the same behavior as above, where you convert a checked exception into an unchecked exception, with the code below:

```java
onClose(Errors.rethrow().wrap(database::stop));
```

Durian provides other behaviors too, such as [logging an exception](https://diffplug.github.io/durian/javadoc/2.0/com/diffplug/common/base/Errors.html#log--) or writing [your own exception handler](https://diffplug.github.io/durian/javadoc/2.0/com/diffplug/common/base/Errors.html#createHandling-java.util.function.Consumer-). If you want to use Durian you can either include it as a [dependency in your project](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.durian%22%20AND%20a%3A%22durian%22) or by copy the source from [two](https://github.com/diffplug/durian/blob/master/src/com/diffplug/common/base/Errors.java) [classes](https://github.com/diffplug/durian/blob/master/src/com/diffplug/common/base/Throwing.java) into your project.


## Replaced `F.Option` with Java 8's `Optional`

The Play Java API has been converted to use the Java 8 [`Optional`](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) class instead of Play's `F.Option` type. The `F.Option` type has been removed.

### How to migrate

Replace code that uses `F.Option` with `Optional`. The two types are similar, but their API is different, so you will need to update your code. The main difference between the two types is that while `F.Option` inherits `java.util.Collection`, `Optional` doesn't.

Here follows a short table that should ease the migration:

|  `F.Option`        |  `Optional`                       |
| ----------         | ----------                        |
| `F.Option.None()`  | `Optional.empty()`                |
| `F.Option.Some(v)` | `Optional.ofNullable(v)`          |
| `o.isDefined()`    | `o.isPresent()`                   |
| `o.isEmpty()`      | `!o.isPresent()`                  |
| `o.get()`          | `o.get()`                         |
| `o.getOrElse(f)`   | `o.orElseGet(f)` or `o.orElse(v)` |
| `o.map(f)`         | `o.map(f)`                        |

**TODO: Add links to javadoc**

`Optional` has a lot more combinators, so we highly encourage you to [learn its API](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) if you are not familiar with it already.


## Replaced static methods with dependency injection

**TODO: Explain what this change is and why it was made. I don't really understand the heading! Does it mean that ExternalAssets is no longer a singleton object with static methods, but now a class that's created with dependency injection? Is it just ExternalAssets?**

### How to migrate

If you are using `controllers.ExternalAssets` in your routes file you must either set

```scala
routesGenerator := InjectedRoutesGenerator
```

in your `build.sbt` or you must use the `@` symbol in front of the route in the `routes` file, e.g.

```
GET /some/path @controllers.ExternalAssets.at
```

## Change to Logback configuration

As part of the change to remove Play's hardcoded dependency on Logback [[(see Highlights)|Highlights25#Support-for-other-logging-frameworks]], one of the classes used by Logback configuration had to be moved to another package.

### How to migrate

You will need to update your Logback configuration files (`logback*.xml`) and change any references to the old `play.api.Logger$ColoredLevel` to the new `play.api.libs.logback.ColoredLevel` class.

The new configuration after the change will look something like this:

```xml
<conversionRule conversionWord="coloredLevel" converterClass="play.api.libs.logback.ColoredLevel" />
```

You can find more details on how to set up Play with different logging frameworks are in [[Configuring logging|SettingsLogger#Using-a-Custom-Logging-Framework]] section of the documentation.


## Renamed WS AsyncHttpClient classes

The [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client) is the technology underneath Play's WS client. Many of Play's WS classes used the outdated name "Ning" to refer to the AsyncHttpClient library. To properly reflect the AsyncHttpClient project name Play 2.5 renames or deprecates classes using the name "Ning".

### How to migrate

**TODO: Check the list of classes and config settings that have changed and document them here. Check which bits are deprecated. I think config using the old name will give a warning at runtime.** More info in the PR: https://github.com/playframework/playframework/pull/5224. **Some text we might want to use:** "package `play.api.libs.ws.ning` was renamed into `play.api.libs.ws.ahc` and `Ning*` classes were renamed into `Ahc*`."


## Deprecated `GlobalSettings`

**TODO: Describe deprecation and the rationale**

### Migration

**TODO: Explain how to migrate to a DI alternative, providing links**


## Akka Streams replaces iteratees

**TODO: Explain change.**

### How to migrate

**TODO: Explain how to change iteratees to streams. Explain the deprecation/removal plan for iteratees. Do we need separate sections for result streaming and HttpEntities, body parsers and Accumulators, WS and WebSockets?**


## Removed Plugins API

The Plugins API was deprecated in Play 2.4 and has been removed in Play 2.5. The Plugins API has been superceded by Play's dependency injection and module system which provides a cleaner and more flexible way to build reusable components.

### How to migrate

Read about how to create reusable components using dependency injection independent in either [[Scala|ScalaDependencyInjection]] or [[Java|JavaDependencyInjection]].

A plugin will usually be a class that is:

* a singleton ([[Java|JavaDependencyInjection#Singletons]] / [[Scala|ScalaDependencyInjection#Singletons]])
* is a dependency for some classes and has dependencies on other components ([[Java|JavaDependencyInjection#Declaring-dependencies]] / [[Scala|ScalaDependencyInjection#Declaring-dependencies]])
* optionally bound in a module ([[Java|JavaDependencyInjection#Programmatic-bindings]] / [[Scala|ScalaDependencyInjection#Programmatic-bindings]])
* optionally started when the application starts ([[Java|JavaDependencyInjection#Eager-bindings]] / [[Scala|ScalaDependencyInjection#Eager-bindings]])
* optionally stopped when the application stops ([[Java|JavaDependencyInjection#Stopping/cleaning-up]] / [[Scala|ScalaDependencyInjection#Stopping/cleaning-up]])

> Note: As part of this effort, the [[modules directory|ModuleDirectory]] has been updated to only include up-to-date modules that do not use the Plugin API. If you have any corrections to make to the module directory please let us know.