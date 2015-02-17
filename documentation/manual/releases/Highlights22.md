<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.2

## New results structure for Java and Scala

Previously, results could be either plain or async, chunked or simple.  Having to deal with all these different types made action composition and filters hard to implement, since often there was functionality that needed to be applied to all types of results, but code had to implemented to recursively unwrap asynchronous results and to apply the same logic to chunked and simple results.

It also created an artificial distinction between asynchronous and synchronous actions in Play, which caused confusion, leading people to think that Play could operate in a synchronous and asynchronous modes.  In fact, Play is 100% asynchronous, the only thing that differentiates whether a result is returned asynchronously or not is whether other asynchronous actions, such as IO, need to be done during action processing.

So we've simplified the structure for results in Java and Scala.  There is now only one result type, `SimpleResult`.  The `Result` superclass still works in many places but is deprecated.

In Java applications, this means actions can now just return `Promise<SimpleResult>` if they wish to do asynchronous processing during a request, while Scala applications can use the `async` action builder, like this:

```scala
def index = Action.async {
  val foo: Future[Foo] = getFoo()
  foo.map(f => Ok(f))
}
```

## Better control over buffering and keep alive

How and when Play buffers results is now better expressed in the Scala API, [`SimpleResult`](api/scala/index.html#play.api.mvc.SimpleResult) has a new property called `connection`, which is of type [`HttpConnection`](api/scala/index.html#play.api.mvc.HttpConnection$).

If set to `Close`, the response will be closed once the body is sent, and no buffering will be attempted.  If set to `KeepAlive`, Play will make a best effort attempt to keep the connection alive, in accordance to the HTTP spec, buffering the response if only no transfer encoding or content length is specified.

## New action composition and action builder methods

We now provide an [`ActionBuilder`](api/scala/index.html#play.api.mvc.ActionBuilder) trait for Scala applications that allows more powerful building of action stacks.  For example:

```scala
object MyAction extends ActionBuilder[AuthenticatedRequest] {
  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
    // Authenticate the action and wrap the request in an authenticated request
    getUserFromRequest(request).map { user =>
      block(new AuthenticatedRequest(user, request))
    } getOrElse Future.successful(Forbidden)
  }

  // Compose the action with a logging action, a CSRF checking action, and an action that only allows HTTPS
  def composeAction[A](action: Action[A]) =
    LoggingAction(CheckCSRF(OnlyHttpsAction(action)))
}
```

The resulting action builder can be used just like the built in `Action` object, with optional parser and request parameters, and async variants.  The type of the request parameter passed to the action will be the type specified by the builder, in the above case, `AuthenticatedRequest`:

```scala
def save(id: String) MyAction(parse.formUrlEncoded) = { request =>
  Ok("User " + request.user + " saved " + request.body)
}
```

## Improved Java promise API

The Java Promise class has been improved in order to bring its functionality closer to Scala's Future and Promise. In particular execution contexts can now be passed into a Promise's methods.

## Iteratee library execution context passing

Execution contexts are now required when calling on methods of Iteratee, Enumeratee and Enumerator. Having execution contexts exposed for the Iteratee library provides finer-grained control over where execution occurs and can lead to performance improvements in some cases.

Execution contexts can be supplied implicitly which means that there is little impact on the code that uses Iteratees.

## sbt 0.13 support

There have been various usability and performance improvements. 

One usability improvement is that we now support `build.sbt` files for building Play projects e.g. `samples/java/helloworld/build.sbt`:

```scala
import play.Project._

name := "helloworld"

version := "1.0"

playJavaSettings
```

The `playJavaSettings` now declares all that is required for a Java project. Similarly `playScalaSettings` exists for Play Scala projects. Check out the sample projects for examples of this new build configuration. Note that the previous method of using build.scala along with `play.Project` is still supported.

For more information on what has changed for sbt 0.13 please refer to its [release notes](http://www.scala-sbt.org/0.13.0/docs/Community/ChangeSummary_0.13.0.html)

## New stage and dist tasks

The _stage_ and _dist_ tasks have been completely overhauled in order to use the [Native Packager Plugin](https://github.com/sbt/sbt-native-packager).

The benefit in using the Native Packager is that many types of archive can now be supported in addition to regular zip files e.g. tar.gz, RPM, OS X disk images, Microsoft Installers (MSI) and more. In addition a Windows batch script is now provided for Play as well as a Unix one.

More information can be found in the [[Creating a standalone version of your application|ProductionDist]] document.

## Built in gzip support

Play now has built in support for gzipping all responses.  For information on how to enable this, see [[Configuring gzip encoding|GzipEncoding]].

## Documentation JAR

Play's distribution now stores its documentation in a JAR file rather than in a directory. A JAR file provides better support for tooling.

Just like in Play 2.1, you can view the documentation when you [[run your Play application in development mode|PlayConsole]] by visiting the special [`/@documentation`](http://localhost:9000/@documentation) address.

If you want to access the raw files, they can now be found in the `play-docs` JAR file contained in the distribution.
