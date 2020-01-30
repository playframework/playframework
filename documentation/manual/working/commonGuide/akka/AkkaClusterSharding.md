<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->
# Cluster Sharding for Akka Typed (incubating)

Play provides an [incubating](https://developer.lightbend.com/docs/lightbend-platform/introduction/getting-help/support-terminology.html) module for integration with [Akka Cluster Sharding Typed](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html). To enable this module, add the following dependency to your build:

Java
: @[java-cluster-sharding-deps](code/javaguide/javaguide.clusterdeps.sbt)

Scala
: @[scala-cluster-sharding-deps](code/scalaguide/scalaguide.clusterdeps.sbt)

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

## Cluster only

It's also possible to only include the integration with [Akka Cluster](https://doc.akka.io/docs/akka/2.6/typed/index-cluster.html) without using [Sharding](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html).

To only enable the cluster module, add the following dependency to your build instead:

: @[scala-cluster-only-deps](code/scalaguide/scalaguide.clusterdeps.sbt)

> Note: this is the same for Java and Scala as it doesn't provide any extra API. It only configures the `ActorSystem` for cluster and adds dev mode cluster formation.

## Cluster Formation

When including the Cluster Sharding or Cluster module, the application `ActorSystem` will be configured for a clustered environment. As a result, it will start [Akka Remote](https://doc.akka.io/docs/akka/2.6/remoting-artery.html) and bind it, by default, to port `25520` (see [Akka docs for how to configure a different port](https://doc.akka.io/docs/akka/2.6/remoting-artery.html#configuration)).

In dev and test mode, the `ActorSystem` will join itself and form a single node cluster. This is a convenience for local development only.

If you desire to form a cluster on your local machine, you can disable it by setting `play.cluster.dev-mode.join-self` to `false`. You will then need to configure each instance to bind to different ports (http and remote ports) and form a cluster using seed-nodes.

For example, the following sbt command will start one node on your local machine and use seed-nodes to form a cluster to itself:

```shell
# this will bind on the default porst 9000 (http) and 25520 (remote)
sbt -Dakka.remote.artery.canonical.hostname=0.0.0.0 run
```

> **Note**: in dev mode, the easiest way to let two nodes communicate is to bind akka remote to all interfaces, hence `akka.remote.artery.canonical.hostname=0.0.0.0`.

You can then start a second node on your local machine and let it 'talk' with the first node to form a cluster:

```shell
 sbt  -Dplay.cluster.dev-mode.join-self=false -Dakka.cluster.seed-nodes.0=akka://application@0.0.0.0:25520 -Dakka.remote.artery.canonical.port=25521 -Dhttp.port=9001 run
```

> **Note**: when starting the second you must disable self join, add a seed node pointing to the first node, change the remote port and the http port.

In prod mode, it is expected that your application's Actor System forms a cluster with other instances of your application. Please consult [Akka's documentation](https://doc.akka.io/docs/akka/2.6/typed/cluster.html) and [Cluster Bootstrap](https://doc.akka.io/docs/akka/2.6/additional/operations.html#cluster-bootstrap) on how to form an Akka Cluster.
