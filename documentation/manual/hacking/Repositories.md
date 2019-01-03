<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Artifact repositories

## Typesafe repository

All Play artifacts are published to the Typesafe repository at <https://repo.typesafe.com/typesafe/maven-releases/>.

> **Note:** it's a Maven2 compatible repository.

To enable it in your sbt build, you must add a proper resolver (typically in `plugins.sbt`):

```scala
resolvers += Resolver.typesafeRepo("releases")
```

## Accessing nightly snapshots

Nightly snapshots of the development (master) branch are published to the Sonatype snapshots repository at <https://oss.sonatype.org/content/repositories/snapshots/>. You can [browse the play directory to find the version of the sbt-plugin you'd like to use](https://oss.sonatype.org/content/repositories/snapshots/com/typesafe/play/sbt-plugin_2.10_0.13/) in your `plugins.sbt`. To enable the snapshots repo in your build, you must add a resolver (typically in `plugins.sbt`):

```scala
resolvers += Resolver.sonatypeRepo("snapshots")
```
