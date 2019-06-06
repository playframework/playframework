<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Java Migration Guide

In order to better fit in to the Java 8 ecosystem, and to allow Play Java users to make more idiomatic use of Java in their applications, Play has switched to using a number of Java 8 types such as `CompletionStage` and `Function`. Play also has new Java APIs for `EssentialAction`, `EssentialFilter`, `Router`, `BodyParser` and `HttpRequestHandler`.

## New Java APIs

There are several API changes to accommodate writing filters and HTTP request handlers in Java, in particular with the `HttpRequestHandler` interface. If you are using Scala for these components you are still free to use the Scala API if you wish.

### Filter API

You will most likely use `EssentialAction` when creating a filter. You can either use the [`Filter`](api/java/play/mvc/Filter.html) API or the lower-level [`EssentialFilter`](api/java/play/mvc/EssentialFilter.html) API that operates on [`EssentialAction`](api/java/play/mvc/EssentialAction.html)s.

### HttpRequestHandler and ActionCreator

The [`HttpRequestHandler`](api/java/play/http/HttpRequestHandler.html) actually existed in Play 2.4, but now it serves a different purpose. The `createAction` and `wrapAction` methods have been moved to a new interface called [`ActionCreator`](api/java/play/http/ActionCreator.html), and are deprecated in `HttpRequestHandler`. These methods are only applied to Java actions, and are used to intercept requests to the controller's method call, but not all requests.

In 2.5, `HttpRequestHandler`'s main purpose is to provide a handler for the request right after it comes in. This is now consistent with what the Scala implementation does, and provides a way for Java users to intercept the handling of all HTTP requests. Normally, the `HttpRequestHandler` will call the router to find an action for the request, so the new API allows you to intercept that request in Java before it goes to the router.

## Using CompletionStage inside an Action

You must supply the HTTP execution context explicitly as an executor when using a Java `CompletionStage` inside an [[Action|JavaActions]], to ensure that the HTTP.Context remains in scope.  If you don't supply the HTTP execution context, you'll get "There is no HTTP Context available from here" errors when you call `request()` or other methods that depend on `Http.Context`.

You can supply the [`play.libs.concurrent.HttpExecutionContext`](api/java/play/libs/concurrent/HttpExecutionContext.html) instance through dependency injection:

``` java
public class Application extends Controller {
    @Inject HttpExecutionContext ec;

    public CompletionStage<Result> index() {
        someCompletableFuture.supplyAsync(() -> { 
          // do something with request()
        }, ec.current());
    }
}
```

## Replaced functional types with Java 8 functional types

A big change in Play 2.5 is the change to use standard Java 8 classes where possible. All functional types have been replaced with their Java 8 counterparts, for example `F.Function1<A,R>` has been replaced with `java.util.function.Function<A,R>`.

The move to Java 8 types should enable better integration with other Java libraries as well as with built-in functionality in Java 8.

### How to migrate

**Step 1:** Change all code that references Play functional interfaces to reference Java 8 interfaces instead.

You need to change code that explicitly mentions a type like `F.Function1`. For example:

```java
void myMethod(F.Callback0 block) { ... }
```

Becomes:

```java
void myMethod(Runnable block) { ... }
```

The table below shows all the changes:

| **old interface**    | **new interface**
| --------------------------------------
| `F.Callback0`        | `java.lang.Runnable`
| `F.Callback<A>`      | `java.util.function.Consumer<A>`
| `F.Callback2<A,B>`   | `java.util.function.BiConsumer<A,B>`
| `F.Callback3<A,B,C>` | No counterpart in Java 8, consider using `akka.japi.function.Function3`
| `F.Predicate<A>`     | `java.util.function.Predicate<A>`
| `F.Function0<R>`     | `java.util.function.Supplier<R>`
| `F.Function1<A,R>`   | `java.util.function.Function<A,R>`
| `F.Function2<A,B,R>` | `java.util.function.BiFunction<A,B,R>`

**Step 2:** Fix any errors caused by checked exceptions that are thrown inside your lambdas.

