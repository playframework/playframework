# Creating a new application

## Create a new application using Typesafe Activator

If you installed Play Framework using [Typesafe Activator](http://typesafe.com/activator), then follow the instructions [here](http://typesafe.com/platform/getstarted) on how to create a new application.

## Create a new application with the Play standalone distribution

The Play standalone distribution comes with a script called `play`, which can easily be used to create a new application.

```bash
$ play new myFirstApp
```

This will ask for some information.

- The application name (just for display, this name will be used later in several messages).
- The template to use for this application. You can choose either a default Scala application or a default Java application.

[[images/playNew.png]]

> Note that choosing a template at this point does not imply that you can’t change language later. For example, you can create a new application using the default Java application template and start adding Scala code whenever you like.

Once the application has been created you can use the `play` command again to enter the [[Play console | PlayConsole]].

```bash
$ cd myFirstApp
$ play
```

## Create a new application without having Play installed

You can also create a new Play application without installing Play, by using sbt. 

> First install [sbt](http://www.scala-sbt.org/) if needed.

Just create a new directory for your new application and configure your sbt build script with two additions.

In `project/plugins.sbt`, add:

```scala
// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "%PLAY_VERSION%")
```

Be sure to replace %PLAY_VERSION% here with the exact version you want to use. If you want to use a snapshot version, you will have to specify this additional resolver: 

```
// Typesafe snapshots
resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
```

In `build.sbt`:

```scala
import play.Project._

name := "My first application"

version := "1.0"

playScalaSettings
```

You can then launch the sbt console in this directory:

```bash
$ cd myFirstApp
$ sbt
```

sbt will load your project and fetch the dependencies.

> **Next:** [[Anatomy of a Play application | Anatomy]]
