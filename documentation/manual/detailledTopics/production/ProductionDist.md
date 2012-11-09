# Creating a standalone version of your application

## Using the dist task

The simplest way to deploy a Play 2.0 application is to retrieve the source (typically via a git workflow) on the server and to use either `play start` or `play stage` to start it in place.

However, you sometimes need to build a binary version of your application and deploy it to the server without any dependencies on Play itself. You can do this with the `dist` task.

In the Play console, simply type `dist`:

```bash
[My first application] $ dist
```

[[images/dist.png]]

> one can easily use an external application.conf by using a special system property called ```conf.file```, so assuming your production ```application.conf``` is stored under your home directory, the following command should create a play distribution using the custom ```application.conf```:_ 
> ```bash
>  $ play -Dconfig.file=/home/peter/prod/application.conf dist 
> ```

This produces a ZIP file containing all JAR files needed to run your application in the `target` folder of your application, the ZIP file’s contents are organized as:

```
my-first-application-1.0
 └ lib
    └ *.jar
 └ start
```

You can use the generated `start` script to run your application.

Alternatively you can run `play dist` directly from your OS shell prompt, which does the same thing:

```bash
$ play dist
```

## Publishing to a Maven (or Ivy) repository

You can also publish your application to a Maven repository. This publishes both the JAR file containing your application and the corresponding POM file.

You have to configure the repository you want to publish to, in the `project/Build.scala` file:

```scala
val main = PlayProject(appName, appVersion, appDependencies).settings(
  
  publishTo := Some(
    "My resolver" at "http://mycompany.com/repo"
  )
  
  credentials += Credentials(
    "Repo", "http://mycompany.com/repo", "admin", "admin123"
  )
  
)
```

Then in the Play console, use the `publish` task:

```bash
[My first application] $ publish
```

> Check the sbt documentation to get more information about the resolvers and credentials definition.
