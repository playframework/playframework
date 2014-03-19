<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Testing your application

Writing tests for your application can be an involved process. Play offers integration with both ScalaTest and specs2 and provides helpers and application stubs to make testing your application as easy as possible.

## Overview

The location for tests is in the "test" folder.  There are sample test files created in the test folder which can be used as templates.

You can run tests from the Play console.

* To run all tests, run `test`.
* To run only one test class, run `test-only` followed by the name of the class i.e. `test-only my.namespace.MySpec`.
* To run only the tests that have failed, run `test-quick`.
* To run tests continually, run a command with a tilde in front, i.e. `~test-quick`.
* To access test helpers such as `FakeApplication` in console, run `test:console`.

Testing in Play is based on SBT, and a full description is available in the [testing SBT](http://www.scala-sbt.org/0.13.0/docs/Detailed-Topics/Testing) chapter.

For the details of using your preferred test framework with Play, see the pages on [[ScalaTest|http://www.scalatest.org]] or [[specs2|http://etorreborre.github.io/specs2/]].

* [[Testing your Application with ScalaTest|ScalaTest]]
* [[Testing your Application with specs2|Specs2]]
