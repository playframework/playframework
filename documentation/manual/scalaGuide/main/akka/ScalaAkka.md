# Integrating with Akka

[Akka](http://akka.io/) uses the Actor Model to raise the abstraction level and provide a better platform to build correct concurrent and scalable applications. For fault-tolerance it adopts the ‘Let it crash’ model, which has been used with great success in the telecoms industry to build applications that self-heal - systems that never stop. Actors also provide the abstraction for transparent distribution and the basis for truly scalable and fault-tolerant applications.

## The application actor system

Akka 2.2.0 can work with several containers called `ActorSystems`. An actor system manages the resources it is configured to use in order to run the actors which it contains. 

A Play application defines a special actor system to be used by the application. This actor system follows the application life-cycle and restarts automatically when the application restarts.

> **Note:** Nothing prevents you from using another actor system from within a Play application. The provided default is convenient if you only need to start a few actors without bothering to set-up your own actor system.

You can access the default application actor system using the `play.api.libs.concurrent.Akka` helper:

@[play-akka-myactor](code/ScalaAkka.scala)


## Configuration

The default actor system configuration is read from the Play application configuration file. For example, to configure the default dispatcher of the application actor system, add these lines to the `conf/application.conf` file:

```
akka.default-dispatcher.fork-join-executor.pool-size-max =64
akka.actor.debug.receive = on
```

> **Note:** You can also configure any other actor system from the same file; just provide a top configuration key.


## Scheduling asynchronous tasks

You can schedule sending messages to actors and executing tasks (functions or `Runnable`). You will get a `Cancellable` back that you can call `cancel` on to cancel the execution of the scheduled operation.

For example, to send a message to the `testActor` every 30 microseconds:

@[play-akka-actor-schedule-repeat](code/ScalaAkka.scala)

> **Note:** This example uses implicit conversions defined in `scala.concurrent.duration` to convert numbers to `Duration` objects with various time units.

Similarly, to run a block of code one seconds from now:

@[play-akka-actor-schedule-run-once](code/ScalaAkka.scala)

> **Next:** [[Internationalization | ScalaI18N]]