Unlike the Play functional interfaces, the Java 8 functional interfaces don't permit checked exceptions to be thrown. If your lambda expressions throw a checked exception then you'll need to change the code. (If you don't throw checked exceptions then you can leave the code unchanged.)

You may get a lot of compiler errors but it's pretty easy to get your code working again. Let's suppose your Play 2.4 code uses a `F.Callback0` lambda to stop a database:

```java
onClose(() -> {
    database.stop(); // <-- can throw an IOException
})
```

In Play 2.5 the `onClose` method has been changed to take a `java.lang.Runnable` argument instead of `F.Callback0`. Because `Runnable`s can't throw checked exceptions the code above won't compile in Play 2.5.

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

Durian provides other behaviors too, such as [logging an exception](https://diffplug.github.io/durian/javadoc/2.0/com/diffplug/common/base/Errors.html#log--) or writing [your own exception handler](https://diffplug.github.io/durian/javadoc/2.0/com/diffplug/common/base/Errors.html#createHandling-java.util.function.Consumer-). If you want to use Durian you can either include it as a [dependency in your project](https://mvnrepository.com/artifact/com.diffplug.durian/durian) or by copy the source from [two](https://github.com/diffplug/durian/blob/master/src/com/diffplug/common/base/Errors.java) [classes](https://github.com/diffplug/durian/blob/master/src/com/diffplug/common/base/Throwing.java) into your project.

## Replaced `F.Promise` with Java 8's `CompletionStage`

APIs that use `F.Promise` now use the standard Java 8 [`CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) class.

### How to migrate

**Step 1:** Change all code that returns `F.Promise` to return `CompletionStage` instead. To aid with migration, `F.Promise` also implements the `CompletionStage` interface, which means any existing code that returns `Promise` can still be invoked from code that has been migrated to use `CompletionStage`.

**Step 2:** Replace relevant static methods in `F.Promise` with an equivalent method (many of these use the [`play.libs.concurrent.Futures`](api/java/play/libs/concurrent/Futures.html) helpers, or the statics on [`CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)):

| `F.Promise` method  | alternative |
| --------------------------------------------|
| `Promise.wrap`      | `scala.compat.java8.FutureConverters.toJava` |
| `Promise.sequence`  | `Futures.sequence` |
| `Promise.timeout`   | `Futures.timeout` |
| `Promise.pure`      | `CompletableFuture.completedFuture` |
| `Promise.throwing`  | Construct `CompletableFuture` and use `completeExceptionally` |
| `Promise.promise`   | `CompletableFuture.supplyAsync` |
| `Promise.delayed`   | `Futures.delayed` |

**Step 3:** Replace existing instance methods with their equivalent on `CompletionStage`:

| `F.Promise`         | `CompletionStage` |
| -----------------------------------------------|
| `or`                | `applyToEither` |
| `onRedeem`          | `thenAcceptAsync` |
| `map`               | `thenApplyAsync` |
| `transform`         | `handleAsync` |
| `zip`               | `thenCombine` (and manually construct a tuple) |
| `fallbackTo`        | `handleAsync` followed by `thenCompose(Function.identity())` |
| `recover`           | `exceptionally` (or `handleAsync` with `HttpExecution#defaultContext()` if you want `Http.Context` captured). |
| `recoverWith`       | same as `recover`, then use `.thenCompose(Function.identity())` |
| `onFailure`         | `whenCompleteAsync` (use `HttpExecution#defaultContext()` if needed) |
| `flatMap`           | `thenComposeAsync` (use `HttpExecution#defaultContext()` if needed) |
| `filter`            | `thenApplyAsync` and implement the filter manually (use `HttpExecution#defaultContext()` if needed) |

These migrations are explained in more detail in the Javadoc for `F.Promise`.

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

`Optional` has a lot more combinators, so we highly encourage you to [learn its API](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) if you are not familiar with it already.

## Thread Local attributes

Thread Local attributes such as `Http.Context`, `Http.Session` etc are no longer passed to a different execution context when used with `CompletionStage` and `*Async` callbacks. 
More information is [here](https://www.playframework.com/documentation/2.5.x/ThreadPools#Java-thread-locals)

## Deprecated static APIs

Several static APIs were deprecated in Play 2.5, in favour of using dependency injected components. Using static global state is bad for testability and modularity, and it is recommended that you move to dependency injection for accessing these APIs. You should refer to the list in the [[Play 2.4 Migration Guide|Migration24#Dependency-Injected-Components]] to find the equivalent dependency injected component for your static API.
