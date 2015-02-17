<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# SBT Cookbook

## Hook actions around `play run`

You can apply actions around the `play run` command by extending `PlayRunHook`.
This trait define the following methods:

 * `beforeStarted(): Unit`
 * `afterStarted(addr: InetSocketAddress): Unit`
 * `afterStopped(): Unit`

`beforeStarted` method is called before the play application is started, but after all "before run" tasks have been completed.

`afterStarted` method is called after the play application has been started.

`afterStopped` method is called after the play process has been stopped.

> **Note:** The following example illustrate how you can start and stop a command with play run hook.
> In the near future [sbt-web](https://github.com/sbt/sbt-web) will provide a better way to integrate Grunt with an SBT build.

Now let's say you want to build a Web application with `grunt` before the application is started.
First, you need to create a Scala object in the `project/` directory to extend `PlayRunHook`.
Let's name it `Grunt.scala`:

```scala
import play.PlayRunHook
import sbt._

object Grunt {
  def apply(base: File): PlayRunHook = {

    object GruntProcess extends PlayRunHook {

      override def beforeStarted(): Unit = {
        Process("grunt dist", base).run
      }
    }

    GruntProcess
  }
}
```

Then in the `build.sbt` file you need to register this hook:

```scala
import Grunt._
import play.PlayImport.PlayKeys.playRunHooks

playRunHooks <+= baseDirectory.map(base => Grunt(base))
```

This will execute the `grunt dist` command in `baseDirectory` before the application is started.

Now we want to execute `grunt watch` command to observe changes and rebuild the Web application when that happen:

```scala
import play.PlayRunHook
import sbt._

import java.net.InetSocketAddress

object Grunt {
  def apply(base: File): PlayRunHook = {

    object GruntProcess extends PlayRunHook {

      var process: Option[Process] = None

      override def beforeStarted(): Unit = {
        Process("grunt dist", base).run
      }

      override def afterStarted(addr: InetSocketAddress): Unit = {
        process = Some(Process("grunt watch", base).run)
      }

      override def afterStopped(): Unit = {
        process.map(p => p.destroy())
        process = None
      }
    }

    GruntProcess
  }
}
```

Once the application has been started we execute `grunt watch` and when the application has been stopped we destroy the grunt process. There's nothing to change in `build.sbt`

## Add compiler options

For example, you may want to add the feature flag to have details on feature warnings:

```
[info] Compiling 1 Scala source to ~/target/scala-2.10/classes...
[warn] there were 1 feature warnings; re-run with -feature for details
```

Simply add `-feature` to the `scalacOptions` attribute:

```scala
scalacOptions += "-feature"
```

## Add additional asset directory

For example you can add the `pictures` folder to be included as an additional asset directory:

```scala
unmanagedResourceDirectories in Assets <+= baseDirectory { _ / "pictures" }
```

This will allow you to use `routes.Assets.at` with this folder.

## Disable documentation

To speed up compilation you can disable documentation generation:

```scala
sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false
```

The first line will disable documentation generation and the second one will avoid to publish the documentation artifact.

## Configure ivy logging level

By default `ivyLoggingLevel` is set on `UpdateLogging.DownloadOnly`. You can change this value with:

 * `UpdateLogging.Quiet` only displays errors
 * `UpdateLogging.FULL` logs the most

For example if you want to only display errors:

```scala
ivyLoggingLevel := UpdateLogging.Quiet
```

## Fork and parallel execution in test

By default parallel execution is disable and fork is enable. You can change this behavior by setting `parallelExecution in Test` and/or `fork in Test`:

```scala
parallelExecution in Test := true

fork in Test := false
```
