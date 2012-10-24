# Installing Play 2.0

## Prerequisites

To run the Play framework, you need [[JDK 6 or later| http://www.oracle.com/technetwork/java/javase/downloads/index.html]]. 

> If you are using MacOS, Java is built-in. If you are using Linux, make sure to use either the Sun JDK or OpenJDK (and not gcj, which is the default Java command on many Linux distros). If you are using Windows, just download and install the latest JDK package.

Be sure to have the `java` and `javac` commands in the current path (you can check this by typing `java -version` and `javac -version` at the shell prompt). 

## Download the binary package

Download the [[Play 2.0 binary package | http://download.playframework.org/releases/]] (take the latest 2.0 RC) and extract the archive to a location where you have both read **and write** access. (Running `play` writes some files to directories within the archive, so don't install to `/opt`, `/usr/local` or anywhere else you’d need special permission to write to.)

## Add the play script to your PATH

For convenience, you should add the framework installation directory to your system PATH. On UNIX systems, this means doing something like:

```bash
export PATH=$PATH:/path/to/play20
```

On Windows you’ll need to set it in the global environment variables. This means update the PATH in the environment variables and don't use a path with spaces.

> If you’re on UNIX, make sure that the `play` script is executable (otherwise do a `chmod a+x play`).

## Check that the play command is available

From a shell, launch the `play help` command. 

```bash
$ play help
```

If everything is properly installed, you should see the basic help:

[[images/play.png]]

> **Next:** [[Creating a new application | NewApplication]]