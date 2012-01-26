package play.api.test

import org.specs2.specification.SpecificationStructure
import java.io._
/**
 * Specs runner based on Specs2
 */
object SpecRunner {

  def main(args: Array[String]) {
    val classes = args.map { specName =>
      try {
        Class.forName(specName).newInstance().asInstanceOf[SpecificationStructure]
      } catch {
        case e: Exception =>
          println("could not create a specs2 specification for " + specName, " (perhaps spec is defined as an object instead of a class?)")
          throw e
      }
    }
    specs2.run(classes: _*)
    System.exit(0)
  }
}

/**
 * JUnit test runner
 */
object JunitRunner {
  def main(args: Array[String]) {
    val junit = new org.junit.runner.JUnitCore
    val classes = args.map(name => Class.forName(name))
    val consoleOutput = new org.junit.internal.TextListener(System.out)
    val xmlOutput = new play.api.test.JUnitXmlTestsListener("target")
    junit.addListener(consoleOutput)
    junit.addListener(xmlOutput)
    junit.run(classes: _*)
    System.exit(0)
  }
}