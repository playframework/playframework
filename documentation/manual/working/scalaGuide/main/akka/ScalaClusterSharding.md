<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Cluster Sharding for Akka Typed (experimental)

Play provides an experimental module for integration with Akka Cluster Sharding Typed. To enable this module, add `scaladslClusterSharding` in your build dependencies:

```scala
libraryDependencies += scaladslClusterSharding
```

## Usage

After having properly included `Cluster Sharding` as a dependency, you can obtain an instance by using dependency injection.
We've provided some helpers for both runtime and compile-time dependency injection.

Note that Play is only providing the DI mechanism. The class instance that will be made available for injection is Akka's `akka.cluster.sharding.typed.scaladsl.ClusterSharding`.

For more information on Akka Cluster Sharding Typed and its usage, consult [Akka's related documentation](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html).

### Runtime dependency injection

Runtime dependency injection works as any other runtime DI module in Play. The module will be automatically included and an instance will be made available for injection.

### Compile-time dependency injection

If you're using compile-time DI, you can get have access to the `akka.cluster.sharding.typed.scaladsl.ClusterSharding` instance by mix in the interface `play.scaladsl.cluster.sharding.typed.ClusterShardingComponents` in your `ApplicationLoader`.

@[cluster-sharding](code/components/ComponentsWithClusterSharding.scala)

## Cluster Formation

When including this module, the application `ActorSystem` will be configured for a clustered environment. As a result, it will start [Akka Remote](https://doc.akka.io/docs/akka/2.6/remoting-artery.html#dependency) and bind it, by default, to port 25520.

In addition to that, it is expected that your application's Actor System forms a cluster with other instances of your application. Please consult [Akka's documentation](https://doc.akka.io/docs/akka/2.6/typed/cluster.html) on how to form an Akka Cluster.
