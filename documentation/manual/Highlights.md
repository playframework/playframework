# What's new in Play 2.1?

Besides fixing more than 120 tickets, play 2.1 contains many new exciting features. This page contains some information about them.

## dependency updates

play, among other things, now depends on the latest version of ```scala (2.10)```, ```sbt (0.12.1)``` and ```netty (3.5.9)```.



## routes in subprojects and modularization

a play application now can include subprojects with route files. Furthermore, the main artifact was broken into 15 subprojects. These two powerful features allow developers to modularize their apps and fully control the classpath.

An example project that's utilizing these new features can be found [[here | ]].
 

## support for `scala.concurrent.Future`

As of 2.1, play is using the new universal `scala.concurrent.Future` API.

## JSON validation and navigation

[[How to use the new JSON validator |http://mandubian.com/2012/09/08/unveiling-play-2-dot-1-json-api-part1-jspath-reads-combinators/]]

## improved error reporting in console mode

in previous versions of play, certain routes and template errors generated console messages that were hard to parse. In 2.1 these exceptions were mapped backed to the source files.

## improved iteratee API

[[About the new iteratee API |http://mandubian.com/2012/08/27/understanding-play2-iteratees-for-normal-humans/]]


## filters and CRSF protection 

The new [[Filter API| https://github.com/playframework/Play20/tree/master/framework/src/play/src/main/scala/play/api/mvc/Filters.scala]] makes it super easy to implement cross cutting concerns. [[CRSF protection|https://github.com/playframework/Play20/tree/master/framework/src/play-filters-helpers/src/main/scala/csrf.scala]] was implemented based on the Filter API.


## new test helpers

[[A set|https://github.com/playframework/Play20/tree/master/framework/test/integrationtest/test]] of new test helpers were introduced for Spec and JUnit. Using these convenience methods one can write native looking specs and junit test cases.

## injectable controllers

[[Managing Controller Class Instantiation | JavaInjection]]

## RequireJS

[[How to use RequireJS | RequireJS-support]]


