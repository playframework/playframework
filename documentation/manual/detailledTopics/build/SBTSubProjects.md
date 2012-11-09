# Working with sub-projects

A complex project is not necessarily composed of a single Play application. You may want to split a large project into several smaller applications, or even extract some logic into a standard Java or Scala library that has nothing to do with a Play application.

It will be helpful to read the [SBT documentation on multi-project builds](https://github.com/harrah/xsbt/wiki/Getting-Started-Multi-Project).  Sub-projects do not have their own build file, but share the parent project's build file.

## Adding a simple library sub-project

You can make your application depend on a simple library project. Just add another sbt project definition in your `project/Build.scala` build file:

```
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName         = "my-first-application"
  val appVersion      = "1.0"

  val appDependencies = Seq()
  
  val mySubProject = Project("my-library", file("modules/myLibrary"))

  val main = PlayProject(
    appName, appVersion, appDependencies
  ).dependsOn(mySubProject)
}
```

Here we have defined a sub-project in the application’s `modules/myLibrary` folder. This sub-project is a standard sbt project, using the default layout:

```
modules
 └ myLibrary
    └ src
       └ main
          └ java
          └ scala 
```

When you have a sub-project enabled in your build, you can focus on this project and compile, test or run it individually. Just use the `projects` command in the Play console prompt to display all projects:

```
[my-first-application] $ projects
[info] In file:/Volumes/Data/gbo/myFirstApp/
[info] 	 * my-first-application
[info] 	   my-library
```

To change the current project use the `project` command:

```
[my-first-application] $ project my-library
[info] Set current project to my-library
>
```

When you run your Play application in dev mode, the dependent projects are automatically recompiled, and if something cannot compile you will see the result in your browser:

[[subprojectError.png]]

## Splitting your web application into several parts

As a Play application is just a standard sbt project with a default configuration, it can depend on another Play application. 

The configuration is very close to the previous one. Simply configure your sub-project as a `PlayProject`:

```
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "zenexity.com"
  val appVersion = "1.2"

  val common = PlayProject(
    appName + "-common", appVersion, path = file("modules/common")
  )
  
  val website = PlayProject(
    appName + "-website", appVersion, path = file("modules/website")
  ).dependsOn(common)
  
  val adminArea = PlayProject(
    appName + "-admin", appVersion, path = file("modules/admin")
  ).dependsOn(common)
  
  val main = PlayProject(
    appName, appVersion
  ).dependsOn(
    website, adminArea
  )
}
```

Here we define a complete project split in two main parts: the website and the admin area. Moreover these two parts depend themselves on a common module.

If you would like the dependent projects to be recompiled and tested when you recompile and test the main project then you will need to add an "aggregate" clause.

```
  val main = PlayProject(
    appName, appVersion
  ).dependsOn(
    website, adminArea
  ).aggregate(
    website, adminArea
  )
}
```