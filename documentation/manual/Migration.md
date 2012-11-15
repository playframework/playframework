Migration guide
===============

Besides the standard deprecations, Play 2.1 contains a few big changes. This document is giving you some information why these changes were necessary and also how to upgrade safely to the new version.

PlayProject
===========

previously here is how your project looked like:
```
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "app-name"
    val appVersion      = "version"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
```
with 2.1 this will change to :

```
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "app-name"
    val appVersion      = "version"

    val appDependencies = Seq(
       // Add your project dependencies here,
       javaCore,
       javaJdbc,
       javaEbean
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
```


(notice the additional dependencies and the lack of mainLang parameter).
The new project layout was necessary because 2.1 introduced modularization which means that now you can pick and choose which artifacts you would like to include into your project. Furthermore, since now there is a javaCore module, so mainLang parameter is not necessary to signal what kind of project we are dealing with anymore("mainly scala or mainly java").

the available play artifacts:
  ```jdbc``` 
  ```anorm```
  ```javaCore```
  ```javaJdbc```
  ```javaEbean```
  ```javaJpa```


scala.concurrent.Future
========================

With the introduction of scala.concurrent.Future in scala 2.10 the scala ecosystem made a huge jump to unify the various Future and Promise libraries out there. The fact that Play is now using scala.concurrent.Future directly means that users can effortlessly combine futures/promises coming from both internal API-s or external libraries. Unfortunately this change also means that scala users would not adjust their codebase to the new API (java users will continue to use a Play's wrapper around scala.concurrent.Future). 



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
  
 ```` 
using scala.concurrent.Future  this will become:
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

notice the extra imports for the execution context (``` play.api.libs.concurrent.execution.Implicits```) and the change for duration (```scala.concurrent.duration ```). Furthermore the "asPromise" method is gone now. 




Generally speaking, if you see error message " error: could not find implicit value for parameter executor", you probably need to add

```import play.api.libs.concurrent.execution.Implicits._```

and you need to remember that if you need a ``PlayPromise`` or a ``PlayRedeemable`` those are mapped to ``scala.concurrent.Future`` and ``scala.concurrent.Promise`` respectively


Scala JSON API
=====================
Play 2.1 comes with a new shiny scala JSON validator and path navigator. This new API however breaks compatibility with existing JSON parsers.

consider:

```
trait play.api.libs.json.Reads[A]{
self=>

def reads(jsValue: JsValue): A

}
```

in 2.1 this becomes:

```
trait play.api.libs.json.Reads[A]{
self=>

def reads(jsValue: JsValue): JsResult[A]

}
```

What this means in practice:

2.0

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

2.1 

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

consider:
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

in 2.1 this becomes:
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


Cookie changes
==============

Due to a change in netty, cookies are expired by setting maxAge to null/None instead of setting it to 0.

maxAge on Cookies has changed to Option[Int] from Int.

RequireJS
==========
in play 2.0 the default behavior for Javascript was to use google closure's commonJS module support. In 2.1 this was changed to use require.JS instead.

What this means in practice is that by default Play will only minify and combine files in stage, dist, start modes only. In dev mode Play will resolve dependencies client side.

If you wish to use this feature, you will need to add your modules to the settings block of your build file:

```
requireJs := "main.js"
```

More information about this feature can be found here:
https://github.com/playframework/Play20/wiki/RequireJS-support

Global changes
==============
Play now has simplified wrappers around typical global features (like filters)

<waiting for the feature to hit master>