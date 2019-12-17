<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# What's new in Play 2.8

This page highlights the new features of Play 2.8. If you want to learn about the changes you need to make when you migrate to Play 2.8, check out the [[Play 2.8 Migration Guide|Migration28]].

## Akka 2.6

Play 2.8 brings the latest minor version of Akka. Although Akka 2.6 is binary compatible with 2.5, there are changes in the default configurations, and some deprecated features were removed. You can see more details about the changes in [Akka 2.6 migration guide][akka-migration-guide].

[akka-migration-guide]: https://doc.akka.io/docs/akka/2.6/project/migration-guide-2.5.x-2.6.x.html

### Akka Typed

#### Cluster Sharding for Akka Typed

Play 2.8 provides dependency injection support for [Akka Cluster Sharding Typed](https://doc.akka.io/docs/akka/2.6/typed/cluster-sharding.html), allowing users to inject a `ClusterSharding` instance and start sharded typed actors across an [Akka Cluster](https://doc.akka.io/docs/akka/2.6/typed/cluster.html).

### Jackson 2.10 and new `ObjectMapper` configuration

Jackson dependency was updated to the [latest minor release, 2.10](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.10).

Moreover, instead of providing its way to create and configure an `ObjectMapper`, which before Play 2.8 requires the user to write a custom binding, Play now uses Akka Jackson support to provide an `ObjectMapper`. So, it is now possible to add Jackson [Modules](https://doc.akka.io/docs/akka/2.6/serialization-jackson.html?language=scala#jackson-modules) and configure [Features](https://doc.akka.io/docs/akka/2.6/serialization-jackson.html?language=scala#additional-features) using `application.conf`. For example, if you want to add [Joda support](https://github.com/FasterXML/jackson-datatype-joda), you only need to add the following configuration:

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

## Java 11 support

Play 2.8.0 is the first version were we officially support Java 11. Play, its standalone modules, samples and seeds are all tested against AdoptOpenJDK 8 & 11. We continue to support Java 8 as the default version.

## Other additions

### Support pre-seek Sources for Range Results

In some cases, it is possible to pre-seek the `Source` when returning results for requests containing a `Range` header. For example, if the application is using [Alpakka S3 connector](https://doc.akka.io/docs/alpakka/current/s3.html), it will efficiently download only the section specified by the `Range` header. See more details in [[Java|JavaResponse]] and [[Scala|ScalaResults]] documentation.

### Build additions

Because [Akka does not allow mixing versions](https://doc.akka.io/docs/akka/2.6/common/binary-compatibility-rules.html#mixed-versioning-is-not-allowed), when adding Akka modules to your application, it is important to use a consistent version for all them. To make that easier, `play.core.PlayVersion` object now has an `akkaVersion` variable that you can use it in your builds like:

```scala
import play.core.PlayVersion.akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion
```

### Lang cookie max age configuration

It is now possible to configure a max age for language cookie. To do that, add the following to your `application.conf`:

```HOCON
play.i18n.langCookieMaxAge = 15 seconds
```

By default, the configuration is `null`, meaning no max age will be set for Lang cookie.

### Threshold for the gzip filter

If gzip filter is enabled, it is now possible to set a byte threshold to control which responses are gzipped based on their body size. To do that, add the following configuration in `application.conf`:

```HOCON
play.filters.gzip.threshold = 1k
```

Responses whose body size is equal or lower than the given byte threshold are then not compressed since the compressed size be larger than the original body.

If the body size cannot be determined (for example, chunked responses), then it is assumed the response is over the threshold.

By default, the threshold is set to `0` (zero) to compress all responses, no matter how large the response body size is.

Please see [[the gzip filter page|GzipEncoding]] for more details.
