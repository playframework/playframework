<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Managing library dependencies

## Unmanaged dependencies

Most people end up using managed dependencies - which allows for fine-grained control, but unmanaged dependencies can be simpler when starting out.

Unmanaged dependencies work like this: create a `lib/` directory in the root of your project and then add jar files to that directory. They will automatically be added to the application classpath. There’s not much else to it!

There’s nothing to add to `build.sbt` to use unmanaged dependencies, although you could change a configuration key if you’d like to use a directory different to `lib`.

## Managed dependencies

Play uses Apache Ivy (via sbt) to implement managed dependencies, so if you’re familiar with Maven or Ivy, you won’t have much trouble.

Most of the time you can simply list your dependencies in the `build.sbt` file. 

Declaring a dependency looks like this (defining `group`, `artifact` and `revision`):

```scala
libraryDependencies += "org.apache.derby" % "derby" % "10.4.1.3"
```

or like this, with an optional `configuration`:

```scala
libraryDependencies += "org.apache.derby" % "derby" % "10.4.1.3" % "test"
```

Multiple dependencies can be added either by multiple declarations like the above, or you can provide a Scala sequence:

```scala
libraryDependencies ++= Seq(
  "org.apache.derby" % "derby" % "10.4.1.3",
  "org.hibernate" % "hibernate-entitymanager" % "3.6.9.Final"
)
```

Of course, sbt (via Ivy) has to know where to download the module. If your module is in one of the default repositories sbt comes with then this will just work.

### Getting the right Scala version with `%%`

If you use `groupID %% artifactID % revision` instead of `groupID % artifactID % revision` (the difference is the double `%%` after the `groupID`), sbt will add your project’s Scala version to the artifact name. This is just a shortcut. 

You could write this without the `%%`:

```scala
libraryDependencies += "org.scala-tools" % "scala-stm_2.9.1" % "0.3"
```

Assuming the `scalaVersion` for your build is `2.9.1`, the following is identical:

```scala
libraryDependencies += "org.scala-tools" %% "scala-stm" % "0.3"
```

### Resolvers

sbt uses the standard Maven2 repository and the Typesafe Releases (<https://repo.typesafe.com/typesafe/releases>) repositories by default. If your dependency isn’t on one of the default repositories, you’ll have to add a resolver to help Ivy find it.

Use the `resolvers` setting key to add your own resolver.

```scala
resolvers += name at location
```

For example:

```scala
resolvers += "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
```

sbt can search your local Maven repository if you add it as a repository:

```scala
resolvers += (
    "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"
)
```

