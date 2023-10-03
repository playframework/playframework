<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Scala 3 Migration Guide

This guide is for migrating a Play application from Scala 2 to Scala 3 and requires that your application is already running on at least [[Play 2.9|Highlights29]] (built on Akka / Akka HTTP) or [Play 3.0](https://www.playframework.com/documentation/latest/Highlights30) (built on Pekko / Pekko HTTP).

> Depending on your codebase, migrating existing Play applications to Scala 3 can be a substantial task. We strongly recommend that you initially [[migrate|Migration29]] to Play 2.9 or 3.0 while staying on Scala 2.13. This approach ensures that everything functions as intended. Afterward, you can make the transition to Scala 3.

## General

In addition to the migration steps mentioned on this page, which cover the migration steps needed to move a Play Framework application to Scala 3, there are general Scala 3 resources that you want to follow to migrate your code base to Scala 3:

- The [Scala 3 Migration Guide](https://docs.scala-lang.org/scala3/guides/migration/compatibility-intro.html)
- The [Scala 3 Language Reference](https://docs.scala-lang.org/scala3/reference/)

## Setting `scalaVersion` in your project

**Both Scala and Java users** must configure sbt to use Scala 3. Even if you have no Scala code in your project, Play itself uses Scala and must be configured to use the right Scala libraries.

To set the Scala version in sbt, simply set the `scalaVersion` key, for example:

```scala
scalaVersion := "3.3.1"
```

> It's important to emphasize that Play exclusively supports [Scala LTS (Long-Term Support)](https://www.scala-lang.org/blog/2022/08/17/long-term-compatibility-plans.html) versions. As a result, any Scala release between Scala 3.3 LTS and the subsequent LTS version will not be officially supported by Play. However, it might still be feasible to use Play with such Scala versions. You may be interested in "[The Scala 3 compatibility story](https://virtuslab.com/blog/the-scala-3-compatibility-story/)" and the [Scala 3.3 release blog post](https://scala-lang.org/blog/2023/05/30/scala-3.3.0-released.html).

## Using Scala 3 with Akka HTTP 10.5 or newer

> This section only applies to Play 2.x, but not Play 3.x.

As mentioned in the Play 2.9 Highlights, [[Play 2.9 keeps shipping Akka 2.6 and Akka HTTP 10.2|Highlights29#Akka-HTTP-10.2]], despite newer versions being available.

Akka HTTP 10.2, however, does not provide Scala 3 artifacts; only Akka HTTP 10.5 introduced them. If you wish to make use of these native Scala 3 artifacts with Akka HTTP and therefore want to upgrade to Akka HTTP 10.5 or newer, you can do that with the assistance of our [Play Scala](https://www.playframework.com/documentation/2.9.x/ScalaAkka#Updating-Akka-version) or [Play Java](https://www.playframework.com/documentation/2.9.x/JavaAkka#Updating-Akka-version) update guides, which also provide notes on which settings to adjust to use Akka HTTP 10.5 or newer with Scala 3. We also strongly encourage you to review:

- [[How Play Deals with Akka’s License Change|General#How-Play-Deals-with-Akkas-License-Change]]

## `running()` wrapper required for certain tests

### Using specs2

specs2's [`Around` trait](https://github.com/etorreborre/specs2/blob/SPECS2-4.20.2/core/shared/src/main/scala/org/specs2/mutable/Around.scala) makes use of Scala's [`DelayedInit`](https://www.scala-lang.org/api/current/scala/DelayedInit.html), which [has been dropped in Scala 3](https://docs.scala-lang.org/scala3/reference/dropped-features/delayed-init.html) (actually not removed, it just isn't doing anything anymore). Unfortunately, there is no replacement for `DelayedInit` in Scala 3. Therefore, when [[writing tests with specs2|ScalaFunctionalTestingWithSpecs2]] that make use of:

- [`WithServer`](api/scala/play/api/test/WithServer.html)
- [`WithApplication`](api/scala/play/api/test/WithApplication.html)
- [`WithApplicationLoader`](api/scala/play/api/test/WithApplicationLoader.html)
- [`WithBrowser`](api/scala/play/api/test/WithBrowser.html)

we had to come up with a solution to not make users completely refactor their tests. The solution we came up with is to wrap tests within a `running()` method, as we think this is the simplest way to migrate those tests without making migration too costly. As an example, for `WithApplication`, code like:

```scala
"testing some logic" in new WithApplication {
  // <test code>
}
```

now needs to be written like:

```scala
"testing some logic" in new WithApplication {
  override def running() = {
    // <test code>
  }
}
```

An advantage of using a wrapper method is that we made it work in Scala 2 as well, so Scala 2 tests will even run if you wrap them in `running()`, so such test code can be easily switched forth/back from/to Scala 2/3, and tests will work in both cases (which may help during migration).

### Using ScalaTest _Plus_ Play

When [[writing tests with ScalaTest|ScalaFunctionalTestingWithScalaTest]] and making use of [App](https://www.playframework.com/documentation/latest/api/scala/org/scalatestplus/play/MixedFixtures$App.html), [Server](https://www.playframework.com/documentation/latest/api/scala/org/scalatestplus/play/MixedFixtures$Server.html), [Chrome](https://www.playframework.com/documentation/latest/api/scala/org/scalatestplus/play/MixedFixtures$Chrome.html), [Firefox](https://www.playframework.com/documentation/latest/api/scala/org/scalatestplus/play/MixedFixtures$Firefox.html), [HtmlUnit](https://www.playframework.com/documentation/latest/api/scala/org/scalatestplus/play/MixedFixtures$HtmlUnit.html), [InternetExplorer](https://www.playframework.com/documentation/latest/api/scala/org/scalatestplus/play/MixedFixtures$InternetExplorer.html), or [Safari](https://www.playframework.com/documentation/latest/api/scala/org/scalatestplus/play/MixedFixtures$Safari.html), you need to wrap the test code within a `running()` method to make them work with Scala 3. That is because under the hood, [ScalaTest also makes use of `DelayedInit`](https://github.com/scalatest/scalatest/pull/2228), just like described in the previous section. As an example, when using `App`, code like:

```scala
"testing some logic" in new App(...) {
 // <test code>
}
```

needs to be converted to:

```scala
"testing some logic" in new App(...) {
  override def running() = {
    // <test code>
  }
}
```

## String Interpolating Routing DSL (sird) imports

We had to split some methods out of the [implicit class](https://docs.scala-lang.org/overviews/core/implicit-classes.html) `play.api.routing.sird.UrlContext`, using [extension methods](https://docs.scala-lang.org/scala3/book/ca-extension-methods.html) instead.
For you, that means if you imported the `UrlContext` class directly:

```scala
import play.api.routing.sird.UrlContext
```

you now have to instead import everything from the `sird` package to make sure the extension methods are imported as well:

```scala
import play.api.routing.sird._
```

## Dependency graph changes

If you start to use Scala 3 in your Play application, the Play `specs2` [[dependency|ScalaTestingWithSpecs2#Using-specs2]] will no longer pull in the [`"org.specs2" %% "specs2-mock"` dependency](https://mvnrepository.com/artifact/org.specs2/specs2-mock) because it is not available for Scala 3 anymore. The Play `specs2` Scala 3 artifacts depend on [`"org.mockito" % "mockito-core"`](https://mvnrepository.com/artifact/org.mockito/mockito-core) instead to use [Mockito](https://github.com/mockito/mockito) directly, which we think is the best alternative to switch your existing test code to at the time of this writing.
You need to adjust your code, e.g., here is a snippet of how to use Mockito:

```scala
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

val userRepository = mock(classOf[UserRepository])
when(userRepository.roles(any[User])).thenReturn(Set(Role("ADMIN")))
```
