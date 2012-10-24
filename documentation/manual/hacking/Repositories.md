# Artifact repositories

## Typesafe repository

All Play artifacts are published to the Typesafe repository at [[http://repo.typesafe.com/typesafe/releases/]].

> **Note:** it's a Maven2 compatible repository.

To enable it in your sbt build, you must add a proper resolver (typically in `plugins.sbt`):

```
// The Typesafe repository
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
```

## Accessing snapshots

Snapshots are published daily from our [[Continuous Server|CIServer]] to the Typesafe snapshots repository at [[http://repo.typesafe.com/typesafe/snapshots/]].

```
// The Typesafe snapshots repository
resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
```

