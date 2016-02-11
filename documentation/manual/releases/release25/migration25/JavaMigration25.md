# Java Migration Guide

**TODO: Write intro here**



## Replaced functional types with Java 8 functional types

A big change in Play 2.5 is the change to use standard Java 8 classes where possible. All functional types have been replaced with their Java 8 counterparts, for example `F.Function1<A,R>` has been replaced with `java.util.function.Function<A,R>`.

The move to Java 8 types should enable better integration with other Java libraries as well as with built-in functionality in Java 8.

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

## Replaced `F.Promise` with Java 8's `CompletionStage`

APIs that use `F.Promise` now use the standard Java 8 [`CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) class.

### How to migrate

**Step 1:** Change all code that returns `F.Promise` to return `CompletionStage` instead. To aid with migration, `F.Promise` also implements the `CompletionStage` interface.

**Step 2** Replace relevant static methods in `F.Promise` with an equivalent method (many of these use the `play.libs.concurrent.Futures` helpers, or the statics on [`CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)):

* `Promise.wrap`       -> `scala.compat.java8.FutureConverters.toJava`
* `Promise.sequence    -> `Futures.sequence`
* `Promise.timeout`    -> `Futures.timeout`
* `Promise.pure`       -> `CompletableFuture.completedFuture`
* `Promise.throwing`   -> Construct `CompletableFuture` and use `completeExceptionally`
* `Promise.promise`    -> `CompletableFuture.supplyAsync`
* `Promise.delayed`    -> `Futures.delayed`

**Step 3** Replace existing instance methods with their equivalent on `CompletionStage`:

* `promise.or`          -> `future.applyToEither`
* `promise.onRedeem`    -> `future.thenAcceptAsync`
* `promise.map`         -> `future.thenApplyAsync`
* `promise.transform`   -> `future.handleAsync`
* `promise.zip`         -> `future.thenCombine` (and manually construct a tuple)
* `promise.fallbackTo`  -> `future.handleAsync` followed by `thenCompose(Function.identity())`
* `promise.recover`     -> `future.exceptionally` (or `future.handleAsync` with `HttpExecution#defaultContext()` if you want `Http.Context` captured).
* `promise.recoverWith` -> same as `recover`, then use `.thenCompose(Function.identity())`
* `promise.onFailure`   -> `future.whenCompleteAsync` (use `HttpExecution#defaultContext()` if needed)
* `promise.flatMap`     -> `future.thenComposeAsync` (use `HttpExecution#defaultContext()` if needed)
* `promise.filter`      -> `future.thenApplyAsync` and implement the filter manually (use `HttpExecution#defaultContext()` if needed)

These migrations are explained in more detail in the [Javadoc for `F.Promise`](api/java/play/libs/F.Promise.html).

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