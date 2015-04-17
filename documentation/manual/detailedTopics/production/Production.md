<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Deploying your application

We have seen how to run a Play application in development mode, however the `run` command should not be used to run an application in production mode.  When using `run`, on each request, Play checks with sbt to see if any files have changed, and this may have significant performance impacts on your application.

There are several ways to deploy a Play application in production mode. Let's start by using the recommended way, creating a distribution artifact.

## The application secret

Before you run your application in production mode, you need to generate an application secret.  To read more about how to do this, see [[Configuring the application secret|ApplicationSecret]].  In the examples below, you will see the use of `-Dapplication.secret=abcdefghijk`.  You must generate your own secret to use here.

## Using the dist task

The dist task builds a binary version of your application that you can deploy to a server without any dependency on sbt or activator, the only thing the server needs is a Java installation.

In the Play console, simply type `dist`:

```bash
[my-first-app] $ dist
```

[[images/dist.png]]

This produces a ZIP file containing all JAR files needed to run your application in the `target/universal` folder of your application.

To run the application, unzip the file on the target server, and then run the script in the `bin` directory.  The name of the script is your application name, and it comes in two versions, a bash shell script, and a windows `.bat` script.

```bash
$ unzip my-first-app-1.0.zip
$ my-first-app-1.0/bin/my-first-app -Dplay.crypto.secret=abcdefghijk
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
> activator universal:packageZipTarball
> ```

By default, the `dist` task will include the API documentation in the generated package. If this is not necessary, add these lines in `build.sbt`:

@[no-scaladoc](code/production.sbt)

For builds with sub-projects, the statement above has to be applied to all sub-project definitions.

## The Native Packager

Play uses the [SBT Native Packager plugin](http://www.scala-sbt.org/sbt-native-packager/). The native packager plugin declares the `dist` task to create a zip file. Invoking the `dist` task is directly equivalent to invoking the following:

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

Please consult the [documentation](http://www.scala-sbt.org/sbt-native-packager) on the native packager plugin for more information.

### Build a server distribution

The sbt-native-packager plugins provides a number archetypes.  The one that Play uses by default is called the Java server archetype, which enables the following features:

* System V or Upstart startup scripts
* [Default folders](http://www.scala-sbt.org/sbt-native-packager/archetypes/java_server/my-first-project.html#default-mappings)

A full documentation can be found in the [documentation](http://www.scala-sbt.org/sbt-native-packager/archetypes/java_server/index.html).

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

Play manages its own PID, which is described in the [[Production configuration|ProductionConfiguration]].  In order to tell the startup script where to place the PID file put a file `application.ini` inside the `dist/conf` folder and add the following content:

```bash
-Dpidfile.path=/var/run/${{app_name}}/play.pid
# Add all other startup settings here, too
```

For a full list of replacements take a closer look at the [customize java server documentation](http://www.scala-sbt.org/sbt-native-packager/archetypes/java_server/customize.html) and [customize java app documentation](http://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/customize.html).

## Publishing to a Maven (or Ivy) repository

You can also publish your application to a Maven repository. This publishes both the JAR file containing your application and the corresponding POM file.

You have to configure the repository you want to publish to, in your `build.sbt` file:

@[publish-repo](code/production.sbt)

Then in the Play console, use the `publish` task:

```bash
[my-first-app] $ publish
```

> Check the [sbt documentation](http://www.scala-sbt.org/release/docs/index.html) to get more information about the resolvers and credentials definition.

## Running a production server in place

In some circumstances, you may not want to create a full distribution, you may in fact want to run your application from your project's source directory.  This requires an sbt or activator installation on the server, and can be done using the `stage` task.

```bash
$ activator clean stage
```

[[images/stage.png]]

This cleans and compiles your application, retrieves the required dependencies and copies them to the `target/universal/stage` directory. It also creates a `bin/<start>` script where `<start>` is the project's name. The script runs the Play server on Unix style systems and there is also a corresponding `bat` file for Windows.

For example to start an application of the project `my-first-app` from the project folder you can:

```bash
$ target/universal/stage/bin/my-first-app -Dplay.crypto.secret=abcdefghijk
```

You can also specify a different configuration file for a production environment, from the command line:

```bash
$ target/universal/stage/bin/my-first-app -Dconfig.file=/full/path/to/conf/application-prod.conf
```

### Running a test instance

Play provides a convenient utility for running a test application in prod mode.

> This is not intended for production usage.

To run an application in prod mode, run `testProd`:

```bash
[my-first-app] $ testProd
```

## Using the SBT assembly plugin

Though not officially supported, the SBT assembly plugin may be used to package and run Play applications.  This will produce one jar as an output artifact, and allow you to execute it directly using the `java` command.

To use this, add a dependency on the plugin to your `project/plugins.sbt` file:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")
```

Now add the following configuration to your `build.sbt`:

@[assembly](code/assembly.sbt)

Now you can build the artifact by running `activator assembly`, and run your application by running:

```
$ java -jar target/scala-2.XX/<yourprojectname>-assembly-<version>.jar -Dplay.crypto.secret=abcdefghijk
```

You'll need to substitute in the right project name, version and scala version, of course.

