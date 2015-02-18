<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Deploying to CloudFoundry / AppFog

## CloudFoundry vs. AppFog

Deploying to [AppFog](https://www.appfog.com/) can be accomplished by following the [Cloud Foundry](http://cloudfoundry.com) instructions below except run `af` instead of `vmc`. Also, with AppFog you need to follow an [extra step of adding an extra jar to the deployment zip file](https://docs.appfog.com/languages/java/play).

## Prerequisites

Sign up for a free [Cloud Foundry](http://cloudfoundry.com) account and install or update the Cloud Foundry command line tool, VMC, to the latest version (0.3.18 or higher) by using the following command:

```bash
gem install vmc
```

## Build your Application

Package your app by typing the `dist` command in the play prompt.

## Deploy your Application

Deploy the created zip file to Cloud Foundry with the VMC push command.  If you choose to create a database service, Cloud Foundry will automatically apply your database evolutions on application start.

```bash
yourapp$ vmc push --path=dist/yourapp-1.0.zip
Application Name: yourapp
Detected a Play Framework Application, is this correct? [Yn]:
Application Deployed URL [yourapp.cloudfoundry.com]:
Memory reservation (128M, 256M, 512M, 1G, 2G) [256M]:
How many instances? [1]:
Create services to bind to 'yourapp'? [yN]: y
1: mongodb
2: mysql
3: postgresql
4: rabbitmq
5: redis
What kind of service?: 3
Specify the name of the service [postgresql-38199]: your-db
Create another? [yN]:
Would you like to save this configuration? [yN]: y
Manifest written to manifest.yml.
Creating Application: OK
Creating Service [your-db]: OK
Binding Service [your-db]: OK
Uploading Application:
  Checking for available resources: OK
  Processing resources: OK
  Packing application: OK
  Uploading (186K): OK
Push Status: OK
Staging Application 'yourapp': OK
Starting Application 'yourapp': OK
```

## Working With Services

### Auto-Reconfiguration
Cloud Foundry uses a mechanism called auto-reconfiguration to automatically connect your Play application to a relational database service. If a single database configuration is found in the Play configuration (for example, `default`) and a single database service instance is bound to the application, Cloud Foundry will automatically override the connection properties in the configuration to point to the PostgreSQL or MySQL service bound to the application.

This is a great way to get simple apps up and running quickly. However, it is quite possible that your application will contain SQL that is specific to the type of database you are using.  In these cases, or if your app needs to bind to multiple services, you may choose to avoid auto-reconfiguration and explicitly specify the service connection properties.

### Connecting to Cloud Foundry Services
As always, Cloud Foundry provides all of your service connection information to your application in JSON format through the VCAP_SERVICES environment variable. However, connection information is also available as series of properties you can use in your Play configuration. Here is an example of connecting to a PostgreSQL service named `tasks-db` from within an application.conf file:

```bash
db.default.driver=${?cloud.services.tasks-db.connection.driver}
db.default.url=${?cloud.services.tasks-db.connection.url}
db.default.password=${?cloud.services.tasks-db.connection.password}
db.default.user=${?cloud.services.tasks-db.connection.username}
```

This information is available for all types of services, including NoSQL and messaging services. Also, if there is only a single service of a type (e.g. postgresql), you can refer to that service only by type instead of specifically by name, as exemplified below:

```bash
db.default.driver=${?cloud.services.postgresql.connection.driver}
db.default.url=${?cloud.services.postgresql.connection.url}
db.default.password=${?cloud.services.postgresql.connection.password}
db.default.user=${?cloud.services.postgresql.connection.username}
```
We recommend keeping these properties in a separate file (for example `cloud.conf`) and then including them only when building a distribution for Cloud Foundry. You can specify an alternative config file to `play dist` by using `-Dconfig.file`.

### Opting out of Auto-Reconfiguration
If you use the properties referenced above, you will automatically be opted-out. To explicitly opt out, include a file named “cloudfoundry.properties” in your application’s conf directory, and add the entry `autoconfig=false`
