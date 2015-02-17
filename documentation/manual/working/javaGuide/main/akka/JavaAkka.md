<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Integrating with Akka

[Akka](http://akka.io/) uses the Actor Model to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications. For fault-tolerance it adopts the ‘Let it crash’ model, which has been used with great success in the telecoms industry to build applications that self-heal - systems that never stop. Actors also provide the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

## The application actor system

Akka can work with several containers called actor systems. An actor system manages the resources it is configured to use in order to run the actors which it contains. 

A Play application defines a special actor system to be used by the application. This actor system follows the application life-cycle and restarts automatically when the application restarts.

> **Note:** Nothing prevents you from using another actor system from within a Play application. The provided default is convenient if you only need to start a few actors without bothering to set-up your own actor system.

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

## Configuration

The default actor system configuration is read from the Play application configuration file. For example, to configure the default dispatcher of the application actor system, add these lines to the `conf/application.conf` file:

@[conf](code/javaguide/akka/akka.conf)

> **Note:** You can also configure any other actor system from the same file, just provide a top configuration key.

For Akka logging configuration, see [[configuring logging|SettingsLogger]].

By default the name of the `ActorSystem` is `application`. You can change this via an entry in the `conf/application.conf`:

```
play.modules.akka.actor-system = "custom-name"
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
