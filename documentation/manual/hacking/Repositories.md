<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Artifact repositories

## Typesafe repository

All Play artifacts are published to the Typesafe repository at <https://repo.typesafe.com/typesafe/releases/>.

> **Note:** it's a Maven2 compatible repository.

To enable it in your sbt build, you must add a proper resolver (typically in `plugins.sbt`):

```
// The Typesafe repository
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
```

## Accessing snapshots

Snapshots are published daily from our [[Continuous Integration Server|ThirdPartyTools]] to the Typesafe snapshots repository at <https://repo.typesafe.com/typesafe/snapshots/>.

> **Note:** it's an ivy style repository.

```
// The Typesafe snapshots repository
resolvers += Resolver.url("Typesafe Ivy Snapshots Repository", url("https://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns)
```

