/**
 * From https://github.com/hydrasi/junit_xml_listener
 */

package eu.henkelmann.sbt

import _root_.sbt._

import java.io.{ StringWriter, PrintWriter, File }
import java.net.InetAddress
import scala.collection.mutable.ListBuffer
import scala.xml.{ Elem, Node, XML }
import sbt.testing.{ Event => TEvent, Status => TStatus, NestedSuiteSelector, NestedTestSelector, TestSelector }
import test.SbtOptionalThrowable

import play.console.Colors

/**
 * A tests listener that outputs the results it receives in junit xml
 * report format.
 * @param outputDir path to the dir in which a folder with results is generated
 */
class JUnitXmlTestsListener(val outputDir: String, logger: Logger) extends TestsListener {
  /**Current hostname so we know which machine executed the tests*/
  val hostname = InetAddress.getLocalHost.getHostName
  /**The dir in which we put all result files. Is equal to the given dir + "/test-reports"*/
  val targetDir = new File(outputDir + "/test-reports/")

  /**all system properties as XML*/
  val properties =
    <properties>
      {
        val iter = System.getProperties.entrySet.iterator
        val props: ListBuffer[Node] = new ListBuffer()
        while (iter.hasNext) {
          val next = iter.next
          props += <property name={ next.getKey.toString } value={ next.getValue.toString }/>
        }
        props
      }
    </properties>

  /**
   * Extract the test name from the TestEvent.
   *
   * I think there should be a nicer way to do this, but it doesn't look like SBT is going to give us that.
   */
  def testNameFromTestEvent(event: TEvent) = {
    def dropPrefix(s: String, prefix: String) = if (s.startsWith(prefix)) {
      s.drop(prefix.length)
    } else {
      s
    }

    event.selector() match {
      case test: TestSelector => dropPrefix(test.testName(), event.fullyQualifiedName() + ".")
      // I don't know if the events below are possible with JUnit, nor do I know exactly what to do with them.
      case test: NestedTestSelector => dropPrefix(test.testName(), event.fullyQualifiedName() + ".")
      case suite: NestedSuiteSelector => suite.suiteId()
      case _ => event.fullyQualifiedName()
    }
  }

  /**
   * Gathers data for one Test Suite. We map test groups to TestSuites.
   * Each TestSuite gets its own output file.
   */
  class TestSuite(val name: String) {
    val events: ListBuffer[TEvent] = new ListBuffer()
    val start = System.currentTimeMillis
    var end = System.currentTimeMillis

    if (name.endsWith("Test")) {
      logger.info(name)
    }

    /**Adds one test result to this suite.*/
    def addEvent(e: TEvent) = {
      events += e
      if (name.endsWith("Test")) {
        logEvent(e)
      }
    }

    def logEvent(event: TEvent) = {

      def logWith(color: String) = logger.info(color + " " + testNameFromTestEvent(event))

      event.status match {
        case TStatus.Error => logWith(Colors.red("!"))
        case TStatus.Failure => logWith(Colors.yellow("x"))
        case TStatus.Skipped => logWith(Colors.yellow("o"))
        case TStatus.Success => logWith(Colors.green("+"))
        case _ => ()
      }
    }

    /**
     * Returns a triplet with the number of errors, failures and the
     * total numbers of tests in this suite.
     */
    def count(): (Int, Int, Int) = {
      var errors, failures = 0
      for (e <- events) {
        e.status match {
          case TStatus.Error => errors += 1
          case TStatus.Failure => failures += 1
          case _ =>
        }
      }
      (errors, failures, events.size)
    }

    /**
     * Stops the time measuring and emits the XML for
     * All tests collected so far.
     */
    def stop(): Elem = {
      end = System.currentTimeMillis
      val duration = end - start

      val (errors, failures, tests) = count()

      if (name.endsWith("Test")) {
        logger.info("")
        logger.info("")
        logger.info("Total for test " + name)
        logger.info(Colors.blue("Finished in " + (duration / 1000.0).toString + " seconds"))
        logger.info(Colors.blue(tests + " tests, " + failures + " failures, " + errors + " errors"))
      }

      <testsuite hostname={ hostname } name={ name } tests={ tests.toString } errors={ errors.toString } failures={ failures.toString } time={ (duration / 1000.0).toString }>
        { properties }
        { events.map(e => formatTestCase(name, e)) }
        <system-out>
          <![CDATA[]]>
        </system-out>
        <system-err>
          <![CDATA[]]>
        </system-err>
      </testsuite>

    }
  }

