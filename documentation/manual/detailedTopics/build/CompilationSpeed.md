<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Improving Compilation Times

Compilation speed can be improved by following some guidelines that are also good engineering practice:

## Use subprojects/modularize

This is something like bulkheads for incremental compilation in addition to the other benefits of modularization. It minimizes the size of cycles, makes inter-dependencies explicit, and allows you to work with a subset of the code when desired. It also allows sbt to compile independent modules in parallel.

## Annotate return types of public methods

This makes compilation faster as it reduces the need for type inference and for accuracy helps address corner cases in incremental compilation arising from inference across source file boundaries.

## Avoid large cycles between source files

Cycles tend to result in larger recompilations and/or more steps.  In sbt 0.13.0+ (Play 2.2+), this is less of a problem.

## Minimize inheritance

A public API change in a source file typically requires recompiling all descendents.
