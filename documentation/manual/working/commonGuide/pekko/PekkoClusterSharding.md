<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Cluster Sharding for Pekko Typed (incubating)

Play provides an incubating module for integration with [Pekko Cluster Sharding Typed](https://pekko.apache.org/docs/pekko/1.0/typed/cluster-sharding.html). To enable this module, add the following dependency to your build:

Java
: @[java-cluster-deps](code/javaguide/javaguide.clusterdeps.sbt)

Scala
: @[scala-cluster-deps](code/scalaguide/scalaguide.clusterdeps.sbt)

## Usage

After having properly included `Cluster Sharding` as a dependency, you can obtain an instance by using dependency injection. We've provided some helpers for both runtime and compile-time dependency injection.

Note that Play is only providing the DI mechanism. The class instance that will be made available for injection is Pekko's `pekko.cluster.sharding.typed.javadsl.ClusterSharding` for Java and `pekko.cluster.sharding.typed.scaladsl.ClusterSharding` for Scala.

### Runtime dependency injection

Runtime dependency injection works as any other runtime DI module in Play, meaning that adding the dependency enables the module automatically, and an instance is available for injection.

### Compile-time dependency injection

If you're using compile-time DI, you can get have access to the `ClusterSharding` by using the components like below:

Java
: @[cluster-compile-time-injection](code/javaguide/pekko/components/ComponentsWithClusterSharding.java)

Scala
: @[cluster-compile-time-injection](code/scalaguide/pekko/components/ComponentsWithClusterSharding.scala)

## Cluster Formation

When including this module, the application `ActorSystem` will be configured for a clustered environment. As a result, it will start [Pekko Remote](https://pekko.apache.org/docs/pekko/1.0/remoting-artery.html) and bind it, by default, to port `17355` (see [Pekko docs for how to configure a different port](https://pekko.apache.org/docs/pekko/1.0/remoting-artery.html#configuration)).

In addition to that, it is expected that your application's Actor System forms a cluster with other instances of your application. Please consult [Pekko's documentation](https://pekko.apache.org/docs/pekko/1.0/typed/cluster.html) on how to form an Pekko Cluster.
