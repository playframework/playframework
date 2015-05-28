<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.4

This page highlights the new features of Play 2.4. If you want learn about the changes you need to make to migrate to Play 2.4, check out the [[Play 2.4 Migration Guide|Migration24]].

## Dependency Injection

Play now supports dependency injection out of the box.

### Motivation

A long term strategy for Play is to remove Play's dependence on global state.  Play currently stores a reference to the current application in a static variable, and then uses this variable in many places throughout its codebase.  Removing this has the following advantages:

* Applications become easier to test and components become easier to mock.
* More interesting deployment scenarios are possible, such as multiple Play instances in a single JVM, or embedding a lightweight Play application.
* The application lifecycle becomes easier to follow and reason about.

Removing Play's global state is however a big task that will require some disruptive changes to the way Play applications are written.  The approach we are taking to do this is to do as much as possible in Play 2.4 while maintaining backwards compatibility.  For a time, many of Play's APIs will support both methods that rely on require global state and methods that don't rely on global state, allowing you to migrate your application to not depend on global state incrementally, rather than all at once when you uprgade to Play 2.4.

The first step to removing global state is to make it such that Play components have their dependencies provided to them, rather than looking them up statically.  This means providing out of the box support for dependency injection.

### Approach

In the Java ecosystem, the approach to dependency injection is generally well agreed upon in [JSR 330](https://jcp.org/en/jsr/detail?id=330), but the right implementation is widely debated, with many existing competing implementations such as Guice, Spring and JEE itself.

In the Scala ecosystem, the approach to dependency injection is not generally agreed upon, with many competing compile time and runtime dependency injection approaches out there.

Play's philosophy in providing a dependency injection solution is to be unopinionated in what approaches we allow, but to be opinionated to the approach that we document and provide out of the box.  For this reason, we have provided the following:

* An implementation that uses [Guice](https://github.com/google/guice) out of the box
* An abstraction that allows other JSR 330 implementations to be plugged in
* All Play components can be instantiated using plain constructors or factory methods
* Traits that instantiate Play components that can be mixed together in a cake pattern like style to assist with compile time dependency injection

You can read more about Play's dependency injection support for [[Java|JavaDependencyInjection]] and [[Scala|ScalaDependencyInjection]].

## Testing

One of the biggest advantages of introducing dependency injection to Play is that many parts of Play can now be much easier to test.  Play now provides a number of APIs to assist in mocking and overriding components, as well as being able to test interactions with Play components in isolation from the rest of your Play application.

You can read about these new APIs here:

* Configuring Guice components in [[Java|JavaTestingWithGuice]] and [[Scala|ScalaTestingWithGuice]]
* Testing database access code in [[Java|JavaTestingWithDatabases]] and [[Scala|ScalaTestingWithDatabases]]
* Testing web service client code in [[Java|JavaTestingWebServiceClients]] and [[Scala|ScalaTestingWebServiceClients]]

## Embedding Play

It is now straightforward to embed a Play application.  Play 2.4 provides both APIs to start and stop a Play server, as well as routing DSLs for Java and Scala so that routes can be embedded directly in code.

In Java, see [[Embedding Play|JavaEmbeddingPlay]] as well as information about the [[Routing DSL|JavaRoutingDsl]].

In Scala, see [[Embedding Play|ScalaEmbeddingPlay]] as well as information about the [[String Interpolating Routing DSL|ScalaSirdRouter]].

## Aggregated reverse routers

Play now supports aggregating reverse routers from multiple sub projects into a single shared project, with no dependency on the project the routes files came from.  This allows a modular Play application to use the Play reverse router as an API between modules, allowing them to render URLs to each other without depending on each other.  It also means a dependency free reverse router could be extracted out of a Play project, and published, for use by external projects that invoke the APIs provided by the project.

For details on how to configure this, see [[Aggregating Reverse Routers|AggregatingReverseRouters]].

## Java 8 support

Play 2.4 now requires JDK 8.  Due to this, Play can, out of the box, provide support for Java 8 data types.  For example, Play's JSON APIs now support Java 8 temporal types including `Instance`, `LocalDateTime` and `LocalDate`.

The Play documentation now shows code examples using Java 8 syntax for anonymous inner classes. As an example, here's how some of the code samples have changed:

Before:

```java
return promise(new Function0<Integer>() {
  public Integer apply() {
    return longComputation();
  }
 }).map(new Function<Integer,Result>() {
  public Result apply(Integer i) {
    return ok("Got " + i);
  }
});
```

After:

```java
return promise(() -> longComputation())
  .map((Integer i) -> ok("Got " + i));
```

## Maven/sbt standard layout

Play will now let you use either its default layout or the directory layout that is the default for Maven and SBT projects. See the [[Anatomy of a Play application|Anatomy]] page for more details.

## Anorm

Anorm has been extracted into a separate project with its own lifecycle, allowing anorm to move at its own pace, not bound to Play.  The anorm project can be found [here](https://github.com/playframework/anorm).

New features in anorm include:

- New positional getter on `Row`.
- Unified column resolution by label, whatever it is (name or alias).
- New streaming API; Functions `fold` and `foldWhile` to work with result stream (e.g. `SQL"Select count(*) as c from Country".fold(0l) { (c, _) => c + 1 }`). Function `withResult` to provide custom stream parser (e.g. `SQL("Select name from Books").withResult(customTailrecParser(_, List.empty[String]))`).
- Supports array (`java.sql.Array`) from column (e.g. `SQL("SELECT str_arr FROM tbl").as(scalar[Array[String]].*)`) or as parameter (e.g. `SQL"""UPDATE Test SET langs = ${Array("fr", "en", "ja")}""".execute()`).
- Improved conversions for numeric and boolean columns.
- New conversions for binary columns (bytes, stream, blob), to parsed them as `Array[Byte]` or `InputStream`.
- New conversions for Joda `Instant` or `DateTime`, from `Long`, `Date` or `Timestamp` column.
- Added conversions to support `List[T]`, `Set[T]`, `SortedSet[T]`, `Stream[T]` and `Vector[T]` as multi-value parameter.
- New conversion to parse text column as UUID (e.g. `SQL("SELECT uuid_as_text").as(scalar[java.util.UUID].single)`).

## Ebean

Play's Ebean support has been extracted into a separate project with its own lifecycle, allowing Ebean support to move at its own pace, not bound to Play.  The play-ebean project can be found [here](https://github.com/playframework/play-ebean).

play-ebean now supports Ebean 4.x.

## HikariCP

[HikariCP](http://brettwooldridge.github.io/HikariCP/) is now the default JDBC connection pool. Its properties can be directly configured using `.conf` files and you should rename the configuration properties to match what is expected by HikariCP.

## WS

WS now supports Server Name Indication (SNI) in HTTPS -- this solves a number of problems with HTTPS based CDNs such as Cloudflare which depend heavily on SNI.

## Experimental Features

Play provides two new experimental features.  These are labelled as experimental because the APIs for them have not yet been finalised, and may change from one release to the next.  Binary compatibility is not guaranteed on these APIs.

### Akka HTTP support

Play supports a new Akka HTTP backend, as an alternative to the current Netty backend.  For instructions on using it, see [[Akka Http Server|AkkaHttpServer]].

### Reactive Streams Support

Play provides an iteratees based implementation of [Reactive Streams](http://www.reactive-streams.org/), allowing other Reactive Streams implementations, such as Akka Streams or RxJava, to be used with Play's iteratee IO APIs.  For more information, see [[Reactive Streams Integration|ReactiveStreamsIntegration]].
