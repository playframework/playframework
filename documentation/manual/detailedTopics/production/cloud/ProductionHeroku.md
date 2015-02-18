<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Deploying to Heroku

[Heroku](https://www.heroku.com/) is a cloud application platform – a way of building and deploying web apps.

To get started:

1. [Install the Heroku Toolbelt](https://toolbelt.heroku.com)
2. [Sign up for a Heroku account](https://id.heroku.com/signup)

There are two methods of deployment to Heroku:

*  Pushing to a remote [Git repository](https://devcenter.heroku.com/articles/deploying-scala).
*  Using the [sbt-heroku plugin](https://devcenter.heroku.com/articles/deploying-scala-and-play-applications-with-the-heroku-sbt-plugin).

## Deploying to a remote Git repository

### Store your application in git

```bash
$ git init
$ git add .
$ git commit -m "init"
```

### Create a new application on Heroku

```bash
$ heroku create
Creating warm-frost-1289... done, stack is cedar-14
http://warm-frost-1289.herokuapp.com/ | git@heroku.com:warm-frost-1289.git
Git remote heroku added
```

This provisions a new application with an HTTP (and HTTPS) endpoint and Git endpoint for your application.  The Git endpoint is set as a new remote named `heroku` in your Git repository's configuration.

### Deploy your application

To deploy your application on Heroku, use Git to push it into the `heroku` remote repository:

```bash
$ git push heroku master
Counting objects: 93, done.
Delta compression using up to 4 threads.
Compressing objects: 100% (84/84), done.
Writing objects: 100% (93/93), 1017.92 KiB | 0 bytes/s, done.
Total 93 (delta 38), reused 0 (delta 0)
remote: Compressing source files... done.
remote: Building source:
remote:
remote: -----> Play 2.x - Scala app detected
remote: -----> Installing OpenJDK 1.8... done
remote: -----> Downloading SBT... done
remote: -----> Priming Ivy cache (Scala-2.11, Play-2.3)... done
remote: -----> Running: sbt update
...
remote: -----> Dropping ivy cache from the slug
remote: -----> Dropping sbt boot dir from the slug
remote: -----> Dropping compilation artifacts from the slug
remote: -----> Discovering process types
remote:        Procfile declares types -> web
remote:
remote: -----> Compressing... done, 93.3MB
remote: -----> Launching... done, v6
remote:        https://warm-frost-1289.herokuapp.com/ deployed to Heroku
remote:
remote: Verifying deploy... done.
To https://git.heroku.com/warm-frost-1289.git
* [new branch]      master -> master
```

Heroku will run `sbt clean stage` to prepare your application. On the first deployment, all dependencies will be downloaded, which takes a while to complete (but will be cached for future deployments).

### Check that your application has been deployed

Now, let’s check the state of the application’s processes:

```bash
$ heroku ps
=== web (1X): `target/universal/stage/bin/sample-app -Dhttp.port=${PORT}`
web.1: up 2015/01/09 11:27:51 (~ 4m ago)
```

The web process is up.  Review the logs for more information:

```bash
$ heroku logs
2011-08-18T00:13:41+00:00 heroku[web.1]: Starting process with command `target/universal/stage/bin/myapp`
2011-08-18T00:14:18+00:00 app[web.1]: Starting on port:28328
2011-08-18T00:14:18+00:00 app[web.1]: Started.
2011-08-18T00:14:19+00:00 heroku[web.1]: State changed from starting to up
...
```

We can also tail the logs in the same manner as we could do at a regular command line.  This is useful for debugging:

```bash
$ heroku logs -t --app warm-frost-1289
2011-08-18T00:13:41+00:00 heroku[web.1]: Starting process with command `target/universal/stage/bin/myapp`
2011-08-18T00:14:18+00:00 app[web.1]: Starting on port:28328
2011-08-18T00:14:18+00:00 app[web.1]: Started.
2011-08-18T00:14:19+00:00 heroku[web.1]: State changed from starting to up
...
```

Looks good. We can now visit the app by running:

```bash
$ heroku open
```

## Deploying with the sbt-heroku plugin

The Heroku sbt plugin utilizes an API to provide direct deployment of prepackaged standalone web applications to Heroku. This may be a preferred approach for applications that take a long time to compile, or that need to be deployed from a Continuous Integration server such as Travis CI or Jenkins.

### Adding the plugin

To include the plugin in your project, add the following to your `project/plugins.sbt` file:

```scala
resolvers += Resolver.url("heroku-sbt-plugin-releases", url("http://dl.bintray.com/heroku/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.heroku" % "sbt-heroku" % "0.3.0")
```

Next, we must configure the name of the Heroku application the plugin will deploy to. But first, create a new app. Install the Heroku Toolbelt and run the create command with the `-n` flag, which will prevent it from adding a Git remote.

```bash
$ heroku create -n
Creating obscure-sierra-7788... done, stack is cedar-14
http://obscure-sierra-7788.herokuapp.com/ | git@heroku.com:obscure-sierra-7788.git
```

Now add something like this to your `build.sbt`, but replace “obscure-sierra-7788” with the name of the application you created.

```scala
herokuAppName in Compile := "obscure-sierra-7788"
```

The sbt-heroku project's documentation contains details on [configuring the execution of the plugin](https://github.com/heroku/sbt-heroku#configuring-the-plugin).

### Deploying with the plugin

With the plugin added, you can deploy to Heroku by running this command:

```bash
$ sbt stage deployHeroku
...
[info] ---> Packaging application...
[info]      - including: ./target/universal/stage
[info] ---> Creating slug...
[info]      - file: ./target/heroku/slug.tgz
[info]      - size: 63MB
[info] ---> Uploading Slug...
[info]      - id: 73c1f7f2-75a4-4bb9-a3ce-e7ec2d70fa96
[info]      - stack: cedar-14
[info]      - process types: web
[info] ---> Releasing...
[info]      - version: 65
[success] Total time: 90 s, completed Aug 29, 2014 3:36:43 PM
```

And you can visit your application by running this command:

```bash
$ heroku open -a obscure-sierra-7788
```

You can see the logs for you application by running this command:

```bash
$ heroku logs -a obscure-sierra-7788
```

## Connecting to a database

Heroku provides a number of relational and NoSQL databases through [Heroku Add-ons](https://addons.heroku.com).  Play applications on Heroku are automatically provisioned a [Heroku Postgres](https://addons.heroku.com/heroku-postgresql) database.  To configure your Play application to use the Heroku Postgres database, first add the PostgreSQL JDBC driver to your application dependencies (`build.sbt`):

```scala
libraryDependencies += "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
```

Then create a new file in your project's root directory named `Procfile` (with a capital "P") that contains the following (substituting the `myapp` with your project's name):

```txt
web: target/universal/stage/bin/myapp -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL}
```

This instructs Heroku that for the process named `web` it will run Play and override the `applyEvolutions.default`, `db.default.driver`, and `db.default.url` configuration parameters.  Note that the `Procfile` command can be maximum 255 characters long.  Alternatively, use the `-Dconfig.resource=` or `-Dconfig.file=` mentioned in [[production configuration|ProductionConfiguration]] page.
Note that the creation of a Procfile is not actually required by Heroku, as Heroku will look in your play application's conf directory for an application.conf file in order to determine that it is a play application.

## Further learning resources

* [Getting Started with Scala on Heroku](https://devcenter.heroku.com/articles/getting-started-with-scala)
* [Deploying Scala and Play Applications with the Heroku sbt Plugin](https://devcenter.heroku.com/articles/deploying-scala-and-play-applications-with-the-heroku-sbt-plugin)
* [Deploy Scala and Play Applications to Heroku from Travis CI](https://devcenter.heroku.com/articles/deploy-scala-and-play-applications-to-heroku-from-travis-ci)
* [Deploy Scala and Play Applications to Heroku from Jenkins CI](https://devcenter.heroku.com/articles/deploy-scala-and-play-applications-to-heroku-from-jenkins-ci)
* [Running a Remote sbt Console for a Scala or Play Application](https://devcenter.heroku.com/articles/running-a-remote-sbt-console-for-a-scala-or-play-application)
* [Using WebSockets on Heroku with Java and the Play Framework](https://devcenter.heroku.com/articles/play-java-websockets)
* [Seed Project for Play and Heroku](https://github.com/jkutner/play-heroku-seed)
* [Play Tutorial for Java](https://github.com/jamesward/play2torial/blob/master/JAVA.md)
* [Getting Started with Play, Scala, and Squeryl](http://www.artima.com/articles/play2_scala_squeryl.html)
* [Edge Caching With Play, Heroku, and CloudFront](http://www.jamesward.com/2012/08/08/edge-caching-with-play2-heroku-cloudfront)
* [Optimizing Play for Database-Driven Apps](http://www.jamesward.com/2012/06/25/optimizing-play-2-for-database-driven-apps)
* [Play App with a Scheduled Job on Heroku](https://github.com/jamesward/play2-scheduled-job-demo)
* [Using Amazon S3 for File Uploads with Java and Play](https://devcenter.heroku.com/articles/using-amazon-s3-for-file-uploads-with-java-and-play-2)
