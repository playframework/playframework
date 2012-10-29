# Deploying to Cloudfoundry (Preview)
[Cloud Foundry](http://cloudfoundry.com/) is a cloud application platform - Deploy and scale applications in seconds, without locking yourself into a single cloud.

## Prerequisite
Before you can deploy your app in cloudfoundry.com, you need to **open an account** with cloudfoundry.com. After registering it takes a day or two to get your account activated. 

You also have to have either the vmc command line tool or STS (eclipse plugin) installed on your system - visit the [getting started](http://docs.cloudfoundry.com/getting-started.html) link and follow the instructions there.

Once you have set-up the pre-requisites and got your user name and password from cloudfoundry.com - you are all set to deploy your app on cloudfoundry.com Paas.

## Deploying
Next we will bundle the application : `play dist`

We want to push the application contents as folder structure and not as zip so that we want to be able to push deltas later on. Hence open the folder myPlayApp/dist in your explorer and extract the contents inside 'myPlayApp-1.0-SNAPSHOT.zip' in the same folder.

Delete the myPlayApp-1.0-SNAPSHOT.zip. Go inside the myPlayApp-1.0-SNAPSHOT folder and delete the README and start files as well - we do not need them.

Now we are ready to push our content to cloudfoundry. Let's login to cloudfoundry first.

```bash
$ cd dist/myPlayApp-1.0-SNAPSHOT/
$ vmc login
```

> Attempting login to [http://api.cloudfoundry.com] <br />
> Email: ratul75@hotmail.com <br />
> Password: ****************** <br />
> Successfully logged into [http://api.cloudfoundry.com]<br />

```bash
$ vmc target api.cloudfoundry.com
```

> Successfully targeted to [http://api.cloudfoundry.com]

When prompted by vmc, answer with y or n as shown below.

```bash
$ vmc push
```

> Would you like to deploy from the current directory? [Yn]: **y**<br />
> Application Name: **myPlayApp**<br />
> Detected a Standalone Application, is this correct? [Yn]: **y**<br />
> 1: java<br />
> 2: node<br />
> 3: node06<br />
> 4: ruby18<br />
> 5: ruby19<br />
> Select Runtime [java]: **1**<br />
> Selected java<br />
> Start Command: **java $JAVA_OPTS -Dhttp.port=$VCAP_APP_PORT -cp "\`dirname \`/lib/*" play.core.server.NettyServer \`dirname \`**<br />
> Application Deployed URL [None]: **myPlayApp.${target-base}**    <br />
> Memory reservation (128M, 256M, 512M, 1G, 2G) [512M]: **256M**<br />
> How many instances? [1]: **1**<br />
> Create services to bind to 'myPlayApp'? [yN]: **n**<br />
> Would you like to save this configuration? [yN]: **y**<br />
> Manifest written to manifest.yml.<br />
> Creating Application: OK<br />
> Uploading Application:<br />
>   Checking for available resources: OK<br />
>   Processing resources: OK<br />
>   Packing application: OK<br />
>   Uploading (80K): OK   <br />
> Push Status: OK<br />
> Staging Application 'myPlayApp': OK<br />
> Starting Application 'myPlayApp': OK<br />


## Extract data from cloudfoundry
In your application you can fetch datas from cloudfoundry through the environment variables :

```java
System.getenv("VCAP_APP_PORT")
System.getenv("VCAP_APP_HOST")
```