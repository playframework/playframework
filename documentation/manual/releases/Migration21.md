<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.1 migration guide

This is a guide for migrating from Play 2.0 to Play 2.1.

To migrate a **Play 2.0.x** application to **Play 2.1.0** first update Play's `sbt-plugin` in the `project/plugins.sbt` file:

```
addSbtPlugin("play" % "sbt-plugin" % "2.1.0")
```

Now update the `project/Build.scala` file to use the new `play.Project` class instead of the `PlayProject` class:

First the import:

```
import play.Project._
```

Then the `main` project creation:

```
val main = play.Project(appName, appVersion, appDependencies).settings(
```

Lastly, update your `project/build.properties` file:

```
sbt.version=0.12.2
```

Then clean and re-compile your project using the `play` command in the **Play 2.1.0** distribution:

```
play clean
play ~run
```

If any compilation errors cropped up, this document will help you figure out what deprecations or incompatible changes may have caused the errors.

## Changes to the build file

Because Play 2.1 introduces further modularization, you now have to explicitly specify the dependencies your application needs. By default any `play.Project` will only contain a dependency to the core Play library.  You have to select the exact set of optional dependencies your application need.  Here are the new modularized dependencies in **Play 2.1**:

- `jdbc` : The **JDBC** connection pool and the the `play.api.db` API.
- `anorm` : The **Anorm** component.
- `javaCore` : The core **Java** API.
- `javaJdbc` : The Java database API.
- `javaEbean` : The Ebean plugin for Java.
- `javaJpa` : The JPA plugin for Java.
- `filters` : A set of build-in filters for Play (such as the CSRF filter).

Here is a typical `Build.scala` file for **Play 2.1**:

```
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "app-name"
    val appVersion      = "1.0"

    val appDependencies = Seq(
       javaCore, javaJdbc, javaEbean
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
```

The `mainLang` parameter for the project is not required anymore. The main language is determined based on the dependencies added to the project. If dependencies contains `javaCore` then the language is set to `JAVA` otherwise `SCALA`.Notice the modularized dependencies in the `appDependencies` section. 

## play.mvc.Controller.form() renamed to play.data.Form.form()

Also related to modularization, the `play.data` package and its dependencies were moved out from play core to `javaCore` artifact. As a consequence of this, `play.mvc.Controller#form` was moved to `play.data.Form#form`

## play.db.ebean.Model.Finder.join() renamed to fetch()

As part of the cleanup the Finder API join methods are replaced with fetch methods. They behave exactly same.

## Play's Promise to become Scala's Future

With the introduction of `scala.concurrent.Future` in Scala 2.10 the scala ecosystem made a huge jump to unify the various Future and Promise libraries out there.

The fact that Play is now using `scala.concurrent.Future` directly means that users can effortlessly combine futures/promises coming from both internal API-s or external libraries.

> Java users will continue to use a Play's wrapper around scala.concurrent.Future for now. 

Consider the following snippet:


```
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import akka.util.duration._

def stream = Action {
    AsyncResult {
      implicit val timeout = Timeout(5.seconds)
      val akkaFuture =  (ChatRoomActor.ref ? (Join()) ).mapTo[Enumerator[String]]
      //convert to play promise before sending the response
      akkaFuture.asPromise.map { chunks =>
        Ok.stream(chunks &> Comet( callback = "parent.message"))
      }
    }
  }
  
```

Using the new `scala.concurrent.Future` this will become:

```
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration._

  def stream = Action {
    AsyncResult {
      implicit val timeout = Timeout(5.seconds)
      val scalaFuture = (ChatRoomActor.ref ? (Join()) ).mapTo[Enumerator[String]]
      scalaFuture.map { chunks =>
        Ok.stream(chunks &> Comet( callback = "parent.message"))
      }
    }
  }
```

Notice the extra imports for:

- The new import for the execution context `play.api.libs.concurrent.Execution.Implicits`
- The change for duration `scala.concurrent.duration` (instead of using the Akka API) 
- The `asPromise` method has been removed

