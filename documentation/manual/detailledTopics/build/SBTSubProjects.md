# Working with sub-projects

A complex project is not necessarily composed of a single Play application. You may want to split a large project into several smaller applications, or even extract some logic into a standard Java or Scala library that has nothing to do with a Play application.

It will be helpful to read the [SBT documentation on multi-project builds](http://www.scala-sbt.org/release/docs/Getting-Started/Multi-Project).  Sub-projects do not have their own build file, but share the parent project's build file.

## Adding a simple library sub-project

You can make your application depend on a simple library project. Just add another sbt project definition in your `project/Build.scala` build file:

```
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "my-first-application"
  val appVersion      = "1.0"

  val appDependencies = Seq(
    //if it's a java project add javaCore, javaJdbc, jdbc etc.
  )
  
  val mySubProject = Project("my-library", file("myLibrary"))

  val main = play.Project(
    appName, appVersion, appDependencies, path = file("myProject")
  ).dependsOn(mySubProject)


}
```

Here we have defined a sub-project in the application’s `myLibrary` folder. This sub-project is a standard sbt project, using the default layout:

```
myProject
 └ app
 └ conf
 └ public
myLibrary
 └ src
    └ main
       └ java
       └ scala
project
 └ Build.scala
```

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

## Splitting your web application into several parts

As a Play application is just a standard sbt project with a default configuration, it can depend on another Play application. 

The configuration is very close to the previous one. Simply configure your sub-project as a `play.Project`:

```
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "zenexity.com"
  val appVersion = "1.2"

  val common = play.Project(
    appName + "-common", appVersion, path = file("common")
  )
  
  val website = play.Project(
    appName + "-website", appVersion, path = file("website")
  ).dependsOn(common)
  
  val adminArea = play.Project(
    appName + "-admin", appVersion, path = file("admin")
  ).dependsOn(common)
  
  val main = play.Project(
    appName, appVersion, path = file("main")
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
```

> Note: in order to avoid naming collision, make sure your controllers, including the Assets controller in your subprojects are using a different name space than the main project

## Splitting the route file

As of `play 2.1` it's also possible to split the route file into smaller pieces. This is a very handy feature if you want to create a robust, reusable multi-module play application

### Consider the following build file

`project/Build.scala`:

```scala
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "myproject"
    val appVersion      = "1.0-SNAPSHOT"

    val adminDeps = Seq(
      // Add your project dependencies here,
       "mysql" % "mysql-connector-java" % "5.1.18",
      jdbc,
      anorm
    )

    val mainDeps = Seq()
  
   lazy val admin = play.Project(appName + "-admin", appVersion, adminDeps, path = file("modules/admin"))


  lazy  val main = play.Project(appName, appVersion, mainDeps).settings(
      // Add your own project settings here      
    ).dependsOn(admin).aggregate(admin)

}
```

### project structure

```
app
  └ controllers
  └ models
  └ views
conf
  └ application.conf
  └ routes
modules
  └ admin
    └ conf/admin.routes
    └ app/controllers
    └ app/models
    └ app/views     
project
 └ build.properties
 └ Build.scala
 └ plugins.sbt
```

> Note: there is only a single instance of `application.conf`. Also, the route file in `admin` is called `admin.routes`

`conf/routes`:

```
GET /index                          controllers.Application.index()

->  /admin admin.Routes

GET     /assets/*file               controllers.Assets.at(path="/public", file)
```

`modules/admin/conf/admin.routes`:

```
GET /index                           controllers.admin.Application.index()

GET     /assets/*file               controllers.admin.Assets.at(path="/public", file)

```

### Assets and controller classes should be all defined in the `controllers.admin` package

`modules/admin/controllers/Assets.scala`:

```scala
package controllers.admin
object Assets extends controllers.AssetsBuilder
```

> Note: Java users can do something very similar i.e.

```java
// Assets.java
package controllers.my;
public class Assets {
//can be referenced as `controllers.my.Assets.delegate.at` in the route file
public static controllers.AssetsBuilder delegate = new controllers.AssetsBuilder();
}
```

and a controller:

`modules/admin/controllers/Application.scala`:

```scala
package controllers.admin

import play.api._
import play.api.mvc._
import views.html._

object Application extends Controller {

  def index = Action { implicit request =>
    Ok("admin")
  }
}
```

### Reverse routing in ```admin```

in case of a regular controller call:


```
controllers.admin.routes.Application.index
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
controllers.Application.index
```

and

```
http://localhost:9000/admin/index
``` 

triggers 

```
controllers.admin.Application.index
```

