<!--- Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com> -->
# Artifact repositories

## Maven Central

All Play artifacts are published to the [Maven Central](https://search.maven.org/) at <https://repo1.maven.org/maven2/com/typesafe/play/>.

This repository is enabled by default in your project, so you don't need to manually add it.

## Accessing nightly snapshots

Nightly snapshots are published to the Sonatype snapshots repository. You can [browse the play directory to find the version of the sbt-plugin you'd like to use](https://oss.sonatype.org/content/repositories/snapshots/com/typesafe/play/sbt-plugin_2.12_1.0/) in your `plugins.sbt`. To enable the snapshots repo in your build, you must add a resolver (typically in `plugins.sbt`):

```scala
resolvers += Resolver.sonatypeRepo("snapshots")
```
