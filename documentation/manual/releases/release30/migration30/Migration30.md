<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Play 3.0 Migration Guide

This is a guide for migrating from Play 2.9 to Play 3.0. If you need to migrate from an earlier version of Play, you must first follow the [[Play 2.9 Migration Guide|Migration29]].

## How to migrate

Aside from switching from Akka to [Pekko](https://pekko.apache.org/) (more details [[here|General#How-Play-Deals-with-Akkas-License-Change]]), Play 3.0 offers identical features and bug fixes as in [[Play 2.9|Highlights29]]. Play 3.0 and 2.9 will receive the same features and bug fixes and are maintained in parallel. Since these two major releases are nearly identical, we refer you to the [[Play 2.9 Migration Guide|Migration29]]. Please read through it and follow its steps to migrate from Play 2.8 to Play 2.9. Afterward, continue with the Play 3.0 specific migration notes below to complete the migration to Play 3.0. We also recommend reading through the [[Play 2.9 Highlights|Highlights29]] and [[Play 3.0 Highlights|Highlights30]] pages.

> As mentioned in the [[Play 2.9 Highlights|Highlights29]] and the [[2.9 Migration Guide|Migration29]], Play 2.9 and Play 3.0 both offer Scala 3 support. If you want to migrate your application to Scala 3, please follow the [[Scala 3 Migration Guide|Scala3Migration]] _after_ you finish migrating to Play 3.0.

## Play 3.0 specific migration steps

In addition to the migration steps described in the [[Play 2.9 Migration Guide|Migration29]], there are specific steps you need to take to migrate to Play 3.0:

### Changed `groupId`

With Play 3.0, we changed the `groupId` from `com.typesafe.play` to `org.playframework` to emphasize that Play is fully [community-driven](https://www.playframework.com/sponsors). If you use Play libraries as dependencies or make use of Play sbt plugins, you must update the `groupId` of those dependencies. There might be cases, although rare, where you might need to adjust imports in your code. See this comparison of how to update your build files and code:

Play 3.0
: ```sbt
// Dependencies:
libraryDependencies += "org.playframework" ...
// sbt plugins:
addSbtPlugin("org.playframework" ...
// Source code:
import org.playframework.*
```

Play 2.9
: ```sbt
// Dependencies:
libraryDependencies += "com.typesafe.play" ...
// sbt plugins:
addSbtPlugin("com.typesafe.play" ...
// Source code:
import com.typesafe.play.*
```

### Play upgrade

Apart from the `groupId` change, you need to update the Play version number in `project/plugins.sbt`:

```scala
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.x")
```

Where the "x" in `3.0.x` is the minor version of Play you want to use, for instance `3.0.0`.
Check the release notes for Play's minor version [releases](https://github.com/playframework/playframework/releases).

### Migration from Akka to Pekko

Play 3.0 migrated from Akka to Pekko. The migration is fairly straightforward, primarily involving renaming imports, packages, class names, method names, and configuration keys from Akka to Pekko. No significant code refactoring should be necessary. The Pekko project provides detailed migration guides:

* [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/current/project/migration-guides.html)
* [Pekko HTTP Migration Guide](https://pekko.apache.org/docs/pekko-http/current/migration-guide/index.html)

Here are some additional useful links related to Pekko that you might find interesting:

* [Pekko Website](https://pekko.apache.org/)
* [Overview of all Pekko modules with links to docs](https://pekko.apache.org/modules.html)
* [Pekko 1.0 docs](https://pekko.apache.org/docs/pekko/1.0/)
* [Pekko HTTP 1.0 docs](https://pekko.apache.org/docs/pekko-http/1.0/)
* [Pekko GitHub repository (with Issues and Pull Requests)](https://github.com/apache/incubator-pekko)
* [Pekko HTTP GitHub repository (with Issues and Pull Requests)](https://github.com/apache/incubator-pekko-http)
* [Pekko GitHub discussions](https://github.com/apache/incubator-pekko/discussions)
* [Pekko mailing lists](https://lists.apache.org/list.html?users@pekko.apache.org)

To give you a rough overview, here are the most common migration steps that need to be done. As mentioned, it's essentially a process of renaming. In the end, if you search for "akka" case-insensitively in your project's source code or search for files and folders named "Akka" in a case-insensitive manner, you should not get a match.

Common renames in Scala and Java source files:

Play 3.0 using Pekko
: ```scala
// Imports:
import org.apache.pekko.*
// Changed packages in code:
org.apache.pekko.pattern.after(...)
// Class names:
Pekko.providerOf(...)
class MyModule extends AbstractModule with PekkoGuiceSupport { ... }
// Method names:
headers.convertRequestHeadersPekko(...)
```

Play 2.9 using Akka
: ```scala
// Imports:
import akka.*
// Changed packages in code:
akka.pattern.after(...)
// Class names:
Akka.providerOf(...)
class MyModule extends AbstractModule with AkkaGuiceSupport { ... }
// Method names:
headers.convertRequestHeadersAkka(...)
```

Common Configuration key renames (usually in `conf/application.conf`):

Play 3.0 using Pekko
: ```conf
pekko { }
pekko {
  loggers = ["org.apache.pekko.event.slf4j.Slf4jLogger"]
  logging-filter = "org.apache.pekko.event.slf4j.Slf4jLoggingFilter"
}
pekko.serialization { }
pekko.coordinated-shutdown { }
pekko.remote { }
play.pekko { }
play.pekko.dev-mode { }
play.pekko.dev-mode.pekko { }
promise.pekko.actor { }
```

Play 2.9 using Akka
: ```conf
akka { }
akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]
  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
}
akka.serialization { }
akka.coordinated-shutdown { }
akka.remote { }
play.akka { }
play.akka.dev-mode { }
play.akka.dev-mode.akka { }
promise.akka.actor { }
```

Pay special attention to the server backend configuration if you were previously using Akka HTTP (the default):

Play 3.0 using Pekko
: ```conf
play.server {
      provider = "play.core.server.PekkoHttpServerProvider"
      pekko {
          server-header="PekkoHTTP Server"
          # With Http2 enabled:
          #server-header="PekkoHTTP Server Http2"
      }
}
```

Play 2.9 using Akka
: ```conf
play.server {
      provider = "play.core.server.AkkaHttpServerProvider"
      akka {
          server-header="AkkaHTTP Server"
          # With Http2 enabled:
          #server-header="AkkaHTTP Server Http2"
      }
}
```

Make sure to adjust package and class names in your logging config in `conf/logback.xml`:

Play 3.0 using Pekko
: ```xml
<logger name="org.apache.pekko" level="WARN"/>
```

Play 2.9 using Akka
: ```conf
<logger name="akka" level="WARN"/>
```

If applicable to your project, consider renaming folders and files from Akka to Pekko:

Play 3.0 using Pekko
: ```
app/mymodule/pekkomagic/
app/mymodule/MorePekkoMagic.scala
```

Play 2.9 using Akka
: ```
app/mymodule/akkamagic/
app/mymodule/MoreAkkaMagic.scala
```

#### Changed artifacts

Due to the switch to Pekko, two artifact names have changed, in addition to the change of the `groupId` to `org.playframework`:

Play 3.0 using Pekko
: ```sbt
"org.playframework" %% "play-pekko-http-server" % "3.0.0"
"org.playframework" %% "play-pekko-http2-support" % "3.0.0"
```

Play 2.9 using Akka
: ```sbt
"com.typesafe.play" %% "play-akka-http-server" % "3.0.0"
"com.typesafe.play" %% "play-akka-http2-support" % "3.0.0"
```
