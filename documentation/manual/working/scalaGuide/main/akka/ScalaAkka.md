<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Integrating with Akka

[Akka](https://akka.io/) uses the Actor Model to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications. For fault-tolerance it adopts the ‘Let it crash’ model, which has been used with great success in the telecoms industry to build applications that self-heal - systems that never stop. Actors also provide the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

## The application actor system

Akka can work with several containers called actor systems. An actor system manages the resources it is configured to use in order to run the actors which it contains.

A Play application defines a special actor system to be used by the application. This actor system follows the application life-cycle and restarts automatically when the application restarts.

### Writing actors

To start using Akka, you need to write an actor.  Below is a simple actor that simply says hello to whoever asks it to.

@[actor](code/ScalaAkka.scala)

This actor follows a few Akka conventions:

* The messages it sends/receives, or its _protocol_, are defined on its companion object
* It also defines a `props` method on its companion object that returns the props for creating it

### Creating and using actors

To create and/or use an actor, you need an `ActorSystem`.  This can be obtained by declaring a dependency on an ActorSystem, like so:

@[controller](code/ScalaAkka.scala)

The `actorOf` method is used to create a new actor.  Notice that we've declared this controller to be a singleton.  This is necessary since we are creating the actor and storing a reference to it, if the controller was not scoped as singleton, this would mean a new actor would be created every time the controller was created, which would ultimate throw an exception because you can't have two actors in the same system with the same name.

### Asking things of actors

The most basic thing that you can do with an actor is send it a message.  When you send a message to an actor, there is no response, it's fire and forget.  This is also known as the _tell_ pattern.
  
In a web application however, the _tell_ pattern is often not useful, since HTTP is a protocol that has requests and responses.  In this case, it is much more likely that you will want to use the _ask_ pattern.  The ask pattern returns a `Future`, which you can then map to your own result type.

Below is an example of using our `HelloActor` with the ask pattern:

@[ask](code/ScalaAkka.scala)

A few things to notice:

* The ask pattern needs to be imported, and then this provides a `?` operator on the actor.
* The return type of the ask is a `Future[Any]`, usually the first thing you will want to do after asking actor is map that to the type you are expecting, using the `mapTo` method.
* An implicit timeout is needed in scope - the ask pattern must have a timeout.  If the actor takes longer than that to respond, the returned future will be completed with a timeout error.

## Dependency injecting actors

If you prefer, you can have Guice instantiate your actors and bind actor refs to them for your controllers and components to depend on.

For example, if you wanted to have an actor that depended on the Play configuration, you might do this:

@[injected](code/ScalaAkka.scala)

Play provides some helpers to help providing actor bindings.  These allow the actor itself to be dependency injected, and allows the actor ref for the actor to be injected into other components.  To bind an actor using these helpers, create a module as described in the [[dependency injection documentation|ScalaDependencyInjection#Play-applications]], then mix in the [`AkkaGuiceSupport`](api/scala/play/api/libs/concurrent/AkkaGuiceSupport.html) trait and use the `bindActor` method to bind the actor:

@[binding](code/ScalaAkka.scala)

This actor will both be named `configured-actor`, and will also be qualified with the `configured-actor` name for injection.  You can now depend on the actor in your controllers and other components:

@[inject](code/ScalaAkka.scala)

### Dependency injecting child actors

The above is good for injecting root actors, but many of the actors you create will be child actors that are not bound to the lifecycle of the Play app, and may have additional state passed to them.

In order to assist in dependency injecting child actors, Play utilises Guice's [AssistedInject](https://github.com/google/guice/wiki/AssistedInject) support.

Let's say you have the following actor, which depends on configuration to be injected, plus a key:

@[injectedchild](code/ScalaAkka.scala)

Note that the `key` parameter is declared to be `@Assisted`, this tells that it's going to be manually provided.

We've also defined a `Factory` trait, this takes the `key`, and returns an `Actor`.  We won't implement this, Guice will do that for us, providing an implementation that not only passes our `key` parameter, but also locates the `Configuration` dependency and injects that.  Since the trait just returns an `Actor`, when testing this actor we can inject a factory that returns any actor, for example this allows us to inject a mocked child actor, instead of the actual one.

Now, the actor that depends on this can extend [`InjectedActorSupport`](api/scala/play/api/libs/concurrent/InjectedActorSupport.html), and it can depend on the factory we created:

@[injectedparent](code/ScalaAkka.scala)

It uses the `injectedChild` to create and get a reference to the child actor, passing in the key. The second parameter (`key` in this example) will be used as the child actor's name.

Finally, we need to bind our actors.  In our module, we use the `bindActorFactory` method to bind the parent actor, and also bind the child factory to the child implementation:

@[factorybinding](code/ScalaAkka.scala)

This will get Guice to automatically bind an instance of `ConfiguredChildActor.Factory`, which will provide an instance of `Configuration` to `ConfiguredChildActor` when it's instantiated.

## Configuration

The default actor system configuration is read from the Play application configuration file. For example, to configure the default dispatcher of the application actor system, add these lines to the `conf/application.conf` file:

```
akka.actor.default-dispatcher.fork-join-executor.parallelism-max = 64
akka.actor.debug.receive = on
```

For Akka logging configuration, see [[configuring logging|SettingsLogger]].

### Built-in actor system name

By default the name of the Play actor system is `application`. You can change this via an entry in the `conf/application.conf`:

```
play.akka.actor-system = "custom-name"
```

> **Note:** This feature is useful if you want to put your play application ActorSystem in an Akka cluster.

## Using your own Actor system

While we recommend you use the built in actor system, as it sets up everything such as the correct classloader, lifecycle hooks, etc, there is nothing stopping you from using your own actor system.  It is important however to ensure you do the following:

* Register a [[stop hook|ScalaDependencyInjection#Stopping/cleaning-up]] to shut the actor system down when Play shuts down
* Pass in the correct classloader from the Play [Environment](api/scala/play/api/Application.html) otherwise Akka won't be able to find your applications classes
* Ensure that you don't read your akka configuration from the default `akka` config, which is used by Play's actor system already, as this will cause problems such as when the systems try to bind to the same remote ports


## Akka Coordinated Shutdown

Play handles the shutdown of the `Application` and the `Server` using Akka's [Coordinated Shutdown](https://doc.akka.io/docs/akka/2.6/actors.html?language=java#coordinated-shutdown). Find more information in the [[Coordinated Shutdown|Shutdown]] common section.

NOTE: Play only handles the shutdown of its internal `ActorSystem`. If you are using extra actor systems, make sure they are all terminated and feel free to migrate your termination code to [Coordinated Shutdown](https://doc.akka.io/docs/akka/2.6/actors.html?language=java#coordinated-shutdown).

## Akka Cluster

You can make your Play application join an existing [Akka Cluster](https://doc.akka.io/docs/akka/2.6/cluster-usage.html). In that case it is recommended that you leave the cluster gracefully. Play ships with Akka's Coordinated Shutdown which will take care of that graceful leave. 

If you already have custom Cluster Leave code it is recommended that you replace it with Akka's handling. See [Akka docs](https://doc.akka.io/docs/akka/2.6/actors.html?language=java#coordinated-shutdown) for more details.

## Updating Akka version

If you want to use a newer version of Akka, one that is not used by Play yet, you can add the following to your `build.sbt` file:

@[akka-update](code/scalaguide.akkaupdate.sbt)

Of course, other Akka artifacts can be added transitively. Use [sbt-dependency-graph](https://github.com/sbt/sbt-dependency-graph) to better inspect your build and check which ones you need to add explicitly.

If you haven't switched to the Netty server backend and therefore are using Play's default Akka HTTP server backend, you also have to update Akka HTTP. Therefore, you need to add its dependencies explicitly as well:

@[akka-http-update](code/scalaguide.akkaupdate.sbt)

### Important note on using Akka HTTP 10.5.0 or newer with Scala 3

Starting with Akka version [2.7.0](https://github.com/akka/akka/pull/31561) and Akka HTTP version [10.4.0](https://doc.akka.io/docs/akka-http/current/release-notes/10.4.x.html#10-4-0), those libraries are published under the  [Business Source License (BSL) v1.1](https://doc.akka.io/docs/akka/current/project/licenses.html). This means that when using these Akka or Akka HTTP versions (or newer), your company might need to pay license fees. For more details, refer to the [Akka License FAQ](https://www.lightbend.com/akka/license-faq). On another note, starting with Akka HTTP version 10.5.0 [native Scala 3 artifacts get published](https://github.com/akka/akka-http/releases/tag/v10.5.0).
Play does not ship with those newer versions, but instead it defaults to using Akka 2.6 and Akka HTTP 10.2.x. You are free to upgrade to the newer commercial versions, as described in the previous section.  However, if you choose to do so and want to use Scala 3, you need to set `akkaHttpScala3Artifacts := true`  to exclude any Akka HTTP Scala 2.13 artifacts that Play depends on by default:

@[akka-exclude-213artifacts](code/scalaguide.akkaupdate.sbt)

This is necessary because Play uses the `for3Use2_13` cross version [workaround](https://www.scala-lang.org/blog/2021/04/08/scala-3-in-sbt.html#using-scala-213-libraries-in-scala-3) to make Akka HTTP 10.2.x work with Scala 3. The above setting disables this behaviour to make sure there are no Akka HTTP Scala 2 artifacts on the classpath (which very likely will conflict with the Akka HTTP Scala 3 artifacts you upgrade to).

Be aware, however, that using this `akkaHttpScala3Artifacts` approach only works when the `PlayAkkaHttpServer` sbt plugin is enabled. If the plugin is not active, but the `play-akka-http-server` dependency is pulled in directly like

```scala
lazy val root = project.in(file("."))
  // PlayAkkaHttpServer, PlayScala, etc. not activated
  .settings(
    // ...
    Compile / mainClass := Some("play.core.server.ProdServerStart"),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-akka-http-server" % "<play_version>",
    )
  )
```

you have to manually apply what `akkaHttpScala3Artifacts` would do:

```scala
val akkaDeps =
  Seq("akka-actor", "akka-actor-typed", "akka-slf4j", "akka-serialization-jackson", "akka-stream")
val scala2Deps = Map(
  "com.typesafe.akka"            -> ("2.6.21", akkaDeps),
  "com.typesafe"                 -> ("0.6.1", Seq("ssl-config-core")),
  "com.fasterxml.jackson.module" -> ("2.14.3", Seq("jackson-module-scala"))
)

lazy val root = project.in(file("."))
  // PlayAkkaHttpServer, PlayScala, PlayJava, etc. not activated
  .settings(
    // ...
    Compile / mainClass := Some("play.core.server.ProdServerStart"),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-akka-http-server" % "<play_version>",
    )
    // Work around needed because akka-http 10.2.x is not published for Scala 3
    excludeDependencies ++=
      (if (scalaBinaryVersion.value == "3") {
        scala2Deps.flatMap(e => e._2._2.map(_ + "_3").map(ExclusionRule(e._1, _))).toSeq
      } else {
        Seq.empty
      }),
    libraryDependencies ++=
      (if (scalaBinaryVersion.value == "3") {
        scala2Deps.flatMap(e => e._2._2.map(e._1 %% _ % e._2._1).map(_.cross(CrossVersion.for3Use2_13))).toSeq
      } else {
        Seq.empty
      }),
  )
```

### Using Cluster Sharding and Akka HTTP without native Scala 3 artifacts

If you use Scala 3 and Akka HTTP version 10.4 or earlier (which does not provide native Scala 3 artifacts) and want to use [[Cluster Sharding for Akka Typed|AkkaClusterSharding]] you have to apply the `for3Use2_13` cross version [workaround](https://www.scala-lang.org/blog/2021/04/08/scala-3-in-sbt.html#using-scala-213-libraries-in-scala-3) to make things work:

```sbt
libraryDependencies += clusterSharding
libraryDependencies += ("com.typesafe.akka" %% "akka-cluster-sharding-typed" % "2.6.21").cross(CrossVersion.for3Use2_13)
excludeDependencies += sbt.ExclusionRule("com.typesafe.akka", "akka-cluster-sharding-typed_3")
```

This configuration ensures that Cluster Sharding for Akka Typed can be used seamlessly with Scala 3 and Akka HTTP versions that lack native Scala 3 artifacts.

> **Note:** When doing such updates, keep in mind that you need to follow Akka's [Binary Compatibility Rules](https://doc.akka.io/docs/akka/2.6/common/binary-compatibility-rules.html). And if you are manually adding other Akka artifacts, remember to keep the version of all the Akka artifacts consistent since [mixed versioning is not allowed](https://doc.akka.io/docs/akka/2.6/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed).

> **Note:** When resolving dependencies, sbt will get the newest one declared for this project or added transitively. It means that if Play depends on a newer Akka (or Akka HTTP) version than the one you are declaring, Play's version wins. See more details about [how sbt does evictions here](https://www.scala-sbt.org/1.x/docs/Library-Management.html#Eviction+warning).
