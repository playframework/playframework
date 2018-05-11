<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# Coordinated Shutdown

Play 2.6 incorporated the use of Akka's [Coordinated Shutdown](https://doc.akka.io/docs/akka/current/scala/actors.html#coordinated-shutdown) but still didn't rely on it completely. Since Play 2.7, Coordinated Shutdown is responsible for the complete shutdown of Play. In production, the trigger to invoke the clean shutdown could be a `SIGTERM` or, if your Play process is part of an Akka Cluster, a [`Downing`](https://doc.akka.io/docs/akka/2.5/cluster-usage.html) event.

> **Note**: If you are using Play embedded or if you manually handle `Application`'s and `Server`'s on your tests, the migration to Coordinated Shutdown inside Play can affect your shutdown process since using Coordinated Shutdown introduces small changes on the dependent lifecycles of the `Application` and the `Server`: 1) invoking `Server#stop` MUST stop the `Server` and MUST also stop the `Application` running on that `Server`; and 2) invoking `Application#stop` MUST stop the `Application` and MAY also stop the `Server` where the application is running.

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

The list above mentions the relevant phases in the order they'll run by default. Follow the [Akka docs](https://doc.akka.io/docs/akka/current/scala/actors.html#coordinated-shutdown) to change this behavior.

Note the `ApplicationLifecycle#stopHooks` will run on `service-stop` in reverse order of creation. Coordinated Shutdown gives you the flexibility of running a shutdown task on a different phase. Your current code using `ApplicationLifecycle#stopHooks` should be fine but consider reviewing how and when it's invoked. If, for example, you have an actor which periodically does some database operation then the actor needs a database connection. Depending on how the two are created it's possible your database connection pool is closed in an `ApplicationLifecycle#stopHook` which happens in the `service-stop` phase but your actor might now be closed on the `actor-system-terminate` phase which happens later.

Continue using `ApplicationLifecycle#stopHooks` if running your cleanup code in the `service-stop` phase is coherent with your usage.

To opt-in to using Coordinated Shutdown tasks you need to inject a `CoordinatedShutdown` instance and use `addTask` as the example below:


Scala
: @[shutdown-task](code/shutdown/ResourceAllocatingScalaClass.scala)

Java
: @[shutdown-task](code/shutdown/ResourceAllocatingJavaClass.java)

