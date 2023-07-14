<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Working with sub-projects

A complex project is not necessarily composed of a single Play application. You may want to split a large project into several smaller applications, or even extract some logic into a standard Java or Scala library that has nothing to do with a Play application.

It will be helpful to read the [sbt documentation on multi-project builds](https://www.scala-sbt.org/release/docs/Getting-Started/Multi-Project).  Sub-projects can be fully defined in the parent project's build file, although here we put sub-projects' settings in their own build file. 

## Adding a simple library sub-project

You can make your application depend on a simple library project. Just add another sbt project definition in your `build.sbt` file:

```
name := "my-first-application"

version := "1.0"

lazy val myFirstApplication = (project in file("."))
    .enablePlugins(PlayScala)
    .aggregate(myLibrary)
    .dependsOn(myLibrary)

lazy val myLibrary = project
```

The lowercased `project` on the last line is a Scala Macro which will use the name of the val it is being assigned to in order to determine the project's name and folder.

The `myFirstApplication` project declares the base project.  If you don't have any sub projects, this is already implied, however when declaring sub projects, it's usually required to declare it so that you can ensure that it aggregates (that is, runs things like compile/test etc on the sub projects when run in the base project) and depends on (that is, adds the sub projects to the main projects classpath) the sub projects.

The above example defines a sub-project in the application’s `myLibrary` folder. This sub-project is a standard sbt project, using the default layout:

```
myProject
 └ build.sbt
 └ app
 └ conf
 └ public
 └ myLibrary
   └ build.sbt
   └ src
     └ main
       └ java
       └ scala
```

`myLibrary` has its own `build.sbt` file, this is where it can declare its own settings, dependencies etc.

When you have a sub-project enabled in your build, you can focus on this project and compile, test or run it individually. Just use the `projects` command in the Play console prompt to display all projects:

```
[my-first-application] $ projects
[info] In file:/Volumes/Data/gbo/myFirstApp/
[info] 	 * my-first-application
[info] 	   my-library
```

The default project is the one whose variable name comes first alphabetically.  You may make your main project by making its variable name aaaMain.  To change the current project use the `project` command:

```
[my-first-application] $ project my-library
[info] Set current project to my-library
>
```

When you run your Play application in dev mode, the dependent projects are automatically recompiled, and if something cannot compile you will see the result in your browser:

[[subprojectError.png]]

## Sharing common variables and code

If you want your sub projects and root projects to share some common settings or code, then these can be placed in a Scala file in the `project` directory of the root project.  For example, in `project/Common.scala` you might have:

```scala
import sbt._
import Keys._

object Common {
  val settings: Seq[Setting[_]] = Seq(
    organization := "com.example",
    version := "1.2.3-SNAPSHOT"
  )

  val fooDependency = "com.foo" %% "foo" % "2.4"
}
```

Then in each of your `build.sbt` files, you can reference anything declared in the file:

```scala
name := "my-sub-module"

Common.settings

libraryDependencies += Common.fooDependency
```

One thing to note is that if you have a mix of Play and non-Play projects, you may need to share Play configuration explicitly.  For example, you may want to share the `InjectedRoutesGenerator` and specs2 for every Play project:

```scala
object Common {

  val playSettings = settings ++ Seq(
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies += specs2 % Test,
    resolvers += Resolver.sonatypeRepo("snapshots") // contains akka(-http) snapshots
  )
}
```

And in the sub-project's `build.sbt` file, you would have the following:

```scala
Common.playSettings
```

## Splitting your web application into several parts

As a Play application is just a standard sbt project with a default configuration, it can depend on another Play application.  You can make any sub module a Play application by adding the `PlayJava` or `PlayScala` plugins, depending on whether your project is a Java or Scala project, in its corresponding `build.sbt` file.

> **Note:** In order to avoid naming collision, make sure your controllers, including the Assets controller in your subprojects are using a different name space than the main project.  For example, controllers in the `admin` module should have the fully qualified package name of `admin.MyController`.

## Splitting the route file

It's also possible to split the route file into smaller pieces. This is a very handy feature if you want to create a robust, reusable multi-module play application.

### Consider the following build configuration

`build.sbt`:

```scala
name := "myproject"

lazy val admin = (project in file("modules/admin")).enablePlugins(PlayScala)

lazy val main = (project in file("."))
    .enablePlugins(PlayScala).dependsOn(admin).aggregate(admin)
```

`modules/admin/build.sbt`

```scala
name := "myadmin"

libraryDependencies ++= Seq(
  "com.mysql" % "mysql-connector-j" % "8.0.33",
  jdbc,
  anorm
)
```

### Project structure

```
build.sbt
app
  └ controllers
  └ models
  └ views
conf
  └ application.conf
  └ routes
modules
  └ admin
    └ build.sbt
    └ conf
      └ admin.routes
    └ app
      └ controllers
      └ models
      └ views
project
  └ build.properties
  └ plugins.sbt
```

> **Note:** Configuration and route file names must be unique in the whole project structure. Particularly, there must be only one `application.conf` file and only one `routes` file. To define additional routes or configuration in sub-projects, use sub-project-specific names. For instance, the route file in `admin` is called `admin.routes`. To use a specific set of settings in development mode for a sub project, it would be even better to put these settings into the build file, e.g. `PlayKeys.devSettings += ("play.http.router", "admin.Routes")`.

`conf/routes`:

```
GET /index                  controllers.HomeController.index()

->  /admin admin.Routes

GET     /assets/*file       controllers.Assets.at(path="/public", file)
```

`modules/admin/conf/admin.routes`:

@[assets-routes](code/common.build.routes)

> **Note:** Resources are served from a unique classloader, and thus resource path must be relative from project classpath root.
> Subprojects resources are generated in `target/web/public/main/lib/{module-name}`, so the resources are accessible from `/public/lib/{module-name}` when using `play.api.Application#resources(uri)` method, which is what the `Assets.at` method does.

### Assets and controller classes should be all defined in the `controllers.admin` package

Java
: @[assets-builder](code/javaguide/common/build/controllers/Assets.java)

Scala
: @[assets-builder](code/scalaguide/common/build/controllers/SubProjectsAssetsBuilder.scala)

And a controller:

Java
: @[](code/javaguide/common/build/controllers/HomeController.java)

Scala
: @[admin-home-controller](code/scalaguide/common/build/controllers/SubProjectsAssetsBuilder.scala)


### Reverse routing in ```admin```

in case of a regular controller call:


```
controllers.admin.routes.HomeController.index
```

and for `Assets`:

```
controllers.admin.routes.Assets.at("...")
```

### Through the browser

```
http://localhost:9000/index
```

triggers

```
controllers.HomeController.index
```

and

```
http://localhost:9000/admin/index
```

triggers

```
controllers.admin.HomeController.index
```
