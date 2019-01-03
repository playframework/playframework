<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# SBT Cookbook

## Hooking into Play's dev mode

When Play runs in dev mode, that is, when using `sbt run`, it's often useful to hook into this to start up additional processes that are required for development.  This can be done by defining a `PlayRunHook`, which is a trait with the following methods:

 * `beforeStarted(): Unit` - called before the play application is started, but after all "before run" tasks have been completed.
 * `afterStarted(): Unit` - called after the play application has been started.
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

## Configure externalized resources

Since Play 2.4 the content of the `conf` directory is added to the classpath by default.
When [[packaging a Play application for production|Deploying]] that `conf` folder (or it's content) can exist in two places:
Either the `conf` folder will *not* be packaged into a `jar` file (together with the rest of the application) but instead stays *outside* that `jar` file on the file system. Therefore the content of that `conf` folder (e.g. `application.conf`) can be edited and by restarting the application any changes will be picked up immediately - there is no need to repackage and redeploy an application. This is the default.
Or Play can be configured to always put the content of the `conf` folder *inside* the application `jar` file. This can be done by setting
```
PlayKeys.externalizeResources := false
```
in `build.sbt`. Sometimes this behaviour is needed for cases where a library requires (some) resources of the `conf` folder and the application class files to live inside the *same* `jar` file in order to work correctly.

Since Play 2.7 another configuration key exists that allows you to exclude specific resources so they won't be externalized, even though when `PlayKeys.externalizeResources := true`:
```
PlayKeys.externalizeResourcesExcludes += baseDirectory.value / "conf" / "somefolder" / "somefile.xml"
```
Thanks to this configuration key you don't have to put all the files of the `conf` folder into the `jar` file anymore when that is needed only for some specific files.

## Disable documentation

To speed up compilation you can disable documentation generation:

@[disable-scaladoc](code/cookbook.sbt)

The first line will disable documentation generation and the second one will avoid to publish the documentation artifact.

## Configure ivy logging level

By default `ivyLoggingLevel` is set on `UpdateLogging.DownloadOnly`. You can change this value with:

 * `UpdateLogging.Quiet` only displays errors
 * `UpdateLogging.FULL` logs the most

For example if you want to only display errors:

@[ivy-logging](code/cookbook.sbt)

## Fork and parallel execution in test

By default parallel execution is disabled and fork is enabled. You can change this behavior by setting `parallelExecution in Test` and/or `fork in Test`:

@[fork-parallel-test](code/cookbook.sbt)
