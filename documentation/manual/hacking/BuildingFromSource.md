# Building Play from sources

To benefit from the latest improvements and bug fixes after the initial beta release, you may want to compile Play from sources. You’ll need a [Git client](http://git-scm.com/) to fetch the sources.

## Grab the source
From the shell, first checkout the Play sources:

```bash
$ git clone git://github.com/playframework/playframework.git
```

Then go to the `playframework/framework` directory and launch the `build` script to enter the sbt build console:

```bash
$ cd playframework/framework
$ ./build
> publish-local
```

> Note that you don’t need to install sbt yourself: Play embeds its own version.

If you want to make changes to the code you can use `publish-local` to rebuild the framework.

## Build the documentation

Documentation is available at playframework/documentation as Markdown files.  To see HTML, run the following:

```bash
$ cd playframework/documentation
$ ./build run
```

To see documentation at [http://localhost:9000/@documentation](http://localhost:9000/@documentation)

To build the Scaladoc and Javadoc, run `doc` against the source code:

```bash
$ cd playframework/framework
$ ./build doc
```

## Run tests

You can run basic tests from the sbt console using the `test` task:

```
> test
```

We are also using several Play applications to test the framework. To run this complete test suite, use the `runtests` script:

```
$ ./runtests
```

## Use in projects

Creating projects using the Play version you have built from source works much the same as a regular Play application.

export PATH=$PATH:<projdir>/playframework

If you have an existing Play application that you are upgrading, please add

```
resolvers ++= Seq(
  ...
  Resolver.file("Local Repository", file("<projdir>/playframework/repository/local"))(Resolver.ivyStylePatterns),
  ...
)

addSbtPlugin("play" % "sbt-plugin" % "2.2-SNAPSHOT")
```

to project/plugins.sbt. 

## Using Code in Eclipse

You can find at [Stackoverflow](http://stackoverflow.com/questions/10053201/how-to-setup-eclipse-ide-work-on-the-playframework-2-0/10055419#10055419) some information how to setup eclipse to work on the code.
