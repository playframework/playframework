<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# The Build System

The Play build system uses [sbt](http://www.scala-sbt.org/), a high-performance integrated build for Scala and Java projects.  Using `sbt` as our build tool brings certain requirements to play which are explained on this page.

## Play application directory structure 

Most people get started with Play using the `activator new foo` command which produces a directory structure like this:

- `/`: The root folder of your application
- `/README`: A text file describing your application that will get deployed with it.
- `/app`: Where your application code will be stored.
- `/build.sbt`: The [sbt](http://www.scala-sbt.org/) settings that describe building your application.
- `/conf`: Configuration files for your application
- `/project`: Further build description information
- `/public`: Where static, public assets for your application are stored.
- `/test`: Where your application's test code will be stored.

For now, we are going to concern ourselves with the `/build.sbt` file and the `/project` directory.

## The `/build.sbt` file. 

When you use the `activator new foo` command, the build description file, `/build.sbt`, will be generated like this:

@[default](code/build.sbt)

The `name` line defines the name of your application and it will be the same as the name of your application's root directory, `/`, which is derived from the argument that you gave to the `activator new` command. 

The `version` line provides  the version of your application which is used as part of the name for the artifacts your build will produce.

The `libraryDependencies` line specifies the libraries that your application depends on. More on this below.

You should use the `PlayJava` or `PlayScala` plugin to configure sbt for Java or Scala respectively.

## Using scala for building

Activator is also able to construct the build requirements from scala files inside your project's `project` folder. The recommended practice is to use `build.sbt` but there are times when using scala directly is required. If you find yourself, perhaps because you're migrating an older project, then here are a few useful imports:

```scala
import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
```

The line indicating `autoImport` is the correct means of importing an sbt plugin's automatically declared properties. Along the same lines, if you're importing an sbt-web plugin then you might well:

```scala
import com.typesafe.sbt.less.autoImport._
import LessKeys._
```

## The `/project` directory

Everything related to building your project is kept in the `/project` directory underneath your application directory.  This is an [sbt](http://www.scala-sbt.org/) requirement. Inside that directory, there are two files:

- `/project/build.properties`: This is a marker file that declares the sbt version used.
- `/project/plugins.sbt`: SBT plugins used by the project build including Play itself.

## Play plugin for sbt (`/project/plugins.sbt`)

The Play console and all of its development features like live reloading are implemented via an sbt plugin.  It is registered in the `/project/plugins.sbt` file:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % playVersion) // where version is the current Play version, i.e.  "2.3.0" 
```
> Note that `build.properties` and `plugins.sbt` must be manually updated when you are changing the play version.

## Adding dependencies and resolvers

Adding dependencies is simple as the build file for the `zentasks` Java sample shows:

```scala
name := "zentask"

version := "1.0"

libraryDependencies ++= Seq(javaJdbc, javaEbean)     

lazy val root = (project in file(".")).enablePlugins(PlayJava)
```

...and so are resolvers for adding in additional repositories:

```scala
resolvers += "Repository name" at "https://url.to/repository" 
```

