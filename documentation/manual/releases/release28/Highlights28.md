<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# What's new in Play 2.8

This page highlights the new features of Play 2.8. If you want to learn about the changes you need to make when you migrate to Play 2.8, check out the [[Play 2.8 Migration Guide|Migration28]].

## Akka 2.6

Play 2.8 brings the latest minor version of Akka. Although Akka 2.6 is binary compatible with 2.5, there are changes in the default configurations and some deprecated features were removed. You can see more details about the changes in [Akka 2.6 migration guide][akka-migration-guide].

[akka-migration-guide]: https://doc.akka.io/docs/akka/2.6/project/migration-guide-2.5.x-2.6.x.html

### Akka Typed

#### Cluster Sharding for Akka Typed

Play 2.8 provides dependency injection support for [Akka Cluster Sharding Typed](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html), allowing users to inject a `ClusterSharding` instance and start sharded typed actors across an [Akka Cluster](https://doc.akka.io/docs/akka/2.6/typed/cluster.html).

### Jackson `ObjectMapper` configuration

Instead of providing its way to create and configure an `ObjectMapper`, which before Play 2.8 requires the user to write a custom binding if some customization is required, Play now uses Akka Jackson support to provide an `ObjectMapper`. It means that it is now possible to add Jackson [Modules](http://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/Module.html) and configure [Features](https://github.com/FasterXML/jackson-databind/wiki/JacksonFeatures) using `application.conf`. For example, if you want to add [Joda support](https://github.com/FasterXML/jackson-datatype-joda), you just need to add the following configuration:

```HOCON
akka.serialization.jackson.jackson-modules += "com.fasterxml.jackson.datatype.joda.JodaModule"
```

And if you need to write numbers as strings, add the following configuration:

```HOCON
akka.serialization.jackson.serialization-features.WRITE_NUMBERS_AS_STRINGS=true
```

### Guice support for Akka Actor Typed

The pre-existing `AkkaGuiceSupport` utility, which helped bind Akka's "classic" actors, has gained additional methods to support Akka 2.6's new [Typed Actor API][].

[Typed Actor API]: https://doc.akka.io/docs/akka/2.6/typed/actors.html

Here's a quick example, in Scala, of defining a Guice module which binds a `HelloActor`:

```scala
object AppModule extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindTypedActor(HelloActor(), "hello-actor")
  }
}
```

And again, in Java, where the actor extends Akka's Java DSL `AbstractBehavior`:

```java
public final class AppModule extends AbstractModule
    implements AkkaGuiceSupport {
  @Override
  protected void configure() {
    bindTypedActor(HelloActor.class, "hello-actor");
  }
}
```

See [[Integrating with Akka Typed|AkkaTyped]] for more details.

## Other additions

### Build additions

When adding Akka moodules to your application, it is important that you use a consistent version of Akka for all the modules since [mixed versioning is not allowed](https://doc.akka.io/docs/akka/current/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed). To make it easier, `play.core.PlayVersion` object now adds `akkaVersion` so that you can use it in your builds like:

```scala
import play.core.PlayVersion.akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion
```

### Lang cookie max age configuration

It is now possible to configure a max age for language cookie. To do that, add the following to your `application.conf`:

```HOCON
play.i18n.langCookieMaxAge = 15 seconds
```

By the default, the configuration is `null` meaning no max age will be set for Lang cookie.

### Threshold for the gzip filter

If you have the gzip filter enabled you can now also set a byte threshold via `application.conf` to control which responses are and aren't gzipped based on their body size:

```HOCON
play.filters.gzip.threshold = 1k
```
Responses whose body size are equal or lower than the given byte threshold won't be compressed, because it's assumed, when gzipped, they end up being bigger than the original body.
If the body size cannot be determined (e.g. chunked responses), then it is assumed the response is over the threshold.
By default the threshold is set to 0 to compress all responses, no matter how large the response body size is.

Please see [[the gzip filter page|GzipEncoding]] for more details.
