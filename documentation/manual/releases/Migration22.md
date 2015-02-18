<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.2 Migration Guide

This is a guide for migrating from Play 2.1 to Play 2.2. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.1 Migration Guide|Migration21]].

## Build tasks

### Update the Play organization and version

Play is now published under a different organisation id.  This is so that eventually we can deploy Play to Maven Central.  The old organisation id was `play`, the new one is `com.typesafe.play`.

The version also must be updated to 2.2.0.

In `project/plugins.sbt`, update the Play plugin to use the new organisation id:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0")
```

In addition, if you have any other dependencies on Play artifacts, and you are not using the helpers to depend on them, you may have to update the organisation and version numbers there.

### Update SBT version

`project/build.properties` is required to be updated to use sbt 0.13.0.

### Update root project

If you're using a multi-project build, and none of the projects has a root directory of the current directory, the root project is now determined by overriding rootProject instead of alphabetically:

```scala
override def rootProject = Some(myProject) 
```

### Update Scala version

If you have set the scalaVersion (e.g. because you have a multi-project build that uses Project in addition to play.Project), you should update it to 2.10.2.

### Play cache module

Play cache is now split out into its own module.  If you are using the Play cache, you will need to add this as a dependency.  For example, in `Build.scala`:

```scala
val addDependencies = Seq(
  jdbc,
  cache,
  ...
)
```

Note that if you depend on plugins that depend on versions of Play prior to 2.2 then there will be a conflict within caching due to multiple caches being loaded. Update to a later plugin version or ensure that older Play versions are excluded if you see this issue.

### sbt namespace no longer extended

The `sbt` namespace was previously extended by Play e.g. `sbt.PlayCommands.intellijCommandSettings`. This is considered bad practice and so
Play now uses its own namespace for sbt related things e.g. `play.PlayProject.intellijCommandSettings`.

## New results structure in Scala

In order to simplify action composition and filtering, the Play results structure has been simplified.  There is now only one type of result, `SimpleResult`, where before there were `SimpleResult`, `ChunkedResult` and `AsyncResult`, plus the interfaces `Result` and `PlainResult`.  All except `SimpleResult` have been deprecated.  `Status`, a subclass of `SimpleResult`, still exists as a convenience class for building results.  In most cases, actions can still use the deprecated types, but they will get deprecation warnings.  Actions doing composition and filters however will have to switch to using `SimpleResult`.

### Async actions

Previously, where you might have the following code:

```scala
def asyncAction = Action {
  Async {
    Future(someExpensiveComputation)
  }
}
```

You can now use the [`Action.async`](api/scala/index.html#play.api.mvc.ActionBuilder) builder:

```scala
def asyncAction = Action.async {
  Future(someExpensiveComputation)
}
```

### Working with chunked results

Previously the `stream` method on `Status` was used to produce chunked results.  This has been deprecated, replaced with a [`chunked`](api/scala/index.html#play.api.mvc.Results$Status) method, that makes it clear that the result is going to be chunked.  For example:

```scala
def cometAction = Action {
  Ok.chunked(Enumerator("a", "b", "c") &> Comet(callback = "parent.cometMessage"))
}
```

Advanced uses that created or used `ChunkedResult` directly should be replaced with code that manually sets/checks the `TransferEncoding: chunked` header, and uses the new `Results.chunk` and `Results.dechunk` enumeratees.

### Action composition

We are now recommending that action composition be done using [`ActionBuilder`](api/scala/index.html#play.api.mvc.ActionBuilder) implementations for building actions.

Details on how to do these can be found [[here|ScalaActionsComposition]].

### Filters

The iteratee produced by `EssentialAction` now produces `SimpleResult` instead of `Result`.  This means filters that needed to work with the result no longer have to unwrap `AsyncResult` into a `PlainResult`, arguably making all filters much simpler and easier to write.  Code that previously did the unwrapping can generally be replaced with a single iteratee `map` call.

### play.api.http.Writeable application

Previously the constructor to `SimpleResult` took a `Writeable` for the type of the `Enumerator` passed to it.  Now that enumerator must be an `Array[Byte]`, and `Writeable` is only used for the `Status` convenience methods.

### Tests

Previously `Helpers.route()` and similar methods returned a `Result`, which would always be an `AsyncResult`, and other methods on `Helpers` such as `status`, `header` and `contentAsString` took `Result` as a parameter.  Now `Future[SimpleResult]` is returned by `Helpers.route()`, and accepted by the extraction methods.  For many common use cases, where type inference is used to determine the types, no changes should be necessary to test code.

## New results structure in Java

In order to simply action composition, the Java structure of results has been changed.  `AsyncResult` has been deprecated, and `SimpleResult` has been introduced, to distinguish normal results from the `AsyncResult` type.

### Async actions

Previously, futures in async actions had to be wrapped in the `async` call.  Now actions may return either `Result` or `Promise<Result>`.  For example:

```java
public static Promise<Result> myAsyncAction() {
  Promise<Integer> promiseOfInt = Promise.promise(
    new Function0<Integer>() {
      public Integer apply() {
        return intensiveComputation();
      }
    }
  );
  return promiseOfInt.map(
    new Function<Integer, Result>() {
      public Result apply(Integer i) {
        return ok("Got result: " + i);
      }
    }
  );
}
```

### Action composition

The signature of the `call` method in `play.mvc.Action` has changed to now return `Promise<SimpleResult>`.  If nothing is done with the result, then typically the only change necessary will be to update the type signatures.

## Iteratees execution contexts

Iteratees, enumeratees and enumerators that execute application supplied code now require an implicit execution context.  For example:

```scala
import play.api.libs.concurrent.Execution.Implicits._

