This is a backport of Play 2.1.x to Scala 2.9.x.  It fills the gap for users that want to use Scala 2.9.x later than 2.9.1 with Play.

The minimum Scala version required for this backport is Scala 2.9.3, since it contains SIP-14 futures which are heavily used by Play 2.1.

The definition of the backport is a set of commits that are applied to each release of Play 2.1.x.  As such, no work should be done on this branch for 2.1.x, only work for the purposes of maintaining the backport itself.  At each release of Play 2.1.x, this branch will be rebased to the tag of that release, integration tests run, and if they pass, this branch will be published.

To release this branch, you can run the following from the framework directory:

    ./build -Dplay.version=<play version here> -Dscala.version=<scala version to build and publish against> clean publish

The publish of this branch only publishes the Play runtime artifacts that pertain to a particular Scala version.  It does not publish the SBT plugins or the artifacts that are not Scala version specific.  The way this is implemented is that the publishing of those artifacts is actually done to a dummy local repository.

To use this backport, simply add the following to your build settings:

    scalaVersion := "2.9.3"

There are a few small differences between this backport and the 2.10 version of Play 2.1.1:

* There are no JSON macros, since macros are not supported in Scala 2.9.x
* Akka 2.0.x is used, since Akka 2.1.x only supports Scala 2.10.x
* When using Akka futures, a bridge, `Akka.asScalaFuture` needs to be used to convert these to Scala futures that can be returned by a Play asynchronous action.
