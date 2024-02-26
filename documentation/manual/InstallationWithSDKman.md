## Installation of Play with SDKman
### Prerequisites

Normally Play is built with [sbt](https://www.scala-sbt.org/) to support feature like routes, templates compilation and
auto-reloading, though other build systems such as maven are supported. In this guide we describe how to install Play
with sbt.

Play requires Java 1.8 or 1.11 and Scala 2.12 or 2.13.

### Installing system and runtime dependencies

To install java and scala you can use the JVM version management tool SDKman. To install SDKman [go to the
installation instructions here](https://sdkman.io/install). Once installed we can list the available JDKs using;

```bash
sdk list java
sdk list scala
```

From here we can choose an installation candidate. At the time of writing (Feb 2022) the latest java 11 version was
`11.0.12-open` from `Java.net`. We can install using this command;

```bash 
sdk install java 11.0.12-open
sdk use java 11.0.12-open
```

To verify your installation, run the following command.

```bash
java -version
```

You should see something like below. This is the output from GraalVM for Java 11.

```bash
openjdk version "11.0.13" 2021-10-19
OpenJDK Runtime Environment GraalVM CE 21.3.0 (build 11.0.13+7-jvmci-21.3-b05)
OpenJDK 64-Bit Server VM GraalVM CE 21.3.0 (build 11.0.13+7-jvmci-21.3-b05, mixed mode, sharing)
```

Once installed we need to install the latest version of scala `2.13`.

```bash 
sdk install scala 2.13.8
sdk use scala 2.13.8
```

```bash
scala -version 
```

To verify the scala install run the following;

```bash
scala -version
```

The output should be similar to this;

```bash
Scala code runner version 2.13.6 -- Copyright 2002-2021, LAMP/EPFL and Lightbend, Inc.
```

We are going to use SBT to help build our Play project. Fortunately we can install this using SDKman as well! There
are various other methods of installation which can be found [here](https://www.scala-sbt.org/download.html)

```bash 
sdk install sbt
```

To verify the sbt installation we can simply run;

```bash
sbt -version
```

And the output should look like this;

```bash
sbt version in this project: 1.6.1
sbt script version: 1.6.1
```

Now we are ready to install our seed scala project.

```bash
sbt new playframework/play-scala-seed.g8
```

The output should be similar to this;

```bash
copying runtime jar...
[info] welcome to sbt 1.6.1 (GraalVM Community Java 11.0.13)
[info] loading global plugins from /home/max/.sbt/1.0/plugins
[info] set current project to new (in build file:/tmp/sbt_1f9e5816/new/)

This template generates a Play Scala project 

name [play-scala-seed]: playgettingstarted 
organization [com.example]: 
play_version [2.8.13]: 
scala_version [2.13.8]: 

Template applied in /home/max/IdeaProjects/IdeaProjects/./playgettingstarted
```

### Running the project

Let's change our directory and run the project;

```bash
cd playgettingstarted
sbt run
```

The first run will take a while since all dependencies for play will need to download. When this is done you'll be
prompted in your terminal that the server is listening on port 9000. You can now navigate to `127.0.0.1:9000` in
your browser, and you'll be greeted with the default splash page for the scala seed project.

Congratulations! You have successfully installed Play!

### Learn more

#### Example projects

There are a large number of examples you can learn from which are [housed here in the play-samples
There are a large number of examples you can learn from which are housed [here in the play-samples
repo](https://github.com/playframework/play-samples).

### Next Steps

- [[Integration with your IDE|IDE]]
- [[Anatomy of a play project|Anatomy]]
    - This covers the structure of a play project and where all the various components sit. It is a bit different from
      a standard scala or java project.
- Examples and How-to
    - Integrating with a database
    - Working with view layer and twirl
    - Working with controllers
-
- [[Create your first controller|GettingStartedControllers]]
- [[Create your first model and database connection|GettingStartedModels]]
- [[Create your first view|GettingStartedViews]]

### More about Play

You can read more about Plays [[philosophy and history here|MoreAboutPlay]]. 
