<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Installing Play

## Prerequisites

You need to have a JDK 1.8 (or later) installed on your machine (see [General Installation Tasks](#JDK-installation)).

## Quick Start

1. **Download** the latest [Typesafe Activator](https://typesafe.com/get-started).
2. **Extract** the archive on a location where you have write access.
3. **Change** dir with cmd `cd activator*` (or with the file-manager)
4. **Start** it with cmd `activator ui` (or with the file-manager)
5. **Access** it at [http://localhost:8888](http://localhost:8888)

You'll find documentation and a list of application samples which get you going immediately. For a simple start, try the **play-java** sample.


### Command Line

To use play from any location on your file-system, add the **activator** directory to your path (see [General Installation Tasks](#Add-Executables-to-Path)).

Creating `my-first-app` based on the `play-java` template is as simple as:

```bash
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

1. **MacOS**, Java is built-in, but you may have to [Update to the latest](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
2. **Linux**, use either the latest Oracle JDK or OpenJDK (do not use not gcj). 
3. **Windows** just download and install the [latest JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) package.


### Add Executables to Path

For convenience, you should add the Activator installation directory to your system `PATH`.

On **Unix**, use `export PATH=/path/to/activator:$PATH`

On **Windows**, add `;C:\path\to\activator` to your `PATH` environment variable. Do not use a path with spaces.

### File Permissions

#### Unix

Running `activator` writes some files to directories within the distribution, so don't install to `/opt`, `/usr/local` or anywhere else you’d need special permission to write to.

Make sure that the `activator` script is executable. If it's not, do a `chmod u+x /path/to/activator`.

### Proxy Setup

If you're behind a proxy make sure to define it with `set HTTP_PROXY=http://<host>:<port>` on Windows or `export  HTTP_PROXY=http://<host>:<port>` on UNIX.
