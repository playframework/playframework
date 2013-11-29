<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Deploying to Heroku

[Heroku](https://www.heroku.com/) is a cloud application platform – a way of building and deploying web apps.

To get started:

1. [Install the Heroku Toolbelt](https://toolbelt.heroku.com)
2. [Sign up for a Heroku account](https://id.heroku.com/signup)


## Store your application in git

```bash
$ git init
$ git add .
$ git commit -m "init"
```


## Create a new application on Heroku

```bash
$ heroku create
Creating warm-frost-1289... done, stack is cedar
http://warm-1289.herokuapp.com/ | git@heroku.com:warm-1289.git
Git remote heroku added
```

This provisions a new application with an HTTP (and HTTPS) endpoint and Git endpoint for your application.  The Git endpoint is set as a new remote named `heroku` in your Git repository's configuration.


## Deploy your application

To deploy your application on Heroku, just use git to push it into the `heroku` remote repository:

```bash
$ git push heroku master
Counting objects: 34, done.
Delta compression using up to 8 threads.
Compressing objects: 100% (20/20), done.
Writing objects: 100% (34/34), 35.45 KiB, done.
Total 34 (delta 0), reused 0 (delta 0)

-----> Heroku receiving push
-----> Scala app detected
-----> Building app with sbt v0.11.0
-----> Running: sbt clean compile stage
       ...
-----> Discovering process types
       Procfile declares types -> web
-----> Compiled slug size is 46.3MB
-----> Launching... done, v5
       http://8044.herokuapp.com deployed to Heroku

To git@heroku.com:floating-lightning-8044.git
* [new branch]      master -> master
```

Heroku will run `sbt clean stage` to prepare your application. On the first deployment, all dependencies will be downloaded, which takes a while to complete (but will be cached for future deployments).


## Check that your application has been deployed

Now, let’s check the state of the application’s processes:

```bash
$ heroku ps
Process       State               Command
------------  ------------------  ----------------------
web.1         up for 10s          target/universal/stage/bin/myapp 
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
$ heroku logs -t --app floating-lightning-8044
2011-08-18T00:13:41+00:00 heroku[web.1]: Starting process with command `target/start`
2011-08-18T00:14:18+00:00 app[web.1]: Starting on port:28328
2011-08-18T00:14:18+00:00 app[web.1]: Started.
2011-08-18T00:14:19+00:00 heroku[web.1]: State changed from starting to up
...
```

Looks good. We can now visit the app by running:

```bash
$ heroku open
```


## Connecting to a database

Heroku provides a number of relational and NoSQL databases through [Heroku Add-ons](https://addons.heroku.com).  Play applications on Heroku are automatically provisioned a [Heroku Postgres](https://addons.heroku.com/heroku-postgresql) database.  To configure your Play application to use the Heroku Postgres database, first add the PostgreSQL JDBC driver to your application dependencies (`build.sbt`):

```scala
libraryDependencies += "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
```

Then create a new file in your project's root directory named `Procfile` (with a capital "P") that contains the following (substituting the `myapp` with your project's name):

```txt
web: target/universal/stage/bin/retailos -Dhttp.port=${PORT} -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL}
```

This instructs Heroku that for the process named `web` it will run Play and override the `applyEvolutions.default`, `db.default.driver`, and `db.default.url` configuration parameters.  Note that the `Procfile` command can be maximum 255 characters long.  Alternatively, use the `-Dconfig.resource=` or `-Dconfig.file=` mentioned in [[production configuration|ProductionConfiguration]] page.
Note that the creation of a Procfile is not actually required by Heroku, as Heroku will look in your play application's conf directory for an application.conf file in order to determine that it is a play application.

## Further learning resources

* [Play Tutorial for Java](https://github.com/jamesward/play2torial/blob/master/JAVA.md)
* [Getting Started with Play, Scala, and Squeryl](http://www.artima.com/articles/play2_scala_squeryl.html)
* [Edge Caching With Play, Heroku, and CloudFront](http://www.jamesward.com/2012/08/08/edge-caching-with-play2-heroku-cloudfront)
* [Optimizing Play for Database-Driven Apps](http://www.jamesward.com/2012/06/25/optimizing-play-2-for-database-driven-apps)
* [Play Scala Console on Heroku](http://www.jamesward.com/2012/06/11/play-2-scala-console-on-heroku)
* [Play App with a Scheduled Job on Heroku](https://github.com/jamesward/play2-scheduled-job-demo)
* [Using Amazon S3 for File Uploads with Java and Play](https://devcenter.heroku.com/articles/using-amazon-s3-for-file-uploads-with-java-and-play-2)