Iteratee.foreach[String] { msg =>
  println(msg)
}
```

## Concurrent F.Promise execution

The way that the [`F.Promise`](api/java/play/libs/F.Promise.html) class executes user-supplied code has changed in Play 2.2.

In Play 2.1, the `F.Promise` class restricted how user code was executed. Promise operations for a given HTTP request would execute in the order that they were submitted, essentially running sequentially.

With Play 2.2, this restriction on ordering has been removed so that promise operations can execute concurrently. Work executed by the `F.Promise` class now uses [[Play's default thread pool|ThreadPools]] without placing any additional restrictions on execution.

However, for those who still want it, Play 2.1's legacy behavior has been captured in the `OrderedExecutionContext` class. The legacy behavior of Play 2.1 can be easily recreated by supplying an `OrderedExecutionContext` as an argument to any of `F.Promise`'s methods.

The following code shows how to recreate Play 2.1's behaviour in Play 2.2. Note that this example uses the same settings as Play 2.1: a pool of 64 actors running within Play's default `ActorSystem`.

````java
import play.core.j.OrderedExecutionContext;
import play.libs.Akka;
import play.libs.F.*;
import scala.concurrent.ExecutionContext;

ExecutionContext orderedExecutionContext = new OrderedExecutionContext(Akka.system(), 64);
Promise<Double> pi = Promise.promise(new Function0<Double>() {
  Double apply() {
    return Math.PI;
  }
}, orderedExecutionContext);
Promise<Double> mappedPi = pi.map(new Function<Double, Double>() {
  Double apply(x Double) {
    return 2 * x;
  }
}, orderedExecutionContext);
````

## Jackson Json
We have upgraded Jackson to version 2 which means that the package name is now `com.fasterxml.jackson.core` instead of `org.codehaus.jackson`.

## Preparing a distribution

The _stage_ and _dist_ tasks have been completely re-written in Play 2.2 so that they use the [Native Packager Plugin](https://github.com/sbt/sbt-native-packager). 

Play distributions are no longer created in the project's `dist` folder. Instead, they are created in the project's `target` folder. 

Another thing that has changed is the location of the Unix script that starts a Play application. Prior to 2.2 the Unix script was named `start` and it resided in the root level folder of the distribution. With 2.2 the `start` script is named as per the project's name and it resides in the distribution's `bin` folder. In addition there is now a `.bat` script available to start the Play application on Windows.

> Please note that the format of the arguments passed to the `start` script has changed. Please issue a `-h` on the `start` script to see the arguments now accepted.

Please consult the [["Starting your application in production mode"|Production]] documentation for more information on the new `stage` and `dist` tasks.

## Upgrade from Akka 2.1 to 2.2

The migration guide for upgrading from Akka 2.1 to 2.2 can be found [here](http://doc.akka.io/docs/akka/2.2.0/project/migration-guide-2.1.x-2.2.x.html).
