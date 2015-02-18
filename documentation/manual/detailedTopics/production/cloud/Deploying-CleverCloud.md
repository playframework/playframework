<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Deploying to Clever Cloud
[Clever Cloud](https://www.clever-cloud.com/en/) is a Platform as a Service solution. You can deploy on it Scala, Java, PHP, Python and Node.js applications. Its main particularity is that it supports **automatic vertical and horizontal scaling**.

Clever Cloud supports Play! 2 applications natively. The present guide explains how to deploy your application on Clever Cloud.

## Create a new application on Clever Cloud

Create your Play! application on Clever Cloud [dashboard](https://console.clever-cloud.com).

## Deploy your application

To deploy your application on Clever Cloud, just use git to push your code to the application remote repository.


```bash
$ git remote add <your-remote-name> <your-git-deployment-url>
$ git push <your-remote-name> master
```

**Important tip: do not forget to push to the remote master branch.**

If you work in a different branch, just use: 

```bash
$ git remote add <your-remote-name> <your-git-deployment-url>
$ git push <your-remote-name> <your-branch-name>:master
```

<br/>
Clever Cloud will run `sbt update stage` to prepare your application. On the first deployment, all dependencies will be downloaded, which takes a while to complete (but will be cached for future deployments).


## Check the deployment of your application

You can check the deployment of your application by visiting the ***logs*** section of your application in the dashboard.


## [Optional] Configure your application
You can custom your application with a `clevercloud/play.json` file.

The file must contain the following fields:

```javascript
{
    "deploy": {
        "goal": <string>
    }
}
```

That field can contain additional configuration like:

`"-Dconfig.resource=clevercloud.conf"`, `"-Dplay.version=2.0.4"` or `"-Dplay.modules.evolutions.autoApply=true"`.

## Connecting to a database

Just go to the ***Services*** section in the Clever Cloud dashboard to add the database you need: MySQL, PostgreSQL or Couchbase.

As in every Play! 2 application, the only file you have to modify is your `conf/application.conf` file.

**Example: setup MySQL database**

```
db.default.url="jdbc:mysql://{yourcleverdbhost}/{dbname}"
db.default.driver=com.mysql.jdbc.Driver
db.default.user={yourcleveruser}
db.default.password={yourcleverpass}
```

## Further information
If you need further information, just check our complete [documentation](http://doc.clever-cloud.com/java/play-framework-2/).
