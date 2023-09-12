<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Play Requirements

A Play application only needs to include the Play JAR files to run properly. These JAR files are published to the Maven Repository, therefore you can use any Java or Scala build tool to build a Play project. However, Play provides an enhanced development experience (support for routes, templates compilation and auto-reloading) when using the sbt.

Play requires:

1. One of the Java LTS versions 11, 17, or 21. However, please note that Java 11 support will be dropped in an upcoming Play release, so we recommend using at least Java 17.
1. [sbt](#Verifying-and-installing-sbt) - we recommend the latest version

## Verifying and installing Java

To check that you have Java SE 11 or higher, enter the following in a terminal:

```bash
java -version
```

You should see something like:

```
openjdk version "17.0.8" 2023-07-18
OpenJDK Runtime Environment Temurin-17.0.8+7 (build 17.0.8+7)
OpenJDK 64-Bit Server VM Temurin-17.0.8+7 (build 17.0.8+7, mixed mode)
```

You can obtain Java SE from [Adoptium](https://adoptium.net/).

## Verifying and installing sbt

Play example projects available from [Lightbend Tech Hub](https://developer.lightbend.com/start/?group=play) automatically download dependencies and have `./sbt` and `sbt.bat` launchers for Unix and Windows environments, respectively. You do not have to install sbt to run them.

If you want to use sbt to create a new project, you need to [install the sbt launcher](https://www.scala-sbt.org/download.html) on your system. With sbt installed, you can use our [giter8](http://www.foundweekends.org/giter8/) template for Java or Scala to create your own project with a single command, using `sbt new`. Find the links on the [sbt download page](https://www.scala-sbt.org/download.html) to install the sbt launcher on your system and refer to the [sbt documentation for details about how to set it up](https://www.scala-sbt.org/release/docs/Setup-Notes.html).

## Congratulations!

You are now ready to work with Play! to learn about Play hands-on, try the examples as described on the next page. If you have [sbt installed](https://www.scala-sbt.org/1.x/docs/Setup.html), you can create a new Play  project with a [[single command|NewApplication]], using our giter8 Java or Scala template. The templates set up the project structure and dev environment for you. You can also easily integrate Play projects into your favorite [[IDE|IDE]].
