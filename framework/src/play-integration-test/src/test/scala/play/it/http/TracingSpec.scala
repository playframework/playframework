/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import play.api.mvc._
import play.api.test._
import play.api.test.TestServer
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits._
import scala.util.Random
import play.it.tracing._
import org.specs2.matcher.Matcher

object TracingSpec extends PlaySpecification {
  import ValidationRecord._

  val haveBasicCausalOrder:Matcher[ValidationRecord] = ({(r:ValidationRecord) =>
    val s1 = r.traces.sortBy(_.timestamp)
    s1.zip(s1.drop(1)).forall { case (a,b) => a.order < b.order }
  },"causal and chronological order do not match")

  def haveExactlyOccur(count:Int,traceName:String):Matcher[ValidationRecord] =
    ({(r:ValidationRecord) => r.findByName(traceName).length == count},
      s"$traceName does not occur $count times")
  def haveOccurExactlyOnce(traceName:String):Matcher[ValidationRecord] = haveExactlyOccur(1,traceName)

  def checkBasicSanity(r:ValidationRecord) = {
    r must haveOccurExactlyOnce(RECORD_REQUEST_START)
    r must haveOccurExactlyOnce(RECORD_INPUT_HEADER)
    r must haveOccurExactlyOnce(RECORD_OUTPUT_HEADER)
    r must haveOccurExactlyOnce(RECORD_RESOLVED)
    r must haveOccurExactlyOnce(RECORD_ROUTE_REQUEST_RESULT)
    r must haveOccurExactlyOnce(RECORD_INPUT_PROCESSING_START)
    r must haveOccurExactlyOnce(RECORD_INPUT_PROCESSING_END)
    r must haveOccurExactlyOnce(RECORD_ACTION_START)
    r must haveOccurExactlyOnce(RECORD_OUTPUT_PROCESSING_START)
    r must haveOccurExactlyOnce(RECORD_SIMPLE_RESULT)
    r must haveOccurExactlyOnce(RECORD_ACTION_END)
    r must haveOccurExactlyOnce(RECORD_OUTPUT_PROCESSING_END)
    r must haveOccurExactlyOnce(RECORD_REQUEST_END)
  }

  def haveOccurBefore(traceName1:String, traceName2:String):Matcher[ValidationRecord] =
    ({ (r:ValidationRecord) =>
      val o = for {
        t1 <- r.findByName(traceName1).headOption.map((_:InvocationTrace[_]).order)
        t2 <- r.findByName(traceName2).headOption.map((_:InvocationTrace[_]).order)
      } yield (t1 < t2)
      o.getOrElse(true)
     },s"$traceName1 must occur before $traceName2")

  def haveAllOccurAfter(traceName1:String, traceName2:String):Matcher[ValidationRecord] =
    ({ (r:ValidationRecord) =>
      val o = for {
        t1 <- r.findByName(traceName1).headOption.map((_:InvocationTrace[_]).order)
      } yield r.findByName(traceName2).map((_:InvocationTrace[_]).order).forall(t1 < _)
      o.getOrElse(true)
     },s"$traceName1 must occur before $traceName2")

  def haveAllOccurBefore(traceName1:String, traceName2:String):Matcher[ValidationRecord] =
    ({ (r:ValidationRecord) =>
      val o = for {
        t1 <- r.findByName(traceName2).headOption.map((_:InvocationTrace[_]).order)
      } yield r.findByName(traceName1).map((_:InvocationTrace[_]).order).forall(_ < t1)
      o.getOrElse(true)
    },s"$traceName1 must occur before $traceName2")

