<<<<<<< HEAD:documentation/manual/scalaGuide/main/tests/code/HelloWorldSpec.scala
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests
=======
package scalaguide.tests.specs
>>>>>>> Organized code samples for test into scalatest and specs subdirectories of test/code. Adjusted links on page such to match and verified it with the validate-docs target.:documentation/manual/scalaGuide/main/tests/code/specs2/HelloWorldSpec.scala

// #scalatest-helloworldspec
import org.specs2.mutable._

class HelloWorldSpec extends Specification {

  "The 'Hello world' string" should {
    "contain 11 characters" in {
      "Hello world" must have size(11)
    }
    "start with 'Hello'" in {
      "Hello world" must startWith("Hello")
    }
    "end with 'world'" in {
      "Hello world" must endWith("world")
    }
  }
}
// #scalatest-helloworldspec
