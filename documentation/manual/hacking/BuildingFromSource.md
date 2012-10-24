# Building Play 2.0 from sources

To benefit from the latest improvements and bug fixes after the initial beta release, you may want to compile Play 2.0 from sources. You’ll need a [[Git client | http://git-scm.com/]] to fetch the sources.

From the shell, first checkout the Play 2.0 sources:

```bash
$ git clone git://github.com/playframework/Play20.git
```

Then go to the `Play20/framework` directory and launch the `build` script to enter the sbt build console:

```bash
$ cd Play20/framework
$ ./build
> build-repository
```

Once in the sbt console, run `build-repository` to compile and build everything. This will also create the local Ivy repository containing all of the required dependencies.

> Note that you don’t need to install sbt yourself: Play 2.0 embeds its own version (currently sbt 0.11.2).

If you want to make changes to the code you can use `compile` and `publish-local` to rebuild the framework.

## Running tests

You can run basic tests from the sbt console using the `test` task:

```
> test
```

We are also using several Play applications to test the framework. To run this complete test suite, use the `runtests` script:

```
$ ./runtests
```

## Creating projects

Creating projects using the Play version you have built from source works much the same as a regular Play application.

export PATH=$PATH:<projdir>/Play20

If you have an existing Play 2.0 application that you are upgrading from Play 2.0 Beta to edge, please add 

```
resolvers ++= Seq(
  ...
  Resolver.file("Local Repository", file("<projdir>/Play20/repository/local"))(Resolver.ivyStylePatterns),
  ...
)

addSbtPlugin("play" % "sbt-plugin" % "2.1-SNAPSHOT")
```

to project/plugins.sbt.

## Using Code in eclipse.
You can find at [Stackoverflow](http://stackoverflow.com/questions/10053201/how-to-setup-eclipse-ide-work-on-the-playframework-2-0/10055419#10055419) some information how to setup eclipse to work on the code.