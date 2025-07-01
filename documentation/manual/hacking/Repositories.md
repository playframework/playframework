<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Artifact repositories

## Maven Central

All Play artifacts are published to the [Maven Central](https://search.maven.org/) at <https://repo1.maven.org/maven2/org/playframework/>.

This repository is enabled by default in your project, so you don't need to manually add it.

## Accessing nightly snapshots

Nightly snapshots are published to the Maven Central snapshots repository. You can [browse the play directory to find the version of the sbt-plugin you'd like to use](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/org/playframework/sbt-plugin_2.12_1.0/) in your `plugins.sbt`. To enable the snapshots repo in your build, you must add a resolver (typically in `plugins.sbt`):

```scala
resolvers += Resolver.sonatypeCentralSnapshots
```
