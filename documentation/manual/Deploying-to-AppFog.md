AppFog is based off the the Cloud Foundry open source project.  The [Cloud Foundry deployment instructions](https://github.com/playframework/Play20/wiki/Deploying-CloudFoundry) are more detailed and will work with AppFog as well.  Just replace "vmc" with "af".

## Prerequisites

1. Sign up for a free [AppFog](http://appfog.com/) account

2. Install the AppFog command line tool (version 0.3.18 or higher required) by using the following command:

```bash
gem install af
```

2. Login to AppFog on the console:

```bash
af login
```

## Build your Application
Package your app by typing the 'dist' command in the play prompt.

## Deploy your Application
Deploy the created zip file to AppFog with the af push command.

```bash
yourapp$ af push --path=dist/yourapp-1.0.zip
Application Name: yourapp
Detected a Play Framework Application, is this correct? [Yn]:
1: AWS US East - Virginia
2: AWS EU West - Ireland
3: AWS Asia SE - Singapore
4: Rackspace AZ 1 - Dallas
5: HP AZ 2 - Las Vegas
Select Infrastructure: 5
Application Deployed URL [yourapp.hp.af.cm]:
Memory reservation (128M, 256M, 512M, 1G, 2G) [256M]:
How many instances? [1]:
Create services to bind to 'yourapp'? [yN]:
Would you like to save this configuration? [yN]:
Creating Application: OK
Uploading Application:
  Checking for available resources: OK
  Processing resources: OK
  Packing application: OK
  Uploading (5M): OK
Push Status: OK
Staging Application 'yourapp': OK
Starting Application 'yourapp': OK
```
