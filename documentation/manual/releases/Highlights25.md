<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.5

This page highlights the new features of Play 2.5. If you want learn about the changes you need to make to migrate to Play 2.5, check out the [[Play 2.5 Migration Guide|Migration25]].

## Logging SQL statements

There is now a way to log SQL statements that works across different connection pool implementations (like HikariCP and BoneCP) and also different persistence mechanisms such as Anorm, Ebean and JPA. See more information about it works [[for Java|JavaDatabase]] and [[for Scala|ScalaDatabase]].

## Netty native socket transport

You have the possibility to configure the framework to use netty native sockets instead of the default jdk ones.
See more information about it in [[Netty configuration|SettingsNetty]]

## Flexible Logging

You now can configure Play with any SLF4J compatible logging framework, such as log4j2 or AVSL.  Please see the "Using a Custom Logging Framework" section of [[Configuring logging|SettingsLogger]] page.
