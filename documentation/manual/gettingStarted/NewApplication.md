# Creating a new application

## Create a new application with the play command

The easiest way to create a new application is to use the `play new` command.

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

> First install [[sbt| http://www.scala-sbt.org/]] if needed.

Just create a new directory for your new application and configure your sbt build script with two additions.

In `project/plugins.sbt`, add:

```scala
// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.0")
```

Be sure to replace `2.1.0` here by the exact version you want to use. If you want to use a snapshot version, you will have to specify this additional resolver: 

```
// Typesafe snapshots
resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
```

In `project/Build.scala`:

```scala
import sbt._
import Keys._
import play.Project._
 
object ApplicationBuild extends Build {
 
  val appName         = "My first application"
  val appVersion      = "1.0"
 
  val appDependencies = Nil
 
  val main = play.Project(
    appName, appVersion, appDependencies
  ) 
 
}
```

You can then launch the sbt console in this directory:

```bash
$ cd myFirstApp
$ sbt
```

sbt will load your project and fetch the dependencies.

> **Next:** [[Anatomy of a Play application | Anatomy]]