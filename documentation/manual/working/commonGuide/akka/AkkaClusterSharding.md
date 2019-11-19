<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Cluster Sharding for Akka Typed (incubating)

Play provides an [incubating](https://developer.lightbend.com/docs/lightbend-platform/introduction/getting-help/support-terminology.html) module for integration with [Akka Cluster Sharding Typed](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html). To enable this module, add the following dependency to your build:

Java
: @[java-cluster-deps](code/javaguide/javaguide.clusterdeps.sbt)

Scala
: @[scala-cluster-deps](code/scalaguide/scalaguide.clusterdeps.sbt)

## Usage

After having properly included `Cluster Sharding` as a dependency, you can obtain an instance by using dependency injection. We've provided some helpers for both runtime and compile-time dependency injection.

Note that Play is only providing the DI mechanism. The class instance that will be made available for injection is Akka's `akka.cluster.sharding.typed.javadsl.ClusterSharding` for Java and `akka.cluster.sharding.typed.scaladsl.ClusterSharding` for Scala.

### Runtime dependency injection

Runtime dependency injection works as any other runtime DI module in Play, meaning that adding the dependency enables the module automatically, and an instance is available for injection.

### Compile-time dependency injection

If you're using compile-time DI, you can get have access to the `ClusterSharding` by using the components like below:

Java
: @[cluster-compile-time-injection](code/javaguide/akka/components/ComponentsWithClusterSharding.java)

Scala
: @[cluster-compile-time-injection](code/scalaguide/akka/components/ComponentsWithClusterSharding.scala)

## Cluster Formation

When including this module, the application `ActorSystem` will be configured for a clustered environment. As a result, it will start [Akka Remote](https://doc.akka.io/docs/akka/2.6/remoting-artery.html) and bind it, by default, to port `25520` (see [Akka docs for how to configure a different port](https://doc.akka.io/docs/akka/2.6/remoting-artery.html#configuration)).

In addition to that, it is expected that your application's Actor System forms a cluster with other instances of your application. Please consult [Akka's documentation](https://doc.akka.io/docs/akka/2.6/typed/cluster.html) on how to form an Akka Cluster.
