# Creating a standalone version

It is also possible to create a fully packaged an self containend version of your application.

## Using the `dist` command

The `dist command creates a zip file containing  all the application dependencies including Play itself. It is a "ready to deploy" package that you can run on any server (the only dependency is a Java runtime).

```
[My first application] $ dist
```

It generates the zip file in the *dist* directory.

## Running the standalone application

The `dist` command generates a `start.sh` script launching the Java runtime with the proper options and classpath. Of course it is not required to use _this_ script and you can launch Java the plain old way if needed. 

Note that you can specify more Java options to this script:

```
$ ./start.sh -Dplay.http=80 -Dhttp.address=127.0.0.1
``` 