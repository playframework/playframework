# Building Play from sources

To benefit from the latest improvements and bug fixes after the initial beta release, you may want to compile Play from sources. You’ll need a [[Git client | http://git-scm.com/]] to fetch the sources.

##Grab the source
From the shell, first checkout the Play sources:

```bash
$ git clone git://github.com/playframework/Play20.git
```

Then go to the `Play20/framework` directory and launch the `build` script to enter the sbt build console:

```bash
$ cd Play20/framework
$ ./build
> publish-local
```

> Note that you don’t need to install sbt yourself: Play 2.0 embeds its own version (Play 2.0.x uses sbt 0.11.3 and Play 2.1.x uses sbt 0.12.0).

If you want to make changes to the code you can use `publish-local` to rebuild the framework.


##Grab the documentation
The . at the end is significant, because it tells git to use the current directory, rather than making a subdirectory for the wiki.
```bash
$ cd Play20/documentation/manual
$ git clone git://github.com/playframework/Play20.wiki.git .
```
###build the documentation
```bash
$ cd Play20/framework
$ ./build doc
```
If done properly, once you run a project, you should be able to see documentation available locally at [http://localhost:9000/@documentation](http://localhost:9000/@documentation)

You can also refer to [this stackoverflow question.](http://stackoverflow.com/questions/10525791/build-play2-0-documentation-from-source-so-that-it-is-available-from-documentat).
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

to project/plugins.sbt. If you switch from 2.0.x to trunk you must change `build.properties` to contain `sbt.version=0.12.0`

## Using Code in eclipse.
You can find at [Stackoverflow](http://stackoverflow.com/questions/10053201/how-to-setup-eclipse-ide-work-on-the-playframework-2-0/10055419#10055419) some information how to setup eclipse to work on the code.