  def checkSpecificCausalOrder(r:ValidationRecord) = {
    r must haveOccurBefore(RECORD_REQUEST_START, RECORD_INPUT_HEADER)
    r must haveOccurBefore(RECORD_INPUT_HEADER, RECORD_RESOLVED)
    r must haveOccurBefore(RECORD_INPUT_HEADER, RECORD_HANDLER_NOT_FOUND)
    r must haveOccurBefore(RECORD_RESOLVED, RECORD_ROUTE_REQUEST_RESULT)
    r must haveOccurBefore(RECORD_ROUTE_REQUEST_RESULT, RECORD_INPUT_PROCESSING_START)
    r must haveOccurBefore(RECORD_INPUT_PROCESSING_START, RECORD_INPUT_PROCESSING_END)
    r must haveOccurBefore(RECORD_EXPECTED_INPUT_BODY_BYTES, RECORD_INPUT_PROCESSING_START)
//    This condition cannot be met if the EssentialAction returns a result before all the
//    inputs have been consumed.  It is possible for Netty to deliver one more
//    message after all the processing is complete
//    r must haveAllOccurBefore(RECORD_INPUT_BODY_BYTES, RECORD_INPUT_PROCESSING_END)

//    Cannot be met if POST body is not chunked.  But that's OK
//    r must haveAllOccurAfter(RECORD_INPUT_PROCESSING_START,RECORD_INPUT_BODY_BYTES)
    r must haveOccurBefore(RECORD_INPUT_PROCESSING_END, RECORD_ACTION_START)
    r must haveOccurBefore(RECORD_ACTION_START, RECORD_OUTPUT_PROCESSING_START)
    r must haveOccurBefore(RECORD_ACTION_START, RECORD_SIMPLE_RESULT)
    r must haveOccurBefore(RECORD_ACTION_START, RECORD_CHUNKED_RESULT)
    r must haveOccurBefore(RECORD_ACTION_END, RECORD_OUTPUT_PROCESSING_START)
    r must haveAllOccurAfter(RECORD_OUTPUT_PROCESSING_START,RECORD_OUTPUT_BODY_BYTES)
    r must haveOccurBefore(RECORD_OUTPUT_PROCESSING_START, RECORD_EXPECTED_OUTPUT_BODY_BYTES)
    r must haveOccurBefore(RECORD_OUTPUT_PROCESSING_START, RECORD_OUTPUT_PROCESSING_END)
    r must haveOccurBefore(RECORD_EXPECTED_OUTPUT_BODY_BYTES, RECORD_OUTPUT_PROCESSING_END)
    r must haveOccurBefore(RECORD_OUTPUT_PROCESSING_END, RECORD_REQUEST_END)
    r must haveAllOccurBefore(RECORD_OUTPUT_BODY_BYTES, RECORD_OUTPUT_PROCESSING_END)
  }

  def checkEASanity(r:ValidationRecord) = {
    r must haveBasicCausalOrder
    checkBasicSanity(r)
    checkSpecificCausalOrder(r)
  }

  "Tracing" should {

    def withServer[T](action: EssentialAction)(block: Port => T):(ValidationRecord,T) = {
      val record: ValidationRecord = new ValidationRecord
      val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => action
        }
      ),instrumentationFactory = ValidatingPlayInstrumentationFactory.createPlayInstrumentationFactory(record))) {
        (record,block(port))
      }
    }

    "handle get request" in {
      def server[T] = withServer[T](EssentialAction { rh =>
        Iteratee.ignore[Array[Byte]].map(_ => Results.Ok)
      })_

      val (record,result) = server { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        )
        responses.length must_== 1
        responses(0).status must_== 200
      }
      checkEASanity(record)
    }

    "handle small post request" in {
      def server[T] = withServer[T](EssentialAction { rh =>
        Iteratee.ignore[Array[Byte]].map(_ => Results.Ok)
      })_
      val body = new String(Random.alphanumeric.take(1024).toArray)
      val bodyBytes = body.getBytes("UTF-8")

      val (record,result) = server { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body)
        )
        responses.length must_== 1
        responses(0).status must_== 200
      }
      checkEASanity(record)
      val ibeb = record.findRecordExpectedInputBodyBytes
      ibeb.length must_== 1
      ibeb.head.args must_== bodyBytes.size
      val ibb = record.findRecordInputBodyBytes
      ibb.map(_.args).sum must_== bodyBytes.size
    }

    "handle post request" in {
      def server[T] = withServer[T](EssentialAction { rh =>
        Iteratee.ignore[Array[Byte]].map(_ => Results.Ok)
      })_
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      val bodyBytes = body.getBytes("UTF-8")

      val (record,result) = server { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body)
        )
        responses.length must_== 1
        responses(0).status must_== 200
      }
      checkEASanity(record)
      val ibeb = record.findRecordExpectedInputBodyBytes
      ibeb.length must_== 1
      ibeb.head.args must_== bodyBytes.size
      val ibb = record.findRecordInputBodyBytes
      ibb.map(_.args).sum must_== bodyBytes.size
    }

    "gracefully handle early body parser termination" in {
      def server[T] = withServer[T](EssentialAction { rh =>
        Traversable.takeUpTo[Array[Byte]](20 * 1024) &>> Iteratee.ignore[Array[Byte]].map(_ => Results.Ok)
      })_
      val body = new String(Random.alphanumeric.take(50 * 1024).toArray)
      val bodyBytes = body.getBytes("UTF-8")

      val (record,result) = server { port =>
        // Trickle feed is important, otherwise it won't switch to ignoring the body.
        val responses = BasicHttpClient.makeRequests(port, trickleFeed = Some(100L))(
          BasicRequest("POST", "/", "HTTP/1.1", Map("Content-Length" -> body.length.toString), body)
        )
        responses.length must_== 1
        responses(0).status must_== 200
      }
      checkEASanity(record)
    }
  }
}
