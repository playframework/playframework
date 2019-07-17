<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Deploying your application

We have seen how to run a Play application in development mode, however the `run` command should not be used to run an application in production mode.  When using `run`, on each request, Play checks with sbt to see if any files have changed, and this may have significant performance impacts on your application.

There are several ways to deploy a Play application in production mode. Let's start by using the recommended way, creating a distribution artifact.

## The application secret

Before you run your application in production mode, you need to generate an application secret.  To read more about how to do this, see [[Configuring the application secret|ApplicationSecret]].  In the examples below, you will see the use of `-Dplay.http.secret.key=abcdefghijk`.  You must generate your own secret to use here.

## Deploying Play with JPA
 If you are using JPA, you need to take a look at [Deploying with JPA](https://www.playframework.com/documentation/2.6.x/JavaJPA#deploying-play-with-jpa).

## Using the dist task

The `dist` task builds a binary version of your application that you can deploy to a server without any dependency on sbt, the only thing the server needs is a Java installation.

In the Play console, simply type `dist`:

```bash
[my-first-app] $ dist
```

And will see something like:

```bash
$ sbt
[info] Loading global plugins from /Users/play-developer/.sbt/0.13/plugins
[info] Loading project definition from /Users/play-developer/my-first-app/project
[info] Set current project to my-first-app (in build file:/Users/play-developer/my-first-app/)
[my-first-app] $ dist
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-sources.jar ...
[info] Done packaging.
[info] Wrote /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT.pom
[info] Main Scala API documentation to /Users/play-developer/my-first-app/target/scala-2.11/api...
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-web-assets.jar ...
[info] Done packaging.
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT.jar ...
[info] Done packaging.
model contains 21 documentable templates
[info] Main Scala API documentation successful.
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-javadoc.jar ...
[info] Done packaging.
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-sans-externalized.jar ...
[info] Done packaging.
[info]
[info] Your package is ready in /Users/play-developer/my-first-app/target/universal/my-first-app-1.0-SNAPSHOT.zip
[info]
[success] Total time: 5 s, completed Feb 6, 2017 2:08:44 PM
[my-first-app] $
```

This produces a ZIP file containing all JAR files needed to run your application in the `target/universal` folder of your application.

To run the application, unzip the file on the target server, and then run the script in the `bin` directory.  The name of the script is your application name, and it comes in two versions, a bash shell script, and a windows `.bat` script.

```bash
$ unzip my-first-app-1.0.zip
$ my-first-app-1.0/bin/my-first-app -Dplay.http.secret.key=abcdefghijk
```

You can also specify a different configuration file for a production environment, from the command line:

```bash
$ my-first-app-1.0/bin/my-first-app -Dconfig.file=/full/path/to/conf/application-prod.conf
```

For a full description of usage invoke the start script with a `-h` option.

> For Unix users, zip files do not retain Unix file permissions so when the file is expanded the start script will be required to be set as an executable:
>
> ```bash
> $ chmod +x /path/to/bin/<project-name>
> ```
>
> Alternatively a tar.gz file can be produced instead. Tar files retain permissions. Invoke the `universal:packageZipTarball` task instead of the `dist` task:
>
> ```bash
> sbt universal:packageZipTarball
> ```

By default, the `dist` task will include the API documentation in the generated package. If this is not necessary, add these lines in `build.sbt`:

@[no-scaladoc](code/production.sbt)

For builds with sub-projects, the statement above has to be applied to all sub-project definitions.

## The Native Packager

Play uses the [sbt Native Packager plugin](https://sbt-native-packager.readthedocs.io/en/v1.3.25/). The native packager plugin declares the `dist` task to create a zip file. Invoking the `dist` task is directly equivalent to invoking the following:

```bash
[my-first-app] $ universal:packageBin
```

Many other types of archive can be generated including:

* tar.gz
* OS X disk images
* Microsoft Installer (MSI)
* RPMs
* Debian packages
* System V / init.d and Upstart services in RPM/Debian packages

Please consult the [documentation](https://sbt-native-packager.readthedocs.io/en/v1.3.25/) on the native packager plugin for more information.

### Build a server distribution

The sbt-native-packager plugins provides a number archetypes.  The one that Play uses by default is called the Java server archetype, which enables the following features:

* System V or Upstart startup scripts
* [Default folders](https://sbt-native-packager.readthedocs.io/en/v1.3.25/archetypes/java_server/index.html#default-mappings)

More information can be found in the [Java Server Application Archetype documentation](https://sbt-native-packager.readthedocs.io/en/v1.3.25/archetypes/java_server/index.html).

#### Minimal Debian settings

Add the following settings to your build:

@[debian](code/debian.sbt)

Then build your package with:

```bash
[my-first-app] $ debian:packageBin
```

#### Minimal RPM settings

Add the following settings to your build:

@[rpm](code/rpm.sbt)

Then build your package with:

```bash
[my-first-app] $ rpm:packageBin
```

> There will be some error logging. This is because rpm logs to stderr instead of stdout.

### Including additional files in your distribution

Anything included in your project's `dist` directory will be included in the distribution built by the native packager.  Note that in Play, the `dist` directory is equivalent to the `src/universal` directory mentioned in the native packager's own documentation.

## Play PID Configuration

Play manages its own PID, which is described in the [[Production configuration|ProductionConfiguration]].

Since Play uses a separate pidfile, we have to provide it with a proper path, which is `packageName.value` here.  The name of the pid file must be `play.pid`.  In order to tell the startup script where to place the PID file, put a file `application.ini` inside the `dist/conf` folder and add the following content:

```bash
s"-Dpidfile.path=/var/run/${packageName.value}/play.pid",
# Add all other startup settings here, too
```

Please see the sbt-native-packager [page on Play](https://sbt-native-packager.readthedocs.io/en/v1.3.25/recipes/play.html) for more details.

To prevent Play from creating a PID just set the property to `/dev/null`:

```bash
-Dpidfile.path=/dev/null
```

For a full list of replacements take a closer look at the [customize java server documentation](https://sbt-native-packager.readthedocs.io/en/v1.3.25/archetypes/java_server/customize.html) and [customize java app documentation](https://sbt-native-packager.readthedocs.io/en/v1.3.25/archetypes/java_app/customize.html).

## Publishing to a Maven (or Ivy) repository

You can also publish your application to a Maven repository. This publishes both the JAR file containing your application and the corresponding POM file.

You have to configure the repository you want to publish to, in your `build.sbt` file:

@[publish-repo](code/production.sbt)

Then in the Play console, use the `publish` task:

```bash
[my-first-app] $ publish
```

> Check the [sbt documentation](https://www.scala-sbt.org/release/docs/index.html) to get more information about the resolvers and credentials definition.

## Running a production server in place

In some circumstances, you may not want to create a full distribution, you may in fact want to run your application from your project's source directory.  This requires an sbt installation on the server, and can be done using the `stage` task.

```bash
$ sbt clean stage
```

And you will see something like this:

```bash
$ sbt
[info] Loading global plugins from /Users/play-developer/.sbt/0.13/plugins
[info] Loading project definition from /Users/play-developer/my-first-app/project
[info] Set current project to my-first-app (in build file:/Users/play-developer/my-first-app/)
[my-first-app] $ stage
[info] Updating {file:/Users/play-developer/my-first-app/}root...
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-sources.jar ...
[info] Done packaging.
[info] Wrote /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT.pom
[info] Resolving jline#jline;2.12.2 ...
[info] Done updating.
[info] Main Scala API documentation to /Users/play-developer/my-first-app/target/scala-2.11/api...
[info] Compiling 8 Scala sources and 1 Java source to /Users/play-developer/my-first-app/target/scala-2.11/classes...
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-web-assets.jar ...
[info] Done packaging.
model contains 21 documentable templates
[info] Main Scala API documentation successful.
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-javadoc.jar ...
[info] Done packaging.
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT.jar ...
[info] Done packaging.
[info] Packaging /Users/play-developer/my-first-app/target/scala-2.11/my-first-app_2.11-1.0-SNAPSHOT-sans-externalized.jar ...
[info] Done packaging.
[success] Total time: 8 s, completed Feb 6, 2017 2:11:10 PM
[my-first-app] $
```

This cleans and compiles your application, retrieves the required dependencies and copies them to the `target/universal/stage` directory. It also creates a `bin/<start>` script where `<start>` is the project's name. The script runs the Play server on Unix style systems and there is also a corresponding `bat` file for Windows.

For example to start an application of the project `my-first-app` from the project folder you can:

```bash
$ target/universal/stage/bin/my-first-app -Dplay.http.secret.key=abcdefghijk
```

You can also specify a different configuration file for a production environment, from the command line:

```bash
$ target/universal/stage/bin/my-first-app -Dconfig.file=/full/path/to/conf/application-prod.conf
```

### Running a test instance

Play provides a convenient utility for running a test application in prod mode.

> **Note:** This is not intended for production usage.

To run an application in prod mode, run `runProd`:

```bash
[my-first-app] $ runProd
```

## Using the sbt assembly plugin

Though not officially supported, the sbt assembly plugin may be used to package and run Play applications.  This will produce one jar as an output artifact, and allow you to execute it directly using the `java` command.

To use this, add a dependency on the plugin to your `project/plugins.sbt` file:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
```

Now add the following configuration to your `build.sbt`:

@[assembly](code/assembly.sbt)

Now you can build the artifact by running `sbt assembly`, and run your application by running:

```
$ java -Dplay.http.secret.key=abcdefghijk -jar target/scala-2.XX/<yourprojectname>-assembly-<version>.jar
```

You'll need to substitute in the right project name, version and scala version, of course.
