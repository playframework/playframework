<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# Integrating with Akka

[Akka](https://akka.io/) uses the Actor Model to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications. For fault-tolerance it adopts the ‘Let it crash’ model, which has been used with great success in the telecoms industry to build applications that self-heal - systems that never stop. Actors also provide the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

## The application actor system

Akka can work with several containers called actor systems. An actor system manages the resources it is configured to use in order to run the actors which it contains.

A Play application defines a special actor system to be used by the application. This actor system follows the application life-cycle and restarts automatically when the application restarts.

### Writing actors

To start using Akka, you need to write an actor.  Below is a simple actor that simply says hello to whoever asks it to.

@[actor](code/javaguide/akka/HelloActor.java)

Notice here that the `HelloActor` defines a static method called `getProps`, this method returns a `Props` object that describes how to create the actor.  This is a good Akka convention, to separate the instantiation logic from the code that creates the actor.

Another best practice shown here is that the messages that `HelloActor` sends and receives are defined as static inner classes of another class called `HelloActorProtocol`:

@[protocol](code/javaguide/akka/HelloActorProtocol.java)

### Creating and using actors

To create and/or use an actor, you need an `ActorSystem`.  This can be obtained by declaring a dependency on an ActorSystem, then you can use the `actorOf` method to create a new actor.

The most basic thing that you can do with an actor is send it a message.  When you send a message to an actor, there is no response, it's fire and forget.  This is also known as the _tell_ pattern.
  
In a web application however, the _tell_ pattern is often not useful, since HTTP is a protocol that has requests and responses.  In this case, it is much more likely that you will want to use the _ask_ pattern.  The ask pattern returns a Scala `Future`, which you can convert to a Java `CompletionStage` using `scala.compat.java8.FutureConverts.toJava`, and then map to your own result type.

Below is an example of using our `HelloActor` with the ask pattern:

@[ask](code/javaguide/akka/ask/Application.java)

A few things to notice:

* The ask pattern needs to be imported, it's often most convenient to static import the `ask` method.
* The returned future is converted to a `CompletionStage`.  The resulting promise is a `CompletionStage<Object>`, so when you access its value, you need to cast it to the type you are expecting back from the actor.
* The ask pattern requires a timeout, we have supplied 1000 milliseconds.  If the actor takes longer than that to respond, the returned promise will be completed with a timeout error.
* Since we're creating the actor in the constructor, we need to scope our controller as `Singleton`, so that a new actor isn't created every time this controller is used.

## Dependency injecting actors

If you prefer, you can have Guice instantiate your actors and bind actor refs to them for your controllers and components to depend on.

For example, if you wanted to have an actor that depended on the Play configuration, you might do this:

@[injected](code/javaguide/akka/ConfiguredActor.java)

Play provides some helpers to help providing actor bindings.  These allow the actor itself to be dependency injected, and allows the actor ref for the actor to be injected into other components.  To bind an actor using these helpers, create a module as described in the [[dependency injection documentation|JavaDependencyInjection#Play-applications]], then mix in the [`AkkaGuiceSupport`](api/java/play/libs/akka/AkkaGuiceSupport.html) interface and use the `bindActor` method to bind the actor:

@[binding](code/javaguide/akka/modules/MyModule.java)

This actor will both be named `configured-actor`, and will also be qualified with the `configured-actor` name for injection.  You can now depend on the actor in your controllers and other components:

@[inject](code/javaguide/akka/inject/Application.java)

### Dependency injecting child actors

The above is good for injecting root actors, but many of the actors you create will be child actors that are not bound to the lifecycle of the Play app, and may have additional state passed to them.

In order to assist in dependency injecting child actors, Play utilises Guice's [AssistedInject](https://github.com/google/guice/wiki/AssistedInject) support.

Let's say you have the following actor, which depends on configuration to be injected, plus a key:

@[injectedchild](code/javaguide/akka/ConfiguredChildActor.java)

In this case we have used constructor injection - Guice's assisted inject support is only compatible with constructor injection.  Since the `key` parameter is going to be provided on creation, not by the container, we have annotated it with `@Assisted`.

Now in the protocol for the child, we define a `Factory` interface that takes the `key` and returns the `Actor`:

@[protocol](code/javaguide/akka/ConfiguredChildActorProtocol.java)

We won't implement this, Guice will do that for us, providing an implementation that not only passes our `key` parameter, but also locates the `Configuration` dependency and injects that.  Since the trait just returns an `Actor`, when testing this actor we can inject a factor that returns any actor, for example this allows us to inject a mocked child actor, instead of the actual one.

Now, the actor that depends on this can extend [`InjectedActorSupport`](api/java/play/libs/akka/InjectedActorSupport.html), and it can depend on the factory we created:

@[injectedparent](code/javaguide/akka/ParentActor.java)

It uses the `injectedChild` to create and get a reference to the child actor, passing in the key. The second parameter (`key` in this example) will be used as the child actor's name.

Finally, we need to bind our actors.  In our module, we use the `bindActorFactory` method to bind the parent actor, and also bind the child factory to the child implementation:

@[factorybinding](code/javaguide/akka/factorymodules/MyModule.java)

This will get Guice to automatically bind an instance of `ConfiguredChildActorProtocol.Factory`, which will provide an instance of `Configuration` to `ConfiguredChildActor` when it's instantiated.

## Configuration

The default actor system configuration is read from the Play application configuration file. For example, to configure the default dispatcher of the application actor system, add these lines to the `conf/application.conf` file:

@[conf](code/javaguide/akka/akka.conf)

For Akka logging configuration, see [[configuring logging|SettingsLogger]].

### Built-in actor system name

By default the name of the Play actor system is `application`. You can change this via an entry in the `conf/application.conf`:

```
play.akka.actor-system = "custom-name"
```

> **Note:** This feature is useful if you want to put your play application `ActorSystem` in an akka cluster.

## Using your own Actor system

While we recommend you use the built in actor system, as it sets up everything such as the correct classloader, lifecycle hooks, etc, there is nothing stopping you from using your own actor system.  It is important however to ensure you do the following:

* Register a [[stop hook|ScalaDependencyInjection#Stopping/cleaning-up]] to shut the actor system down when Play shuts down
* Pass in the correct classloader from the Play [Environment](api/scala/play/api/Application.html) otherwise Akka won't be able to find your applications classes
* Ensure that you don't read your akka configuration from the default `akka` config, which is used by Play's actor system already, as this will cause problems such as when the systems try to bind to the same remote ports

## Executing a block of code asynchronously

A common use case within Akka is to have some computation performed concurrently without needing the extra utility of an Actor. If you find yourself creating a pool of Actors for the sole reason of performing a calculation in parallel, there is an easier (and faster) way:

@[async](code/javaguide/akka/async/Application.java)

## Akka Coordinated Shutdown

Play handles the shutdown of the `Application` and the `Server` using Akka's [Coordinated Shutdown](https://doc.akka.io/docs/akka/2.6/actors.html?language=java#coordinated-shutdown). Find more information in the [[Coordinated Shutdown|Shutdown]] common section.

NOTE: Play only handles the shutdown of its internal `ActorSystem`. If you are using extra actor systems, make sure they are all terminated and feel free to migrate your termination code to [Coordinated Shutdown](https://doc.akka.io/docs/akka/2.6/actors.html?language=java#coordinated-shutdown).

## Akka Cluster

You can make your Play application join an existing [Akka Cluster](https://doc.akka.io/docs/akka/2.6/cluster-usage.html). In that case it is recommended that you leave the cluster gracefully. Play ships with Akka's Coordinated Shutdown which will take care of that graceful leave. 

If you already have custom Cluster Leave code it is recommended that you replace it with Akka's handling. See [Akka docs](https://doc.akka.io/docs/akka/2.6/actors.html?language=java#coordinated-shutdown) for more details.

## Updating Akka version

If you want to use a newer version of Akka, one that is not used by Play yet, you can add the following to your `build.sbt` file:

@[akka-update](code/javaguide.akkaupdate.sbt)

Of course, other Akka artifacts can be added transitively. Use [sbt-dependency-graph](https://github.com/jrudolph/sbt-dependency-graph) to better inspect your build and check which ones you need to add explicitly.

If you also want to update Akka HTTP, you should also add its dependencies explicitly:

@[akka-http-update](code/javaguide.akkaupdate.sbt)

> **Note:** When doing such updates, keep in mind that you need to follow Akka's [Binary Compatibility Rules](https://doc.akka.io/docs/akka/2.6/common/binary-compatibility-rules.html). And if you are manually adding other Akka artifacts, remember to keep the version of all the Akka artifacts consistent since [mixed versioning is not allowed](https://doc.akka.io/docs/akka/2.6/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed).

> **Note:** When resolving dependencies, sbt will get the newest one declared for this project or added transitively. It means that if Play depends on a newer Akka (or Akka HTTP) version than the one you are declaring, Play version wins. See more details about [how sbt does evictions here](https://www.scala-sbt.org/1.x/docs/Library-Management.html#Eviction+warning).
