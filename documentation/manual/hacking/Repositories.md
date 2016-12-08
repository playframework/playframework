<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Artifact repositories

## Typesafe repository

All Play artifacts are published to the Typesafe repository at <https://repo.typesafe.com/typesafe/maven-releases/>.

> **Note:** it's a Maven2 compatible repository.

To enable it in your sbt build, you must add a proper resolver (typically in `plugins.sbt`):

```scala
resolvers += Resolver.typesafeRepo("releases")
```

## Accessing snapshots

Snapshots are published daily from our [[Continuous Integration Server|ThirdPartyTools]] to the Sonatype snapshots repository at <https://oss.sonatype.org/content/repositories/snapshots/>.

> **Note:** it's an ivy style repository.

```scala
resolvers += Resolver.sonatypeRepo("snapshots")
```

