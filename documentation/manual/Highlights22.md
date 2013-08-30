# What's new in Play 2.2

## New results structure for Java and Scala

*TODO*

## Better control over buffering and keep alive

*TODO*

## New action composition and action builder methods

We now provide an [`ActionBuilder`](api/scala/index.html#play.api.mvc.ActionBuilder) trait that allows more powerful building of action stacks.  For example:

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

*TODO*

## Iteratee execution context passing

*TODO*

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
