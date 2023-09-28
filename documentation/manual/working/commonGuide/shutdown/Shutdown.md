<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Coordinated Shutdown

Play incorporates the use of Pekko's [Coordinated Shutdown](https://pekko.apache.org/docs/pekko/1.0/coordinated-shutdown.html?language=scala) but still didn't rely on it completely. While Coordinated Shutdown is responsible for the complete shutdown of Play, there is still the `ApplicationLifecycle` API, and it is Play's responsibility to exit the JVM.

In production, the trigger to invoke the clean shutdown could be a `SIGTERM` or, if your Play process is part of an Pekko Cluster, a [`Downing`](https://pekko.apache.org/docs/pekko/1.0/cluster-usage.html) event.

> **Note**: If you are using Play embedded or if you manually handle `Application`'s and `Server`'s on your tests, the migration to Coordinated Shutdown inside Play can affect your shutdown process since using Coordinated Shutdown introduces small changes on the dependent lifecycles of the `Application` and the `Server`:
> 1. Invoking `Server#stop` MUST stop the `Server` and MUST also stop the `Application` running on that `Server`
> 2. Invoking `Application#stop` MUST stop the `Application` and MAY also stop the `Server` where the application is running.

## How does it compare to `ApplicationLifecycle`?

Coordinated Shutdown offers several phases where you may register tasks as opposed to `ApplicationLifecycle` only offering a single way to register stop hooks. The traditional `ApplicationLifecycle` stop hooks were a sequence of operations Play would decide the order they were run on. Coordinated Shutdown uses a collection of phases organised in a directed acyclic graph (DAG) where all tasks in a single phase run in parallel. This means that if you moved your cleanup code from `ApplicationLifecycle` to Coordinated Shutdown you will now have to also specify on what phase should the code run. You could use Coordinated Shutdown over `ApplicationLifecycle` if you want fine-grained control over the order in which seemingly unrelated parts of the application should shutdown. For example, if you wanted to signal over the open websockets that the application is going down, you can register a task on the phase named `before-service-unbind` and make that task push a signal message to the websocket clients. `before-service-unbind` is granted to happen before `service-unbind` which is the phase in which, you guessed it, the server connections are unbound. Unbinding the connections will still allow in-flight requests to complete though.

Play's DI exposes an instance of `CoordinatedShutdown`. If you want to migrate from `ApplicationLifecycle` to Coordinated Shutdown, wherever you requested an instance of `ApplicationLifecycle` to be injected you may now request an instance of `CoordinatedShutdown`.

A `CoordinatedShutdown` instance is bound to an `ActorSystem` instance. In those environments where the `Server` and the `Application` share the `ActorSystem` both `Server` and `Application` will be stopped when either one is stopped. You can find more details on the new section on [[Coordinated Shutdown on the Play manual|Shutdown]] or you can have a look at Pekko's [reference docs on Coordinated Shutdown](https://pekko.apache.org/docs/pekko/1.0/coordinated-shutdown.html?language=scala). 

## Shutdown sequence

Coordinated Shutdown is released with a set of default phases organised as a directed acyclic graph (DAG). You can create new phases and overwrite the default values so existing phases depend on yours. Here's a list of the most relevant phases shipped in Pekko and used by Play by default:

```
  before-service-unbind
  service-unbind
  service-requests-done
  service-stop
  // few cluster-related phases only meant for internal use
  before-actor-system-terminate
  actor-system-terminate
```

The list above mentions the relevant phases in the order they'll run by default. Follow the [Pekko docs](https://pekko.apache.org/docs/pekko/1.0/coordinated-shutdown.html?language=scala) to change this behavior.

Note the `ApplicationLifecycle#stopHooks` that you don't migrate to Coordinated Shutdown tasks will still run in reverse order of creation and they will run inside `CoordinatedShutdown` during the `service-stop` phase. That is, Coordinated Shutdown considers all `ApplicationLifecycle#stopHooks` like a single task. Coordinated Shutdown gives you the flexibility of running a shutdown task on a different phase. Your current code using `ApplicationLifecycle#stopHooks` should be fine but consider reviewing how and when it's invoked. If, for example, you have an actor which periodically does some database operation then the actor needs a database connection. Depending on how the two are created it's possible your database connection pool is closed in an `ApplicationLifecycle#stopHook` which happens in the `service-stop` phase but your actor might now be closed on the `actor-system-terminate` phase which happens later.

Continue using `ApplicationLifecycle#stopHooks` if running your cleanup code in the `service-stop` phase is coherent with your usage.

To opt-in to using Coordinated Shutdown tasks you need to inject a `CoordinatedShutdown` instance and use `addTask` as the example below:


Scala
: @[shutdown-task](code/shutdown/ResourceAllocatingScalaClass.scala)

Java
: @[shutdown-task](code/shutdown/ResourceAllocatingJavaClass.java)

## Shutdown triggers

A Play process is usually terminated via a `SIGTERM` signal. When the Play process receives the signal, a JVM shutdown hook is run causing the server to stop via invoking Coordinated Shutdown.

Other possible triggers differ from `SIGTERM` slightly. While `SIGTERM` is handled in an outside-in fashion, you may trigger a shutdown from your code (or a library may detect a cause to trigger the shutdown). For example when running your Play process as part of an Pekko Cluster or adding an endpoint on your API that would allow an admin or an orchestrator to trigger a programmatic shutdown. In these scenarios the shutdown is inside out: all the phases of the Coordinated Shutdown list run in the appropriate order, but the Actor System will terminate before the JVM shutdown hook runs.

When developing your Play application, you should consider all the termination triggers and what steps and in which order they will run.

## Limitations

Pekko Coordinated Shutdown ships with some settings making it very configurable. Despite that, using Pekko Coordinated Shutdown within Play lifecycle makes some of these settings invalid. One such setting is `pekko.coordinated-shutdown.exit-jvm`. Enabling `pekko.coordinated-shutdown.exit-jvm` in a Play project will cause a deadlock at shutdown preventing your process to ever complete. In general, the default values tuning Pekko Coordinated Shutdown should be fine in all Production, Development and Test modes.

## Gracefully shutdown the server

The server backend shutdown happens gracefully, and it follows the steps described in the [Pekko HTTP documentation](https://pekko.apache.org/docs/pekko-http/1.0/server-side/graceful-termination.html). The following summary applies to the Pekko HTTP server backend, but Play set up the Netty server backend to follow those steps as close as possible.

1. First, the server port is unbound and no new connections will be accepted (also applies to the Netty backend)
2. If a request is "in-flight" (being handled by user code), it is given hard deadline time to complete. For both Pekko HTTP and the Netty backend, it is possible to configure the deadline using `play.server.terminationTimeout` (see the generic server configuration in [[Pekko HTTP Settings|SettingsPekkoHttp]] or [[Netty Settings|SettingsNetty]] for more details).
3. If a connection has no “in-flight” request, it is terminated immediately
4. If user code emits a response within the timeout, then this response is sent to the client with a `Connection: close` header and connection is closed.
5. If it is a streaming response, it is also mandated that it shall complete within the deadline, and if it does not, the connection will be terminated regardless of status of the streaming response.
   Note: Pekko HTTP contains [a bug](https://github.com/akka/akka-http/issues/3209) which causes it to not take response entity streams into account during graceful termination. As a workaround you can set `play.server.waitBeforeTermination` to a desired delay to give those responses time to finish. See the generic server configuration in [[Pekko HTTP Settings|SettingsPekkoHttp]] or [[Netty Settings|SettingsNetty]] for more details about this config.
6. If user code does not reply with a response within the deadline, an automatic response is sent with a status configured by `pekko.http.server.termination-deadline-exceeded-response`. The value must be a valid [HTTP status code](https://pekko.apache.org/api/pekko-http/1.0/org/apache/pekko/http/scaladsl/model/StatusCodes$.html).
