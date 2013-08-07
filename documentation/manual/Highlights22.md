# What's new in Play 2.2

## New results structure for Java and Scala

*TODO*

## Better control over buffering and keep alive

*TODO*

## New action composition and action builder methods

*TODO*

## Improved Java promise API

*TODO*

## Iteratee execution context passing

*TODO*

## sbt 0.13 support

Various usability and performance improvements. For more information please refer to the [sbt 0.13 release notes](http://www.scala-sbt.org/0.13.0/docs/Community/ChangeSummary_0.13.0.html)

## New stage and dist tasks

The _stage_ and _dist_ tasks have been completely overhauled in order to use the [Native Packager Plugin](https://github.com/sbt/sbt-native-packager).

The benefit in using the Native Packager is that many types of archive can now be supported in addition to regular zip files e.g. tar.gz, RPM, OS X disk images, Microsoft Installers (MSI) and more. In addition a Windows batch script is now provided for Play as well as a Unix one.

More information can be found in the [[Creating a standalone version of your application|ProductionDist]] document.