## Install Play with Coursier

Play can also be installed with SDKman if you prefer, for instructions installing play with [[SDKman click
here|InstallationWithSDKman]].

### Prerequisites

Normally Play is built with [sbt](https://www.scala-sbt.org/) to support feature like routes, templates compilation and
auto-reloading, though other build systems such as maven are supported. In this guide we describe how to install Play
with sbt.

Play requires Java 1.8 or 1.11 and Scala 2.12 or 2.13.

### Installing system and runtime dependencies

To install java and scala you can use the JVM version management tool Coursier. To install Coursier [go to the
installation instructions here](https://get-coursier.io/docs/cli-installation). Coursier will install a few
dependencies for scala development like `scalafmt` and the scala REPL. We can check our Coursier install like so:

```bash
cs list
```

This will show all the programs managed by Coursier.

From here we can choose an installation candidate for the JVM we want to use. By default, Coursier will choose
AdoptOpenJDK as a runtime environment. The recommended Java version for Play is version 11, so we will install that.

```bash
# Show all the available AdoptOpenJDK JVMs available for install
# `cs java --available` will simply show all candidates available for install.
cs java --available | grep adopt

# A short hand for installing the latest Adopt JVM for java 11
cs java --jvm 11 -version
```

To verify your installation, run the following command.

```bash
java -version
```

You should see something like below. This is the output from GraalVM for Java 11.

```bash
openjdk version "11.0.11" 2021-04-20
OpenJDK Runtime Environment AdoptOpenJDK-11.0.11+9 (build 11.0.11+9)
OpenJDK 64-Bit Server VM AdoptOpenJDK-11.0.11+9 (build 11.0.11+9, mixed mode)
```

Once installed we need to install the latest version of scala `2.13`.

```bash 
cs install scala:2.13.8 scalac:2.13.8
scala -version 
```

The output should be similar to this;

```bash
Scala code runner version 2.13.6 -- Copyright 2002-2021, LAMP/EPFL and Lightbend, Inc.
```

We are going to use SBT to help build our Play project. Fortunately, Coursier automatically installs SBT for us and
should already be on our `$PATH`.  
There are various other methods of installation which can be found [here](https://www.scala-sbt.org/download.html)

To verify the sbt installation we can simply run;

```bash
sbt -version
```

And the output should look like this;

```bash
sbt version in this project: 1.6.1
sbt script version: 1.6.1
```

Now we are ready to install our seed scala project. Change directory where you normally store your projects. In the
case you are using Intellij from jetbrains this is usually;

```bash 
cd ~/IdeaProjects
```

Then we can create our new project using the seed project. This creates a new folder and writes all the needed files
for a minimal play application.

```bash
sbt new playframework/play-scala-seed.g8
```

For now lets add the defaults, though these can be modifided as you need.

```bash
This template generates a Play Scala project 

name [play-scala-seed]: 
organization [com.example]: 
play_version [2.8.13]: 
scala_version [2.13.8]: 
```

The output should be similar to this;

```bash
copying runtime jar...
[info] welcome to sbt 1.6.1 (GraalVM Community Java 11.0.13)
[info] loading global plugins from /home/user/.sbt/1.0/plugins
[info] set current project to new (in build file:/tmp/sbt_1f9e5816/new/)

This template generates a Play Scala project 

name [play-scala-seed]: playgettingstarted 
organization [com.example]: 
play_version [2.8.13]: 
scala_version [2.13.8]: 

Template applied in /home/user/IdeaProjects/IdeaProjects/./play-scala-seed
```

### Running the project

Let's change our directory and run the project;

```bash
cd play-scala-seed
sbt run
```

The first run will take a while since all dependencies for play will need to download. When this is done you'll be
prompted in your terminal that the server is listening on port 9000. You can now navigate to `127.0.0.1:9000` in
your browser, and you'll be greeted with the default splash page for the scala seed project.

Congratulations! You have successfully installed Play!

### Learn more
#### Example projects

There are a large number of examples you can learn from which are housed [here in the play-samples
repo](https://github.com/playframework/play-samples).

### Next Steps

- [[Integration with your IDE|IDE]]
- [[Anatomy of a play project|Anatomy]]
- [[Create your first controller|GettingStartedControllers]]
- [[Create your first model and database connection|GettingStartedModels]]
- [[Create your first view|GettingStartedViews]]

### More about Play

You can read more about Plays [[philosophy and history here|WhatIsPlay]]. 
