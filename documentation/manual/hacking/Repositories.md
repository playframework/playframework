<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Artifact repositories

## Typesafe repository

All Play artifacts are published to the Typesafe repository at <https://dl.bintray.com/typesafe/maven-releases/>.

> **Note:** it's a Maven2 compatible repository.

To enable it in your sbt build, you must add a proper resolver (typically in `plugins.sbt`):

```scala
// The Typesafe repository
resolvers += "Typesafe Releases" at "https://dl.bintray.com/typesafe/maven-releases/"
```

## Accessing snapshots

Snapshots are published daily from our [[Continuous Integration Server|ThirdPartyTools]] to the Typesafe snapshots repository at <https://oss.sonatype.org/content/repositories/snapshots/>.

> **Note:** it's an ivy style repository.

```scala
// The Typesafe snapshots repository
resolvers += Resolver.url("Typesafe Ivy Snapshots Repository", url("https://oss.sonatype.org/content/repositories/snapshots/"))(Resolver.ivyStylePatterns)
```

