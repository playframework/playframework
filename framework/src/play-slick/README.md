# About

This plugin makes [Slick](http://slick.typesafe.com/) a first-class citizen of Play 2.1.

The play-slick plugins consists of 2 parts:
 - DDL schema generation Plugin that works like the Ebean DDL Plugin. Based on config it generates create schema and drop schema SQL commands and writes them to evolutions.
 - A wrapper DB object that uses the datasources defined in the Play config files. It is there so it is possible to use Slick sessions in the same fashion as you would Anorm JDBC connections.

The *intent* is to get this plugin into Play 2.2 if possible.



# Setup
In your application, add `"com.typesafe.play" %% "play-slick" % "0.3.3"` to the appDependencies in your `project/Build.scala` file:

```scala
val appDependencies = Seq(
  //other deps
  "com.typesafe.play" %% "play-slick" % "0.3.3" 
)
```

Note that only Play 2.1.1 is supported.

Please read more about usage on the [wiki](https://github.com/freekh/play-slick/wiki/Usage)

Copyright
---------

Copyright: Typesafe 2013
License: Apache License 2.0, http://www.apache.org/licenses/LICENSE-2.0.html
