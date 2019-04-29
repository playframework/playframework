<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Overview of the build system

The Play build system uses [sbt](https://www.scala-sbt.org/), a high-performance integrated build for Scala and Java projects.  Using `sbt` as our build tool brings certain requirements to play which are explained on this page.

## Understanding sbt

sbt functions quite differently to many traditional build tasks.  Fundamentally, sbt is a task engine.  Your build is represented as a tree of task dependencies that need to be executed, for example, the `compile` task depends on the `sources` task, which depends on the `sourceDirectories` task and the `sourceGenerators` task, and so on.

sbt breaks typical build executions up into very fine grained tasks, and any task at any point in the tree can be arbitrarily redefined in your build.  This makes sbt very powerful, but also requires a shift in thinking if you've come from other build tools that break your build up into very coarsely grained tasks.

The documentation here describes Play's usage of sbt at a very high level.  As you start to use sbt more in your project, it is recommended that you follow the [sbt tutorial](https://www.scala-sbt.org/0.13/tutorial/index.html) to get an understanding for how sbt fits together.  Another resource that many people have found useful is [this series of blog posts](https://jazzy.id.au/2015/03/03/sbt-task-engine.html).

## Play application directory structure

Most people get started with Play using on of our [example templates](https://playframework.com/download#examples), or with the `sbt new` command, which generally produce a directory structure like this:

- `/`: The root folder of your application
- `/README`: A text file describing your application that will get deployed with it.
- `/app`: Where your application code will be stored.
- `/build.sbt`: The [sbt](https://www.scala-sbt.org/) settings that describe building your application.
- `/conf`: Configuration files for your application
- `/project`: Further build description information
- `/public`: Where static, public assets for your application are stored.
- `/test`: Where your application's test code will be stored.

For now, we are going to concern ourselves with the `/build.sbt` file and the `/project` directory.

> **Tip**: See the complete [[anatomy of a Play application here|Anatomy]].

## The `/build.sbt` file.

An sbt build file for Play generally looks something like this:

@[default](code/build.sbt)

The `name` line defines the name of your application and it will be the same as the name of your application's root directory, `/`. In sbt this is derived from the argument that you gave to the `sbt new` command.

The `version` line provides  the version of your application which is used as part of the name for the artifacts your build will produce.

The `libraryDependencies` line specifies the libraries that your application depends on. You can see more details about [how to manage your dependencies in the sbt docs](https://www.scala-sbt.org/0.13/docs/Library-Management.html).

Finally, you need to enable an sbt plugin on your project to "Play-ify" it. This adds support for Play-specific features such as the twirl compiler and the routes compiler, and adds the necessary Play libraries to build your project and run the server. Generally you should use one of the following Play plugins for a Play application:
 - `PlayScala`: a standard Play Scala project.
 - `PlayJava`: a standard Play Java project, with the [[forms|JavaForms]] module.
 - `PlayMinimalJava`: a minimal Play Java project, without forms support.

### Using scala for building

sbt is also able to construct the build requirements from scala files inside your project's `project` folder. The recommended practice is to use `build.sbt` but there are times when using scala directly is required. If you find yourself, perhaps because you're migrating an older project, then here are a few useful imports:

```scala
import sbt._
import Keys._
import play.sbt._
import Play.autoImport._
import PlayKeys._
```

The line indicating `autoImport` is the correct means of importing an sbt plugin's automatically declared properties. Along the same lines, if you're importing an sbt-web plugin then you might well:

```scala
import com.typesafe.sbt.less.autoImport._
import LessKeys._
```

## The `/project` directory

Everything related to building your project is kept in the `/project` directory underneath your application directory.  This is an [sbt](https://www.scala-sbt.org/) requirement. Inside that directory, there are two files:

- `/project/build.properties`: This is a marker file that declares the sbt version used.
- `/project/plugins.sbt`: sbt plugins used by the project build including Play itself.

## Play plugin for sbt (`/project/plugins.sbt`)

The Play console and all of its development features like live reloading are implemented via an sbt plugin.  It is registered in the `/project/plugins.sbt` file:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % playVersion) // where version is the current Play version, i.e. "%PLAY_VERSION%"
```
> **Note**: `build.properties` and `plugins.sbt` must be manually updated when you are changing the play version.
