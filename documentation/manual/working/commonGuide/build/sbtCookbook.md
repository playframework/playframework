<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# sbt Cookbook

## Hooking into Play's dev mode

When Play runs in dev mode, that is, when using `sbt run`, it's often useful to hook into this to start up additional processes that are required for development.  This can be done by defining a `PlayRunHook`, which is a trait with the following methods:

 * `beforeStarted(): Unit` - called before the play application is started, but after all "before run" tasks have been completed.
 * `afterStarted(addr: InetSocketAddress): Unit` - called after the play application has been started.
 * `afterStopped(): Unit` - called after the play process has been stopped.

Now let's say you want to build a Web application with `grunt` before the application is started.  First, you need to create a Scala object in the `project/` directory to extend `PlayRunHook`.  Let's name it `Grunt.scala`:

@[grunt-before-started](code/runhook.sbt)

Now you can register this hook in `build.sbt`:

@[grunt-build-sbt](code/runhook.sbt)

This will execute the `grunt dist` command in `baseDirectory` before the application is started whenever you run `sbt run`.

Now we want to modify our run hook to execute the `grunt watch` command to observe changes and rebuild the Web application when they happen, so we'll modify the `Grunt.scala` file we created before:

@[grunt-watch](code/runhook.sbt)

Now when the application is started using `sbt run`, `grunt watch` will be executed to rerun the grunt build whenever files change.

## Add compiler options

For example, you may want to add the feature flag to have details on feature warnings:

```
[info] Compiling 1 Scala source to ~/target/scala-2.10/classes...
[warn] there were 1 feature warnings; re-run with -feature for details
```

Simply add `-feature` to the `scalacOptions` attribute:

@[compiler-options](code/cookbook.sbt)

## Add additional asset directory

For example you can add the `pictures` folder to be included as an additional asset directory:

@[add-assets](code/cookbook.sbt)

This will allow you to use `routes.Assets.at` with this folder.

## Disable documentation

To speed up compilation you can disable documentation generation:

@[disable-scaladoc](code/cookbook.sbt)

The first line will disable documentation generation and the second one will avoid to publish the documentation artifact.

## Configure ivy logging level

By default `ivyLoggingLevel` is set on `UpdateLogging.DownloadOnly`. You can change this value with:

 * `UpdateLogging.Quiet` only displays errors
 * `UpdateLogging.Full` logs the most

For example if you want to only display errors:

@[ivy-logging](code/cookbook.sbt)

## Fork and parallel execution in test

By default parallel execution is disabled and fork is enabled. You can change this behavior by setting `parallelExecution in Test` and/or `fork in Test`:

@[fork-parallel-test](code/cookbook.sbt)
