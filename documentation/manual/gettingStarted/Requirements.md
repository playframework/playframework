<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Play Requirements

A Play application only needs to include the Play JAR files to run properly. These JAR files are published to the Maven Repository so you can use any Java or Scala build tool to build a Play project. However, Play provides an enhanced development experience (support for routes, templates compilation and auto-reloading) when using the sbt or Gradle build tools.

Play requires:

1. Java SE 1.8 or higher
1. A build tool. Choose from:
    1. [sbt](#Verifying-and-installing-sbt) - we recommend the latest version
    1. [Gradle](#Verifying-and-installing-Gradle) - we recommend the latest version

## Verifying and installing Java

To check that you have Java SE 1.8 or higher, enter the following in a terminal:

```bash
java -version
```

You should see something like:

```
java version "1.8.0_121"
Java(TM) SE Runtime Environment (build 1.8.0_121-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.121-b13, mixed mode)
```

You can obtain Java SE from [Oracle’s JDK Site](https://www.oracle.com/technetwork/java/javase/downloads/index.html). 

## Verifying and installing sbt

Play example projects available from [Lightbend Tech Hub](https://developer.lightbend.com/start/?group=play) automatically download dependencies and have `./sbt` and `sbt.bat` launchers for Unix and Windows environments, respectively. You do not have to install sbt to run them.

But if you want to use sbt to create your project, you need to [install the sbt launcher](https://www.scala-sbt.org/download.html) on your system. With sbt installed, you can use our [giter8](http://www.foundweekends.org/giter8/) template for Java or Scala to create a new project with a single command, using `sbt new`. Refer to the [sbt download page](https://www.scala-sbt.org/download.html) to install the sbt launcher on your system and [sbt documentation for details about how to setup it](https://www.scala-sbt.org/release/docs/Setup-Notes.html).

## Verifying and installing Gradle

Play example projects available from [Lightbend Tech Hub](https://developer.lightbend.com/start/?group=play) automatically download dependencies and have `./gradlew` or `gradlew.bat` launchers for Unix and Windows environments, respectively. You do not need to install Gradle to run them.

If you are ready to start your own project and want to use Gradle, refer to [Gradle install page](https://gradle.org/install/) to install Gradle launcher on your system. If you run into problems after installing, check [Gradle's documentation for help](https://docs.gradle.org/4.6/userguide/troubleshooting.html#sec:troubleshooting_installation). We recommend that you use the latest version of Gradle.

## Congratulations!

You are now ready to work with Play!  The next page will show you how to create projects from the command line and some more detail about creating new applications.

## See also

1. Try the [[Hello World tutorial|HelloWorldTutorial]]
1. Create a [[new application from a template|NewApplication]]
1. Learn more from [Play examples](https://developer.lightbend.com/start/?group=play)