Generally speaking, if you see error message "error: could not find implicit value for parameter executor", you probably need to add:

```
import play.api.libs.concurrent.Execution.Implicits._
```

_(Please see the [Scala documentation about Execution context](http://docs.scala-lang.org/overviews/core/futures.html) for more information)_

And remember that:

- A Play `Promise` is now a Scala `Future`
- A Play `Redeemable` is now a Scala `Promise`

## Changes to the Scala JSON API

**Play 2.1** comes with a shiny new Scala JSON validator and path navigator. This new API however breaks compatibility with existing JSON parsers.

The `play.api.libs.json.Reads` type signature has changed. Consider:

```
trait play.api.libs.json.Reads[A] {
  self =>

  def reads(jsValue: JsValue): A

}
```

In 2.1 this becomes:

```
trait play.api.libs.json.Reads[A] {
  self =>

  def reads(jsValue: JsValue): JsResult[A]

}
```

So, in **Play 2.0** an implementation for a JSON serializer for the `User` type was:

```
implicit object UserFormat extends Format[User] {

  def writes(o: User): JsValue = JsObject(
    List("id" -> JsNumber(o.id),
      "name" -> JsString(o.name),
      "favThings" -> JsArray(o.favThings.map(JsString(_)))
    )
  )

  def reads(json: JsValue): User = User(
    (json \ "id").as[Long],
    (json \ "name").as[String],
    (json \ "favThings").as[List[String]]
  )

}
```

In **Play 2.1** you will need to refactor it as: 

```
implicit object UserFormat extends Format[User] {

  def writes(o: User): JsValue = JsObject(
    List("id" -> JsNumber(o.id),
      "name" -> JsString(o.name),
      "favThings" -> JsArray(o.favThings.map(JsString(_)))
    )   
  )   

  def reads(json: JsValue): JsResult[User] = JsSuccess(User(
    (json \ "id").as[Long],
    (json \ "name").as[String],
    (json \ "favThings").as[List[String]]
  ))  

}
```

The API to generate JSON also evolved. Consider:

```
val jsonObject = Json.toJson(
  Map(
    "users" -> Seq(
      toJson(
        Map(
          "name" -> toJson("Bob"),
          "age" -> toJson(31),
          "email" -> toJson("bob@gmail.com")
        )
      ),
      toJson(
        Map(
          "name" -> toJson("Kiki"),
          "age" -> toJson(25),
          "email" -> JsNull
        )
      )
    )
  )
)
```

With **Play 2.1** this becomes:

```
val jsonObject = Json.obj(
  "users" -> Json.arr(
    Json.obj(
      "name" -> "Bob",
      "age" -> 31,
      "email" -> "bob@gmail.com"
    ),
    Json.obj(
      "name" -> "Kiki",
      "age" -> 25,
      "email" -> JsNull
    )
  )
)
```

More information about these features can be found [[at the Json documentation|ScalaJson]].

## Changes to Cookie handling

Due to a change in _JBoss Netty_, cookies are made to be transient by setting their `maxAge` to be `null` or `None` (depending of the API) instead of setting the `maxAge` to -1.  Any value equal to 0 or less for `maxAge` will cause the cookie to be expired immediately.

The `discardingCookies(String\*)` (Scala) and `discardCookies(String...)` (Java) methods on `SimpleResult` have been deprecated, since these methods are unable to handle cookies set on a particular path, domain or set to be secure.  Please use the `discardingCookies(DiscardingCookie*)` (Scala) and `discardCookie` (Java) methods instead.

## RequireJS

In **Play 2.0** the default behavior for Javascript was to use Google's Closure CommonJS module support. In **Play 2.1** this was changed to use RequireJS instead.

What this means in practice is that by default Play will only minify and combine files in stage, dist, start modes only. In dev mode Play will resolve dependencies client side.

If you wish to use this feature, you will need to add your modules to the settings block of your `project/Build.scala` file:

```
requireJs := "main.js"
```

More information about this feature can be found on the [[RequireJS documentation page|RequireJS-support]].
