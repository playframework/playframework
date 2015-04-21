<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Integrating with Akka

[Akka](http://akka.io/) uses the Actor Model to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications. For fault-tolerance it adopts the ‘Let it crash’ model, which has been used with great success in the telecoms industry to build applications that self-heal - systems that never stop. Actors also provide the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

## The application actor system

Akka can work with several containers called actor systems. An actor system manages the resources it is configured to use in order to run the actors which it contains.

A Play application defines a special actor system to be used by the application. This actor system follows the application life-cycle and restarts automatically when the application restarts.

### Writing actors

To start using Akka, you need to write an actor.  Below is a simple actor that simply says hello to whoever asks it to.

@[actor](code/javaguide/akka/HelloActor.java)

Notice here that the `HelloActor` defines a static method called `props`, this returns a `Props` object that describes how to create the actor.  This is a good Akka convention, to separate the instantiation logic from the code that creates the actor.

Another best practice shown here is that the messages that `HelloActor` sends and receives are defined as static inner classes of another class called `HelloActorProtocol`:

@[protocol](code/javaguide/akka/HelloActorProtocol.java)

### Creating and using actors

To create and/or use an actor, you need an `ActorSystem`.  This can be obtained by declaring a dependency on an ActorSystem, then you can use the `actorOf` method to create a new actor.

The most basic thing that you can do with an actor is send it a message.  When you send a message to an actor, there is no response, it's fire and forget.  This is also known as the _tell_ pattern.
  
In a web application however, the _tell_ pattern is often not useful, since HTTP is a protocol that has requests and responses.  In this case, it is much more likely that you will want to use the _ask_ pattern.  The ask pattern returns a Scala `Future`, which you can then wrap in a Play `Promise`, and then map to your own result type.

Below is an example of using our `HelloActor` with the ask pattern:

@[ask](code/javaguide/akka/ask/Application.java)

A few things to notice:

* The ask pattern needs to be imported, it's often most convenient to static import the `ask` method.
* The returned future is wrapped in a `Promise`.  The resulting promise is a `Promise<Object>`, so when you access its value, you need to cast it to the type you are expecting back from the actor.
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

Let's say you have the following actor, which depends configuration to be injected, plus a key:

@[injectedchild](code/javaguide/akka/ConfiguredChildActor.java)

In this case we have used constructor injection - Guice's assisted inject support is only compatible with constructor injection.  Since the `key` parameter is going to be provided on creation, not by the container, we have annotated it with `@Assisted`.

Now in the protocol for the child, we define a `Factory` interface that takes the `key` and returns the `Actor`:

@[protocol](code/javaguide/akka/ConfiguredChildActorProtocol.java)

We won't implement this, Guice will do that for us, providing an implementation that not only passes our `key` parameter, but also locates the `Configuration` dependency and injects that.  Since the trait just returns an `Actor`, when testing this actor we can inject a factor that returns any actor, for example this allows us to inject a mocked child actor, instead of the actual one.

Now, the actor that depends on this can extend [`InjectedActorSupport`](api/java/play/libs/akka/InjectedActorSupport.html), and it can depend on the factory we created:

@[injectedparent](code/javaguide/akka/ParentActor.java)

It uses the `injectedChild` to create and get a reference to the child actor, passing in the key.

Finally, we need to bind our actors.  In our module, we use the `bindActorFactory` method to bind the parent actor, and also bind the child factory to the child implementation:

@[factorybinding](code/javaguide/akka/factorymodules/MyModule.java)

This will get Guice to automatically bind an instance of `ConfiguredChildActorProtocol.Factory`, which will provide an instance of `Configuration` to `ConfiguredChildActor` when it's instantiated.

## Configuration

The default actor system configuration is read from the Play application configuration file. For example, to configure the default dispatcher of the application actor system, add these lines to the `conf/application.conf` file:

@[conf](code/javaguide/akka/akka.conf)

For Akka logging configuration, see [[configuring logging|SettingsLogger]].

### Changing configuration prefix

In case you want to use the `akka.*` settings for another Akka actor system, you can tell Play to load its Akka settings from another location.

```
play.akka.config = "my-akka"
```

Now settings will be read from the `my-akka` prefix instead of the `akka` prefix.

```
my-akka.actor.default-dispatcher.fork-join-executor.pool-size-max = 64
my-akka.actor.debug.receive = on
```

### Built-in actor system name

By default the name of the Play actor system is `application`. You can change this via an entry in the `conf/application.conf`:

```
play.akka.actor-system = "custom-name"
```

> **Note:** This feature is useful if you want to put your play application ActorSystem in an akka cluster.

## Executing a block of code asynchronously

A common use case within Akka is to have some computation performed concurrently without needing the extra utility of an Actor. If you find yourself creating a pool of Actors for the sole reason of performing a calculation in parallel, there is an easier (and faster) way:

@[async](code/javaguide/akka/async/Application.java)

## Scheduling asynchronous tasks

You can schedule sending messages to actors and executing tasks (functions or `Runnable` instances). You will get a `Cancellable` back that you can call `cancel` on to cancel the execution of the scheduled operation.

For example, to send a message to the `testActor` every 30 minutes:

@[schedule-actor](code/javaguide/akka/JavaAkka.java)

Alternatively, to run a block of code ten milliseconds from now:

@[schedule-code](code/javaguide/akka/JavaAkka.java)
