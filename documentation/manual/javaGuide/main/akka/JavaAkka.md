# Integrating with Akka

[Akka](http://akka.io/) uses the Actor Model to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications. For fault-tolerance it adopts the ‘Let it crash’ model, which has been used with great success in the telecoms industry to build applications that self-heal - systems that never stop. Actors also provide the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

## The application actor system

Akka 2.0 can work with several containers called `ActorSystems`. An actor system manages the resources it is configured to use in order to run the actors it contains. 

A Play application defines a special actor system to be used by the application. This actor system follows the application life-cycle and restarts automatically when the application restarts.

> **Note:** Nothing prevents you from using another actor system from within a Play application. The provided default actor system is just a convenient way to start a few actors without having to set-up your own.

You can access the default application actor system using the `play.libs.Akka` helper:

@[actor-for](code/javaguide/akka/JavaAkka.java)

## Configuration

The default actor system configuration is read from the Play application configuration file. For example to configure the default dispatcher of the application actor system, add these lines to the `conf/application.conf` file:

@[conf](code/javaguide/akka/akka.conf)

> **Note:** You can also configure any other actor system from the same file, just provide a top configuration key.

## Converting Akka `Future` to Play `Promise`

When you interact asynchronously with an Akka actor you will get a `Future` object. You can easily convert a `Future` to a Play `Promise` by calling the `play.libs.F.Promise.wrap()` method:

@[ask](code/javaguide/akka/ask/Application.java)

## Executing a block of code asynchronously

A common use case within Akka is to have some computation performed concurrently without needing the extra utility of an Actor. If you find yourself creating a pool of Actors for the sole reason of performing a calculation in parallel, there is an easier (and faster) way:

@[async](code/javaguide/akka/async/Application.java)

## Scheduling asynchronous tasks

You can schedule sending messages to actors and executing tasks (functions or `Runnable` instances). You will get a `Cancellable` back that you can call `cancel` on to cancel the execution of the scheduled operation.

For example, to send a message to the `testActor` every 30 minutes:

@[schedule-actor](code/javaguide/akka/JavaAkka.java)

Alternatively, to run a block of code ten milliseconds from now:

@[schedule-code](code/javaguide/akka/JavaAkka.java)

> **Next:** [[Internationalization | JavaI18N]]
