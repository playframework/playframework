# Migration guide

The only step to migrate to **Play 2.1** is to change the version of your **Play plugin** in your sbt build.

In `project/plugins.sbt` change this line accordingly:

```
addSbtPlugin("play" % "sbt-plugin" % "2.1")
```

Now you have to recompile your project, and potentially fix some compilation errors. Besides the standard deprecations, Play 2.1 contains a few big incompatible changes. This document is giving you some information why these changes were necessary and also how to upgrade safely to the new version.

## Changes to the build file

Because Play 2.1 introduce further modularization, you have now to specifiy explicitely the dependencies your application needs. By default any _play Project_ will only contain a dependency to the play core library.

> Also we replaced the old `PlayProject` type by the `play.Project` one, so you will probably have to fix your imports.

You have to select the exact set of optional dependencies your application need, in the list of:

- `jdbc` : The **JDBC** connection pool and the the `play.api.db` API. 
- `anorm` : The **Anorm** component.
- `javaCore` : The core **Java** API.
- `javaJdbc` : The Java database API.
- `javaEbean` : The Ebean plugin for Java.
- `javaJpa` : The JPA plugin for Java.
- `filters` : A set of build-in filters for Play (such as the CSRF filter)

Also you don't have anymore to specify the `mainLang` attribute for your project (enabling the `javaCore` dependency for your project is enough to enable Java support for your application).

Here is a typical new `Build.scala` file:

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



## Play's Promise become Scala's Future

With the introduction of `scala.concurrent.Future` in scala 2.10 the scala ecosystem made a huge jump to unify the various Future and Promise libraries out there.

The fact that Play is now using `scala.concurrent.Future` directly means that users can effortlessly combine futures/promises coming from both internal API-s or external libraries. Unfortunately this change also means that scala users would not adjust their codebase to the new API 

> Java users will continue to use a Play's wrapper around scala.concurrent.Future for now. 

Consider the following snippet:


```
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import akka.util.duration._

def stream = Action {
    AsyncResult {
      implicit val timeout = Timeout(5.seconds)
      (ChatRoomActor.ref ? (Join()) ).mapTo[Enumerator[String]].asPromise.map { chunks =>
        Ok.stream(chunks &> Comet( callback = "parent.message"))
      }
    }
  }
  
```

using the new `scala.concurrent.Future` this will become:

```
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.execution.Implicits._

import scala.concurrent.duration._

  def stream = Action {
    AsyncResult {
      implicit val timeout = Timeout(5.seconds)
      (ChatRoomActor.ref ? (Join()) ).mapTo[Enumerator[String]].map { chunks =>
        Ok.stream(chunks &> Comet( callback = "parent.message"))
      }
    }
  }
```

notice the extra imports for:

- The new import for the execution context `play.api.libs.concurrent.execution.Implicits`
- The change for duration `scala.concurrent.duration` instead of using the Akka API) 
- Furthermore the `asPromise` method is gone now

Generally speaking, if you see error message "error: could not find implicit value for parameter executor", you probably need to add

```
import play.api.libs.concurrent.execution.Implicits._
```

_(Please see the Scala documentation about Execution context for mor informations)_

and you need to remember that:

- a Play `Promise` is now a Scala `Future`
- a Play `Redeemable` is now a Scala `Promise`

## Changes to the Scala JSON API

Play 2.1 comes with a new shiny scala JSON validator and path navigator. This new API however breaks compatibility with existing JSON parsers.

Especially the `play.api.libs.json.Reads` type signature has changed. Consider:

```
trait play.api.libs.json.Reads[A] {
  self =>

  def reads(jsValue: JsValue): A

}
```

in 2.1 this becomes:

```
trait play.api.libs.json.Reads[A] {
  self =>

  def reads(jsValue: JsValue): JsResult[A]

}
```

So, in Play 2.0 an implementation for a JSON serializer for the `User` type was:

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

In Play 2.1 you will need to refactor it as: 

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

The API to generate JSON already evolved. Consider:

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

With Play 2.1 this becomes:

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

More information about these features can be found here:

http://mandubian.com/2012/09/08/unveiling-play-2-dot-1-json-api-part1-jspath-reads-combinators/

_(TODO: Itegrate this to the documentation)_


## Changes to Cookie handling

Due to a change in _JBoss Netty_, cookies are made to be transient by setting their `maxAge` to be `null` or `None` (depending of the API) instead of setting the `maxAge` to -1.  Any value equal to 0 or less for `maxAge` will cause the cookie to be expired immediately.

The `discardingCookies(String\*)` (Scala) and `discardCookies(String...)` (Java) methods on `SimpleResult` have been deprecated, since these methods are unable to handle cookies set on a particular path, domain or set to be secure.  Please use the `discardingCookies(DiscardingCookie*)` (Scala) and `discardCookie` (Java) methods instead.

## RequireJS

In play 2.0 the default behavior for Javascript was to use google closure's commonJS module support. In 2.1 this was changed to use require.JS instead.

What this means in practice is that by default Play will only minify and combine files in stage, dist, start modes only. In dev mode Play will resolve dependencies client side.

If you wish to use this feature, you will need to add your modules to the settings block of your build file:

```
requireJs := "main.js"
```

More information about this feature can be found on the [[RequireJS documentation page|RequireJS-support]].
