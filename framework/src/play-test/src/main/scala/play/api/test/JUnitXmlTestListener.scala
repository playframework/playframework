/**
 * based on https://github.com/hydrasi/junit_xml_listener
 */

package play.api.test

import java.io.{ StringWriter, PrintWriter, File }
import java.net.InetAddress
import scala.collection.mutable.ListBuffer
import scala.xml.{ Elem, Node, XML }
import java.io.File
import java.text.NumberFormat

import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/**
 * A tests listener that outputs the results it receives in junit xml
 * report format.
 *  <testsuite failures="y" tests="z" hostname="example.com" name="eu.henkelmann.bla.SomeTest" time="0.23">
 *       <properties>
 *           <property name="os.name" value="Linux" />
 *           ...
 *       </properties>
 *       <testcase classname="eu.henkelmann.bla.SomeTest" name="testFooWorks" time="0.0" >
 *       <testcase classname="eu.henkelmann.bla.SomeTest" name="testBarThrowsException" time="0.0" />
 *       <testcase classname="eu.henkelmann.bla.SomeTest" name="testBaz" time="0.0">
 *           <failure message="the baz was no bar" type="junit.framework.AssertionFailedError">...stack...</failure>
 *        </testcase>
 *       <system-out><![CDATA[]]></system-out>
 *       <system-err><![CDATA[]]></system-err>
 *  </testsuite>
 *
 * @param outputDir path to the dir in which a folder with results is generated
 */
class JUnitXmlTestsListener(val outputDir: String, suiteName: String = "play.api.test.JUnitXmlTestsListener") extends RunListener {

  private val testStarted = new collection.mutable.ListBuffer[Tuple2[String, String]]()
  private val testFailed = collection.mutable.Map[String, Tuple3[String, String, String]]()
  private var tests = 0
  private var failures = 0
  /**Current hostname so we know which machine executed the tests*/
  val hostname = InetAddress.getLocalHost.getHostName
  /**The dir in which we put all result files. Is equal to the given dir + "/test-reports"*/
  val targetDir = new File(outputDir + File.separator + "test-reports")
  val targetFile = "junit-result.xml"

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

  override def testRunFinished(result: Result) {

    val totalTime = elapsedTimeAsString(result.getRunTime())
    val xmlContent = <testsuite name={ suiteName } failures={ failures.toString } tests={ tests.toString } hostname={ hostname } time={ totalTime }>
        { properties }
        {
          testStarted.toList.map { t =>
            <testcase classname={ t._1 } name={ t._2 } time="0.0">{testFailed.get(t._1).map { f: Tuple3[String, String, String] => <failure message={ f._1 } type={ f._2 }>{ f._3 }</failure>}.getOrElse(<failure></failure>)}</testcase>
          }
        }
        <system-out><![CDATA[]]></system-out>
        <system-err><![CDATA[]]></system-err>
      </testsuite>

    val file = new File(targetDir, targetFile)
    if (!file.exists) {
      targetDir.mkdir()
      file.createNewFile
    } else file.delete

    XML.save(file.getAbsolutePath, xmlContent, "UTF-8", true, null)

  }

  override def testStarted(description: Description) {
    tests = tests + 1
    val entry = getKey(description)
    testStarted.append( entry )
  }

  private def getKey(description: Description ): (String, String) = {
    val TestPattern = "(.*)\\((.*)\\)".r
    description.getDisplayName match {
      case TestPattern(description, clazz) => (clazz, description)
      case _ => throw new RuntimeException("could not find test")
    }    
  }

  override def testFailure(failure: Failure) {
    failures += failures +1
    val (clazz, description) = getKey(failure.getDescription)
    val entry = (failure.getMessage, failure.getException.toString, failure.getTrace)
    testFailed += clazz -> entry
  }

  override def testIgnored(description: Description) {}

  def elapsedTimeAsString(runTime: Long): String = {
    NumberFormat.getInstance().format(runTime.asInstanceOf[Double] / 1000)
  }

}