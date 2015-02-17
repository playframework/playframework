<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# About SBT Settings

## About sbt settings

The `build.sbt` file defines settings for your project. You can also define your own custom settings for your project, as described in the [sbt documentation](http://www.scala-sbt.org).  In particular, it helps to be familiar with the [settings](http://www.scala-sbt.org/release/docs/Getting-Started/More-About-Settings) in sbt.

To set a basic setting, use the `:=` operator:

```scala
confDirectory := "myConfFolder"     
```

## Default settings for Java applications

Play defines a default set of settings suitable for Java-based applications. To enable them add the `PlayJava` plugin via your project's enablePlugins method. These settings mostly define the default imports for generated templates e.g. importing `java.lang.*` so types like `Long` are the Java ones by default instead of the Scala ones. `play.Project.playJavaSettings` also imports `java.util.*` so that the default collection library will be the Java one.

## Default settings for Scala applications

Play defines a default set of settings suitable for Scala-based applications. To enable them add the `PlayScala` plugin via your project's enablePlugins method. These default settings define the default imports for generated templates (such as internationalized messages, and core APIs).

## Play project settings with their default value

When you define your sbt project using the default settings explained above, use sbt's `settings` command via the play console for your project (the console is obtained by invoking the `activator` command from the command line). You can then further show the value of a setting by using the sbt `show` command e.g. `show name` will output the project's name.
