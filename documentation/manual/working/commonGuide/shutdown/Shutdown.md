<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Coordinated Shutdown

Play 2.6 incorporated the use of Akka's [Coordinated Shutdown](https://doc.akka.io/docs/akka/current/actors.html?language=scala#coordinated-shutdown) but still didn't rely on it completely. Since Play 2.7, Coordinated Shutdown is responsible for the complete shutdown of Play. In production, the trigger to invoke the clean shutdown could be a `SIGTERM` or, if your Play process is part of an Akka Cluster, a [`Downing`](https://doc.akka.io/docs/akka/2.5/cluster-usage.html) event.

> **Note**: If you are using Play embedded or if you manually handle `Application`'s and `Server`'s on your tests, the migration to Coordinated Shutdown inside Play can affect your shutdown process since using Coordinated Shutdown introduces small changes on the dependent lifecycles of the `Application` and the `Server`: 1) invoking `Server#stop` MUST stop the `Server` and MUST also stop the `Application` running on that `Server`; and 2) invoking `Application#stop` MUST stop the `Application` and MAY also stop the `Server` where the application is running.

## How does it compare to `ApplicationLifecycle`?

Coordinated Shutdown offers several phases where you may register tasks as opposed to `ApplicationLifecycle` only offering a single way to register stop hooks. The traditional `ApplicationLifecycle` stop hooks were a sequence of operations Play would decide the order they were run on. Coordinated Shutdown uses a collection of phases organised in a directed acyclic graph (DAG) where all tasks in a single phase run in parallel. This means that if you moved your cleanup code from `ApplicationLifecycle` to Coordinated Suhtdown you will now have to also specify on what phase should the code run. You could use Coordinated Shutdown over `ApplicationLifecycle` if you want fine-grained control over the order in which seemingly unrelated parts of the application should shutdown. For example, if you wanted to signal over the open websockets that the application is going down, you can register a task on the phase named `before-service-unbind` and make that task push a signal message to the websocket clients. `before-service-unbind` is granted to happen before `service-unbind` which is the phase in which, you guessed it, the server connections are unbound. Unbinding the connections will still allow in-flight requests to complete though.

Play's DI exposes an instance of `CoordinatedShutdown`. If you want to migrate from `ApplicationLifecycle` to Coordinated Shutdown, wherever you requested an instance of `ApplicationLifecycle` to be injected you may now request an instance of `CoordinatedShutdown`.

A `CoordinatedShutdown` instance is bound to an `ActorSystem` instance. In those environments where the `Server` and the `Application` share the `ActorSystem` both `Server` and `Application` will be stopped when either one is stopped. You can find more details on the new section on [[Coordinated Shutdown on the Play manual|Shutdown]] or you can have a look at Akka's [reference docs on Coordinated Shutdown](https://doc.akka.io/docs/akka/2.5/actors.html?language=scala#coordinated-shutdown). 

## Shutdown sequence

Coordinated Shutdown is released with a set of default phases organised as a directed acyclic graph (DAG). You can create new phases and overwrite the default values so existing phases depend on yours. Here's a list of the most relevant phases shipped in Akka and used by Play by default:

```
  before-service-unbind 
  service-unbind
  service-requests-done
  service-stop
  // few cluster-related phases only meant for internal use
  before-actor-system-terminate
  actor-system-terminate
```

The list above mentions the relevant phases in the order they'll run by default. Follow the [Akka docs](https://doc.akka.io/docs/akka/current/actors.html?language=scala#coordinated-shutdown) to change this behavior.

Note the `ApplicationLifecycle#stopHooks` that you don't migrate to Coordinated Shutdown tasks will still run in reverse order of creation and they will run inside `CoordinatedShutdown` during the `service-stop` phase. That is, Coordinated Shutdown considers all `ApplicationLifecycle#stopHooks` like a single task. Coordinated Shutdown gives you the flexibility of running a shutdown task on a different phase. Your current code using `ApplicationLifecycle#stopHooks` should be fine but consider reviewing how and when it's invoked. If, for example, you have an actor which periodically does some database operation then the actor needs a database connection. Depending on how the two are created it's possible your database connection pool is closed in an `ApplicationLifecycle#stopHook` which happens in the `service-stop` phase but your actor might now be closed on the `actor-system-terminate` phase which happens later.

Continue using `ApplicationLifecycle#stopHooks` if running your cleanup code in the `service-stop` phase is coherent with your usage.

To opt-in to using Coordinated Shutdown tasks you need to inject a `CoordinatedShutdown` instance and use `addTask` as the example below:


Scala
: @[shutdown-task](code/shutdown/ResourceAllocatingScalaClass.scala)

Java
: @[shutdown-task](code/shutdown/ResourceAllocatingJavaClass.java)

## Shutdown triggers

A Play process is usually terminated via a `SIGTERM` signal. When the Play process receives the signal, a JVM shutdown hook is run causing the server to stop via invoking Coordinated Shutdown.

Other possible triggers differ from `SIGTERM` slightly. While `SIGTERM` is handled in an outside-in fashion, you may trigger a shutdown from your code (or a library may detect a cause to trigger the shutdown). For example when running your Play process as part of an Akka Cluster or adding an endpoint on your API that would allow an admin or an orchestrator to trigger a programmatic shutdown. In these scenarios the shutdown is inside out: all the phases of the Coordinated Shutdown list are run in the appropriate order, but the Actor System will terminate before the JVM shutdown hook runs.

When developing your Play application, you should consider all the termination triggers and what steps and in which order they will run.

## Limitations

Akka Coordinated Shutdown ships with some settings making it very configurable. Despite that, using Akka Coordinated Shutdown within Play lifecycle makes some of these settings invalid. One such setting is `akka.coordinated-shutdown.exit-jvm`. Enabling `akka.coordinated-shutdown.exit-jvm` in a Play project will cause a deadlock at shutdown preventing your process to ever complete. In general, the default values tuning Akka Coordinated Shutdown should be fine in all Production, Development and Test modes.
