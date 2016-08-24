<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Deploying to Scalingo

[Scalingo](https://scalingo.com/) is a Platform as a Service hosting solution –
it is technology agnostic, your can deploy Scala, Java as well as Ruby or
Node.JS apps.

The platform supports Play! apps natively, you have nothing to add to your project and
here is to setup your app on the platform.

In this guide, all the operations can be done through Scalingo web dashboard or with its
command line tool (it's to your liking).

To get started:

* [Create a Scalingo account](https://scalingo.com)
* [Install the Scalingo CLI](http://cli.scalingo.com) _(optional)_

Scalingo is using GIT as main deployment gateway, you need to create a repository for your
app (if it's not already done)

## Create and store your application in git

```bash
$ git init
$ git add .
$ git commit -m "init"
```

## Create a new application on Scalingo

### Dashboard

Click on the '+' button to create a new application and follow the wizard.

### CLI

```bash
$ scalingo create play-app-name
App 'play-app-name' has been created
Git repository detected: remote scalingo added
→ 'git push scalingo master' to deploy your app
```

This command create a new application named 'play-app-name', configured with a
new remote named `scalingo` in your Git repository's configuration

## Deploy your application

To deploy your application on Scalingo, use Git to push it into the `scalingo`
remote repository:

```bash
$ git push scalingo master
Counting objects: 93, done.
Delta compression using up to 4 threads.
Compressing objects: 100% (84/84), done.
Writing objects: 100% (93/93), 1017.92 KiB | 0 bytes/s, done.
Total 93 (delta 38), reused 0 (delta 0)
<-- Start deployment of play-app-name -->
-----> Installing OpenJDK 1.8... done
-----> Running: sbt compile stage
       ...
       [success] Total time: 1 s, completed Aug 24, 2016 11:31:43 PM
-----> Dropping ivy cache from the slug
-----> Dropping sbt boot dir from the slug
-----> Dropping compilation artifacts from the slug

Build complete, shipping your container...
Waiting for your application to boot...
<-- https://play-app-name.scalingo.io -->

To git@scalingo.com:play-app-name.git
* [new branch]      master -> master
```

Scalingo is using the same technology as Heroku to deploy your app: [a
buildpack](https://github.com/Scalingo/scala-buildpack) It will run `sbt stage`
to prepare your application. On the first deployment, all dependencies will be
downloaded, which takes a while to complete (but they will be cached for future
deployments).

## Check that your application has been deployed

Now, let’s check the state of the application’s processes:

```bash
$ scalingo --app play-app-name ps
+------+--------+------+-----------------------------------------------------------+
| NAME | AMOUNT | SIZE | COMMAND                                                   |
+------+--------+------+-----------------------------------------------------------+
| web  | 1      | M    | target/universal/stage/bin/sample-app -Dhttp.port=${PORT} |
+------+--------+------+-----------------------------------------------------------+
```

The web process is up and running. We can view the logs to get more information
(from the dashboard in the 'Logs' section or with the CLI):

```bash
$ scalingo --app play-app-name logs
2016-08-24 11:35:33.030374238 +0200 CEST [web-1] Picked up JAVA_TOOL_OPTIONS: -Xmx384m -Xss512k -Dfile.encoding=UTF-8
2016-08-24 11:35:33.281760824 +0200 CEST [web-1] [warn] application - Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.
2016-08-24 11:35:33.427678464 +0200 CEST [web-1] [info] p.a.l.c.ActorSystemProvider - Starting application default Akka system: application
2016-08-24 11:35:33.639106705 +0200 CEST [web-1] [info] play.api.Play$ - Application started (Prod)
...
```

We can also tail the logs as we would for a regular file.  This is useful for debugging:

```bash
$ scalingo --app play-app-name logs -f
...
```
## Connecting to a database

The Scalingo platform provides multiple database types, relational or not, we
encourage you to use the PostgreSQL addon for your play app. Add it from the
'Addons' section of the dashboard or with the following command:

```bash
$ scalingo --app play-app-name addons-add scalingo-postgresql free
```

To configure your Play application, you need to install the PostgreSQL JDBC
driver by adding the dependency in your `build.sbt` file:

```scala
libraryDependencies += "org.postgresql" % "postgresql" % "9.4-1201-jdbc41"
```

Then create a new file in your project's root directory named `Procfile` (with
a capital "P") that contains the following (substituting the `myapp` with your
project's name):

```txt
web: target/universal/stage/bin/myapp -Dhttp.port=${PORT} -Dplay.evolutions.db.default.autoApply=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL}
```

This instructs Scalingo that for the process named `web` it will run Play and
override the `play.evolutions.db.default.autoApply`, `db.default.driver`, and
`db.default.url` configuration parameters.  Note that the `Procfile` command
can be maximum 255 characters long.  Alternatively, use the
`-Dconfig.resource=` or `-Dconfig.file=` mentioned in [[production
configuration|ProductionConfiguration]] page.

Also, be aware the `DATABASE_URL` is in the platform independent format:

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

Note that the creation of a Procfile is not actually required by Scalingo, as
Scalingo will look in your Play application's conf directory for an
`application.conf` file in order to determine that it is a Play application.

## Further learning resources

* [Scala documentation](http://doc.scalingo.com/languages/scala/)
