# Creating a standalone version of your application

## Using the dist task

The simplest way to deploy a Play application is to retrieve the source (typically via a git workflow) on the server and to use either `play start` or `play stage` to start it in place.

However, you sometimes need to build a binary version of your application and deploy it to the server without any dependency on Play itself. You can do this with the `dist` task.

In the Play console, simply type `dist`:

```bash
[My first application] $ dist
```

[[images/dist.png]]

This produces a ZIP file containing all JAR files needed to run your application in the `target/universal` folder of your application. Alternatively you can run `play dist` directly from your OS shell prompt, which does the same thing:

```bash
$ play dist
```

> For Windows users a start script will be produced with a .bat file extension. Use this file when running a Play application on Windows.
>
> For Unix users, zip files do not retain Unix file permissions so when the file is expanded the start script will be required to be set as an executable:
>
> ```bash
> $ chmod +x /path/to/bin/<project-name>
> ```
>
> Alternatively a tar.gz file can be produced instead. Tar files retain permissions. Invoke the `universal:package-zip-tarball` task instead of the `dist` task:
>
> ```bash
> play universal:package-zip-tarball
> ```

## The Native Packager

Play uses the [SBT Native Packager plugin](http://www.scala-sbt.org/sbt-native-packager/). The native packager plugin declares the `dist` task to create a zip file. Invoking the `dist` task is directly equivalent to invoking the following:

```bash
$ play universal:package-bin
```

Many other types of archive can be generated including:

* tar.gz
* OS X disk images
* Microsoft Installer (MSI)
* RPMs
* Debian files

Please consult the [documentation](http://www.scala-sbt.org/sbt-native-packager) on the native packager for more information.

## Publishing to a Maven (or Ivy) repository

You can also publish your application to a Maven repository. This publishes both the JAR file containing your application and the corresponding POM file.

You have to configure the repository you want to publish to, in your `build.sbt` file:

```scala
 publishTo := Some(
   "My resolver" at "http://mycompany.com/repo"
 ),
 
 credentials += Credentials(
   "Repo", "http://mycompany.com/repo", "admin", "admin123"
 )
```

Then in the Play console, use the `publish` task:

```bash
[My first application] $ publish
```

> Check the [sbt documentation](http://www.scala-sbt.org/release/docs/index.html) to get more information about the resolvers and credentials definition.

> **Next:** [[Production configuration|ProductionConfiguration]]