<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Installing Play

## Prerequisites

You need to have a JDK 1.8 (or later) installed on your machine (see the **General Installation Tasks** for instructions).


## Install Activator

* **Download** the latest [Typesafe Activator](http://typesafe.com/activator).
* **Extract** the archive on a location where you have write access.
* **Open** a command window and enter.

```
cd activator*
activator ui
```

[http://localhost:8888](http://localhost:8888) - here you can access your local Play installation.

You'll find documentation and a list of application samples which get you going immediately. For a simple start, try the **play-java** sample.

### Command Line
To use play from any location on your file-system:
* Add the **activator** directory to your path (see General Installation Tasks).
* Create your first application from the command-line:

```
activator new my-first-app play-java
cd my-first-app
activator run
```

[http://localhost:9000](http://localhost:9000) - access your application here.

You are now ready to work with Play!

## General Installation Tasks

You may need to deal with those general tasks in order to install Play! on your system. 

### JDK installation

Verify if you have a JDK (Java Development Kit) Version 1.8 or later on your machine. Simply use those commands to verify:

```bash
java -version
javac -version
```

If you don't have the JDK, you have to install it:

* **MacOS**, Java is built-in.
* **Linux**, use either the latest Oracle JDK or OpenJDK (do not use not gcj). 
* **Windows** just download and install the latest JDK package.

http://www.oracle.com/technetwork/java/javase/downloads/index.html
 
### Add Executables to Path

For convenience, you should add the Activator installation directory to your system `PATH`. On UNIX systems, this means doing something like:

**Unix**

```bash
export PATH=/path/to/activator:$PATH
```

Make sure that the `activator` script is executable. If it's not, do a:
 
```bash
chmod u+x /path/to/activator
```

**Windows**

Add `;C:\path\to\activator` to your `PATH` environment variable. Don't use a path with spaces.

### File Permissions

Running `activator` writes some files to directories within the distribution, so don't install to `/opt`, `/usr/local` or anywhere else you’d need special permission to write to.

### Proxy Setup

If you're behind a proxy make sure to define it with `set HTTP_PROXY=http://<host>:<port>` on Windows or `export  HTTP_PROXY=http://<host>:<port>` on UNIX.

