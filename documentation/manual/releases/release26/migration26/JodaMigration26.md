<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Joda Migration Guide

In order to make the default play distribution a bit smaller we removed all occurences of Joda Time from play.
This means that you should migrate to Java8 `java.time`, if you can.

## Use Joda-Time in Forms and Play-Json

If you still need to use Joda-Time in Play Forms and Play-Json you can just add the `play-joda` project:

```
libraryDependencies += "com.typesafe.play" % "play-joda" % "1.0.0"
```

And then import the corresponding Object for Forms:

```
import play.api.data.JodaForms._
```

or for Play-Json

```
import play.api.data.JodaWrites._
import play.api.data.JodaReads._
```