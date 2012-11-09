# Managing library dependencies

## Unmanaged dependencies

Most people end up using managed dependencies - which allows for fine-grained control, but unmanaged dependencies can be simpler when starting out.

Unmanaged dependencies work like this: create a `lib/` directory in the root of your project and then add jar files to that directory. They will automatically be added to the application classpath. There’s not much else to it!

There’s nothing to add to `project/Build.scala` to use unmanaged dependencies, though you could change a configuration key if you’d like to use a directory different to `lib`.

## Managed dependencies

Play 2.0 uses Apache Ivy (via sbt) to implement managed dependencies, so if you’re familiar with Maven or Ivy, you won’t have much trouble.

Most of the time, you can simply list your dependencies in the `project/Build.scala` file. It’s also possible to write a Maven POM file or Ivy configuration file to externally configure your dependencies, and have sbt use those external configuration files.

Declaring a dependency looks like this (defining `group`, `artifact` and `revision`):

```scala
val appDependencies = Seq(
  "org.apache.derby" % "derby" % "10.4.1.3"
)
```

or like this, with an optional `configuration`:

```scala
val appDependencies = Seq(
  "org.apache.derby" % "derby" % "10.4.1.3" % "test"
)
```

Of course, sbt (via Ivy) has to know where to download the module. If your module is in one of the default repositories sbt comes with, this will just work.

### Getting the right Scala version with `%%`

If you use `groupID %% artifactID % revision` instead of `groupID % artifactID % revision` (the difference is the double `%%` after the `groupID`), sbt will add your project’s Scala version to the artifact name. This is just a shortcut. 

You could write this without the `%%`:

```scala
val appDependencies = Seq(
  "org.scala-tools" % "scala-stm_2.9.1" % "0.3"
)
```

Assuming the `scalaVersion` for your build is `2.9.1`, the following is identical:

```scala
val appDependencies = Seq(
  "org.scala-tools" %% "scala-stm" % "0.3"
)
```

### Resolvers

Not all packages live on the same server; sbt uses the standard Maven2 repository and the Scala Tools Releases ([[http://scala-tools.org/repo-releases]]) repositories by default. If your dependency isn’t on one of the default repositories, you’ll have to add a resolver to help Ivy find it.

Use the `resolvers` setting key to add your own resolver.

```scala
resolvers += name at location
```

For example:

```scala
resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
```

sbt can search your local Maven repository if you add it as a repository:

```scala
resolvers += (
    "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)
```

### Final example

Here is a final example, for a project defining several managed dependencies, with custom resolvers:

```scala
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "My first application"
    val appVersion      = "1.0"

    val appDependencies = Seq(
        
      "org.scala-tools" %% "scala-stm" % "0.3",
      "org.apache.derby" % "derby" % "10.4.1.3" % "test"
      
    )

    val main = PlayProject(appName, appVersion, appDependencies).settings(defaultScalaSettings:_*).settings(
      
      resolvers += "JBoss repository" at "https://repository.jboss.org/nexus/content/repositories/",
      resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
            
    )

}

```

> **Next:** [[Working with sub-projects | SBTSubProjects]]