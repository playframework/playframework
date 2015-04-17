<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Integrating with Akka

[Akka](http://akka.io/) uses the Actor Model to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications. For fault-tolerance it adopts the ‘Let it crash’ model, which has been used with great success in the telecoms industry to build applications that self-heal - systems that never stop. Actors also provide the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

## The application actor system

Akka can work with several containers called actor systems. An actor system manages the resources it is configured to use in order to run the actors which it contains. 

A Play application defines a special actor system to be used by the application. This actor system follows the application life-cycle and restarts automatically when the application restarts.

> **Note:** Play includes a built in actor system, but you can start your own actor systems if you want. The provided actor system should be suitable for most users.

### Writing actors

To start using Akka, you need to write an actor.  Below is a simple actor that simply says hello to whoever asks it to.

@[actor](code/ScalaAkka.scala)

This actor follows a few Akka conventions:

* The messages it sends/receives, or its _protocol_, are defined on its companion object
* It also defines a `props` method on its companion object that returns the props for creating it

### Creating and using actors

To create and/or use an actor, you need an `ActorSystem`.  This can be obtained by declaring a dependency on an ActorSystem.  , like so:

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

## Configuration

The default actor system configuration is read from the Play application configuration file. For example, to configure the default dispatcher of the application actor system, add these lines to the `conf/application.conf` file:

```
akka.actor.default-dispatcher.fork-join-executor.parallelism-max = 64
akka.actor.debug.receive = on
```

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

> **Note:** This feature is useful if you want to put your play application ActorSystem in an Akka cluster.

## Scheduling asynchronous tasks

You can schedule sending messages to actors and executing tasks (functions or `Runnable`). You will get a `Cancellable` back that you can call `cancel` on to cancel the execution of the scheduled operation.

For example, to send a message to the `testActor` every 300 microseconds:

@[schedule-actor](code/ScalaAkka.scala)

> **Note:** This example uses implicit conversions defined in `scala.concurrent.duration` to convert numbers to `Duration` objects with various time units.

Similarly, to run a block of code 10 milliseconds from now:

@[schedule-callback](code/ScalaAkka.scala)
