<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Installing Play

This page shows how to download, install and run a Play application.  There's a built in tutorial that shows you around, so running this Play application will show you how Play itself works!

Play is a series of libraries available in [Maven Repository](https://mvnrepository.com/artifact/com.typesafe.play), so you can use any Java build tool to build a Play project. However, much of the development experience Play is known for (routes, templates compilation and auto-reloading) is provided by [sbt](https://www.scala-sbt.org/). In this guide we describe how to install Play with sbt.

## Prerequisites

Play requires Java 1.8.  To check that you have the latest JDK, please run:

```bash
java -version
```

You should see something like:

```
java version "1.8.0_121"
Java(TM) SE Runtime Environment (build 1.8.0_121-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.121-b13, mixed mode)
```

If you don't have the JDK, you have to install it from [Oracle's JDK Site](https://www.oracle.com/technetwork/java/javase/downloads/index.html).

## Installing Play with sbt

We provide a number of sample projects that have `./sbt` and `sbt.bat` launchers for Unix and Windows environments respectively. These can be found on our [download page](https://playframework.com/download#examples). The launcher will automatically download dependencies without you having to install sbt ahead of time.

Or, refer to the [sbt download page](https://www.scala-sbt.org/download.html) to install the sbt launcher on your system, which provides the `sbt` command.

> **Note:** See [sbt documentation](https://www.scala-sbt.org/release/docs/Setup-Notes.html) for details about how to setup sbt. We recommend that you use the latest version of sbt.

If your proxy requires user/password for authentication, you need to add system properties when invoking sbt instead: `./sbt -Dhttp.proxyHost=myproxy -Dhttp.proxyPort=8080 -Dhttp.proxyUser=username -Dhttp.proxyPassword=mypassword -Dhttps.proxyHost=myproxy -Dhttps.proxyPort=8080 -Dhttps.proxyUser=username -Dhttps.proxyPassword=mypassword`

### Running Play with sbt

sbt provides all the necessary commands to run your application. For example, you can use `sbt run` to run your app. For more details on running Play from the command line, refer to the [[new application documentation|NewApplication]].

## Congratulations!

You are now ready to work with Play!  The next page will show you how to create projects from the command line and some more detail about creating new applications.