  def formatTestCase(className: String, event: TEvent) = {
    val trace = event.throwable match {
      case SbtOptionalThrowable(throwable) => {
        val stringWriter = new StringWriter()
        val writer = new PrintWriter(stringWriter)
        throwable.printStackTrace(writer)
        writer.flush()
        stringWriter.toString
      }
      case _ => ""
    }
    <testcase classname={ className } name={ testNameFromTestEvent(event) } time={ "0.0" }>
      {
        (event.status, event.throwable) match {
          case (TStatus.Error, SbtOptionalThrowable(throwable)) =>
            <error message={ throwable.getMessage } type={ throwable.getClass.getName }>
              { trace }
            </error>

          case (TStatus.Error, _) =>
            <error message={ "No Exception or message provided" }/>

          case (TStatus.Failure, SbtOptionalThrowable(throwable)) =>
            <failure message={ throwable.getMessage } type={ throwable.getClass.getName }>
              { trace }
            </failure>

          case (TStatus.Failure, _) =>
            <failure message={ "No Exception or message provided" }/>

          case (TStatus.Skipped, _) =>
            <skipped/>

          case _ => {}
        }
      }
    </testcase>
  }

  /**The currently running test suite*/
  var testSuite: TestSuite = null

  /**Creates the output Dir*/
  override def doInit(): Unit = { targetDir.mkdirs() }

  /**
   * Starts a new, initially empty Suite with the given name.
   */
  override def startGroup(name: String) { testSuite = new TestSuite(name) }

  /**
   * Adds all details for the given even to the current suite.
   */
  override def testEvent(event: TestEvent): Unit = for (e <- event.detail) { testSuite.addEvent(e) }

  /**
   * called for each class or equivalent grouping
   *  We map one group to one Testsuite, so for each Group
   *  we create an XML like this:
   *  <?xml version="1.0" encoding="UTF-8" ?>
   *  <testsuite errors="x" failures="y" tests="z" hostname="example.com" name="eu.henkelmann.bla.SomeTest" time="0.23">
   *       <properties>
   *           <property name="os.name" value="Linux" />
   *           ...
   *       </properties>
   *       <testcase classname="eu.henkelmann.bla.SomeTest" name="testFooWorks" time="0.0" >
   *           <error message="the foo did not work" type="java.lang.NullPointerException">... stack ...</error>
   *       </testcase>
   *       <testcase classname="eu.henkelmann.bla.SomeTest" name="testBarThrowsException" time="0.0" />
   *       <testcase classname="eu.henkelmann.bla.SomeTest" name="testBaz" time="0.0">
   *           <failure message="the baz was no bar" type="junit.framework.AssertionFailedError">...stack...</failure>
   *        </testcase>
   *       <system-out><![CDATA[]]></system-out>
   *       <system-err><![CDATA[]]></system-err>
   *  </testsuite>
   *
   *  I don't know how to measure the time for each testcase, so it has to remain "0.0" for now :(
   */
  override def endGroup(name: String, t: Throwable) = {
    System.err.println("Throwable escaped the test run of '" + name + "': " + t)
    t.printStackTrace(System.err)
  }

  /**
   * Ends the current suite, wraps up the result and writes it to an XML file
   *  in the output folder that is named after the suite.
   */
  override def endGroup(name: String, result: TestResult.Value) = {
    if (name.endsWith("Test")) {
      XML.save(new File(targetDir, testSuite.name.split('.').takeRight(1).mkString + ".xml").getAbsolutePath, testSuite.stop(), "UTF-8", true, null)
    } else {
      testSuite.stop()
    }

  }

  /**Does nothing, as we write each file after a suite is done.*/
  override def doComplete(finalResult: TestResult.Value): Unit = {}

  /**Returns None*/
  override def contentLogger(test: TestDefinition): Option[ContentLogger] = None

}