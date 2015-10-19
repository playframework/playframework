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
remote: -----> Priming Ivy cache (Scala-2.11, Play-2.4)... done
remote: -----> Running: sbt compile stage
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

Heroku will run `sbt stage` to prepare your application. On the first deployment, all dependencies will be downloaded, which takes a while to complete (but they will be cached for future deployments).

If you are using RequireJS and you find that your application hangs at this step:

```bash
[info] Optimizing JavaScript with RequireJS
```

Then try following the steps in the [Using Node.js to Perform JavaScript Optimization for Play and Scala Applications](https://devcenter.heroku.com/articles/using-node-js-to-perform-javascript-optimization-for-play-and-scala-applications) on the Heroku Dev Center. This will greatly improve the performance of the Javascript engine.

### Check that your application has been deployed

Now, let’s check the state of the application’s processes:

```bash
$ heroku ps
=== web (Free): `target/universal/stage/bin/sample-app -Dhttp.port=${PORT}`
web.1: up 2015/01/09 11:27:51 (~ 4m ago)
```

The web process is up and running.  We can view the logs to get more information:

```bash
$ heroku logs
2015-07-13T20:44:47.358320+00:00 heroku[web.1]: Starting process with command `target/universal/stage/bin/myapp -Dhttp.port=${PORT}`
2015-07-13T20:44:49.750860+00:00 app[web.1]: Picked up JAVA_TOOL_OPTIONS: -Xmx384m -Xss512k -Dfile.encoding=UTF-8
2015-07-13T20:44:52.297033+00:00 app[web.1]: [warn] application - Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.
2015-07-13T20:44:54.960105+00:00 app[web.1]: [info] p.a.l.c.ActorSystemProvider - Starting application default Akka system: application
2015-07-13T20:44:55.066582+00:00 app[web.1]: [info] play.api.Play$ - Application started (Prod)
2015-07-13T20:44:55.445021+00:00 heroku[web.1]: State changed from starting to up
2015-07-13T20:44:55.330940+00:00 app[web.1]: [info] p.c.s.NettyServer$ - Listening for HTTP on /0:0:0:0:0:0:0:0:8626
...
```

We can also tail the logs as we would for a regular file.  This is useful for debugging:

```bash
$ heroku logs -t --app warm-frost-1289
2015-07-13T20:44:47.358320+00:00 heroku[web.1]: Starting process with command `target/universal/stage/bin/myapp -Dhttp.port=${PORT}`
2015-07-13T20:44:49.750860+00:00 app[web.1]: Picked up JAVA_TOOL_OPTIONS: -Xmx384m -Xss512k -Dfile.encoding=UTF-8
2015-07-13T20:44:52.297033+00:00 app[web.1]: [warn] application - Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.
2015-07-13T20:44:54.960105+00:00 app[web.1]: [info] p.a.l.c.ActorSystemProvider - Starting application default Akka system: application
2015-07-13T20:44:55.066582+00:00 app[web.1]: [info] play.api.Play$ - Application started (Prod)
2015-07-13T20:44:55.445021+00:00 heroku[web.1]: State changed from starting to up
2015-07-13T20:44:55.330940+00:00 app[web.1]: [info] p.c.s.NettyServer$ - Listening for HTTP on /0:0:0:0:0:0:0:0:8626
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
addSbtPlugin("com.heroku" % "sbt-heroku" % "0.5.1")
```

Next, we must configure the name of the Heroku application the plugin will deploy to. But first, create a new app. Install the Heroku Toolbelt and run the create command.

```bash
$ heroku create
Creating obscure-sierra-7788... done, stack is cedar-14
http://obscure-sierra-7788.herokuapp.com/ | git@heroku.com:obscure-sierra-7788.git
```

Now add something like this to your `build.sbt`, but replace “obscure-sierra-7788” with the name of the application you created (or you can skip this if you are using Git locally).

```scala
herokuAppName in Compile := "obscure-sierra-7788"
```

The sbt-heroku project's documentation contains details on [configuring the execution of the plugin](https://github.com/heroku/sbt-heroku#configuring-the-plugin).

### Deploying with the plugin

With the plugin added, you can deploy to Heroku by running this command:

```bash
$  sbt stage deployHeroku
...
[info] -----> Packaging application...
[info]        - app: obscure-sierra-7788
[info]        - including: target/universal/stage/
[info] -----> Creating build...
[info]        - file: target/heroku/slug.tgz
[info]        - size: 30MB
[info] -----> Uploading slug... (100%)
[info]        - success
[info] -----> Deploying...
[info] remote:
[info] remote: -----> Fetching custom tar buildpack... done
[info] remote: -----> sbt-heroku app detected
[info] remote: -----> Installing OpenJDK 1.8... done
[info] remote: -----> Discovering process types
[info] remote:        Procfile declares types -> console, web
[info] remote:
[info] remote: -----> Compressing... done, 78.9MB
[info] remote: -----> Launching... done, v6
[info] remote:        https://obscure-sierra-7788.herokuapp.com/ deployed to Heroku
[info] remote:
[info] -----> Done
[success] Total time: 90 s, completed Aug 29, 2014 3:36:43 PM
```

And you can visit your application by running this command:

```bash
$ heroku open -a obscure-sierra-7788
```

You can see the logs for your application by running this command:

```bash
$ heroku logs -a obscure-sierra-7788
```

Note that if you are using Git, you can omit the `-a` option above as the app
name will be detected from the Git remote that was added to your config when you
ran `heroku create`.

## Connecting to a database

Heroku provides a number of relational and NoSQL databases through [Heroku Add-ons](https://addons.heroku.com).  Play applications on Heroku are automatically provisioned with a [Heroku Postgres](https://addons.heroku.com/heroku-postgresql) database.  To configure your Play application to use the Heroku Postgres database, first add the PostgreSQL JDBC driver to your application dependencies (`build.sbt`):

```scala
libraryDependencies += "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
```

Then create a new file in your project's root directory named `Procfile` (with a capital "P") that contains the following (substituting the `myapp` with your project's name):

```txt
web: target/universal/stage/bin/myapp -Dhttp.port=${PORT} -Dplay.evolutions.db.default.autoApply=true -Ddb.default.url=${DATABASE_URL}
```

This instructs Heroku that for the process named `web` it will run Play and override the `play.evolutions.db.default.autoApply`, `db.default.driver`, and `db.default.url` configuration parameters.  Note that the `Procfile` command can be maximum 255 characters long.  Alternatively, use the `-Dconfig.resource=` or `-Dconfig.file=` mentioned in [[production configuration|ProductionConfiguration]] page.

Also, be aware the the `DATABASE_URL` is in the platform independent format:

```text
vendor://username:password@host:port/db
```

Play will automatically convert this into a JDBC URL for you if you are using one
of the built in database connection pools. But other database libraries and
frameworks, such as Slick or Hibernate, may not support this format natively.
If that's the case, you may try using the experimental `JDBC_DATABASE_URL` in
place of `DATABASE_URL` in the configuration like this:

```text
db.default.url=${?JDBC_DATABASE_URL}
db.default.username=${?JDBC_DATABASE_USERNAME}
db.default.password=${?JDBC_DATABASE_PASSWORD}
```

Note that the creation of a Procfile is not actually required by Heroku, as Heroku will look in your Play application's conf directory for an `application.conf` file in order to determine that it is a Play application.

## Further learning resources

* [Getting Started with Scala and Play on Heroku](https://devcenter.heroku.com/articles/getting-started-with-scala)
* [Deploying Scala and Play Applications with the Heroku sbt Plugin](https://devcenter.heroku.com/articles/deploying-scala-and-play-applications-with-the-heroku-sbt-plugin)
* [Using Node.js to Perform JavaScript Optimization for Play and Scala Applications](https://devcenter.heroku.com/articles/using-node-js-to-perform-javascript-optimization-for-play-and-scala-applications)
* [Deploy Scala and Play Applications to Heroku from Travis CI](https://devcenter.heroku.com/articles/deploy-scala-and-play-applications-to-heroku-from-travis-ci)
* [Deploy Scala and Play Applications to Heroku from Jenkins CI](https://devcenter.heroku.com/articles/deploy-scala-and-play-applications-to-heroku-from-jenkins-ci)
* [Running a Remote sbt Console for a Scala or Play Application](https://devcenter.heroku.com/articles/running-a-remote-sbt-console-for-a-scala-or-play-application)
* [Using WebSockets on Heroku with Java and the Play Framework](https://devcenter.heroku.com/articles/play-java-websockets)
* [Seed Project for Play and Heroku](https://github.com/jkutner/play-heroku-seed)
* [Play Tutorial for Java](https://github.com/jamesward/play2torial/blob/master/JAVA.md)
* [Getting Started with Play, Scala, and Squeryl](https://www.artima.com/articles/play2_scala_squeryl.html)
* [Edge Caching With Play, Heroku, and CloudFront](http://www.jamesward.com/2012/08/08/edge-caching-with-play2-heroku-cloudfront)
* [Optimizing Play for Database-Driven Apps](http://www.jamesward.com/2012/06/25/optimizing-play-2-for-database-driven-apps)
* [Play App with a Scheduled Job on Heroku](https://github.com/jamesward/play2-scheduled-job-demo)
* [Using Amazon S3 for File Uploads with Java and Play](https://devcenter.heroku.com/articles/using-amazon-s3-for-file-uploads-with-java-and-play-2)
