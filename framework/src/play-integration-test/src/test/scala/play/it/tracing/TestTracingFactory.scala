/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.tracing

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue
import play.instrumentation.spi._
import scala.collection.JavaConversions._

case class InvocationTrace[T](method:String,order:Int,args:T,timestamp:Long = System.nanoTime())

object ValidationRecord {
  final val RECORD_REQUEST_START = "recordRequestStart"
  final val RECORD_REQUEST_END = "recordRequestEnd"
  final val RECORD_INPUT_PROCESSING_START = "recordInputProcessingStart"
  final val RECORD_INPUT_PROCESSING_END = "recordInputProcessingEnd"
  final val RECORD_ACTION_START = "recordActionStart"
  final val RECORD_ACTION_END = "recordActionEnd"
  final val RECORD_OUTPUT_PROCESSING_START = "recordOutputProcessingStart"
  final val RECORD_OUTPUT_PROCESSING_END = "recordOutputProcessingEnd"
  final val RECORD_ROUTE_REQUEST_RESULT = "recordRouteRequestResult"
  final val RECORD_ERROR = "recordError"
  final val RECORD_HANDLER_NOT_FOUND = "recordHandlerNotFound"
  final val RECORD_BAD_REQUEST = "recordBadRequest"
  final val RECORD_RESOLVED = "recordResolved"
  final val RECORD_SIMPLE_RESULT = "recordSimpleResult"
  final val RECORD_CHUNKED_RESULT = "recordChunkedResult"
  final val RECORD_INPUT_CHUNK = "recordInputChunk"
  final val RECORD_OUTPUT_CHUNK = "recordOutputChunk"
  final val RECORD_INPUT_HEADER = "recordInputHeader"
  final val RECORD_EXPECTED_INPUT_BODY_BYTES = "recordExpectedInputBodyBytes"
  final val RECORD_INPUT_BODY_BYTES = "recordInputBodyBytes"
  final val RECORD_OUTPUT_HEADER = "recordOutputHeader"
  final val RECORD_EXPECTED_OUTPUT_BODY_BYTES   = "recordExpectedOutputBodyBytes"
  final val RECORD_OUTPUT_BODY_BYTES = "recordOutputBodyBytes"
}

final class ValidationRecord {
  import ValidationRecord._

  private final val counter:AtomicInteger = new AtomicInteger(0)
  private final val queue:ConcurrentLinkedQueue[InvocationTrace[_]] = new ConcurrentLinkedQueue[InvocationTrace[_]]()
  private final def newTrace[T](method:String,args:T):InvocationTrace[T] =
    InvocationTrace(method,counter.getAndIncrement(),args)
  private final def recordInvocation[T](method:String,args:T):Unit = {
    queue.add(newTrace(method,args))
  }
  final def find[T](p:InvocationTrace[_] => Boolean):Seq[InvocationTrace[T]] =
    queue.filter(p).asInstanceOf[Seq[InvocationTrace[T]]]

  final def findByName[T](name:String):Seq[InvocationTrace[T]] =
    find(_.method eq name)

  final def traces:Seq[InvocationTrace[_]] = queue.toSeq

  final def dump():Unit =
    queue.toSeq.sortBy(_.timestamp).foreach(println)

  override def toString:String =
    s"""ValidationRecord(${queue.toSeq.sortBy(_.timestamp).map(_.toString).mkString(", ")})"""

  final def recordRequestStart():Unit = recordInvocation(RECORD_REQUEST_START,())
  final def findRecordRequestStart():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_REQUEST_START)

  final def recordRequestEnd():Unit = recordInvocation(RECORD_REQUEST_END,())
  final def findRecordRequestEnd():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_REQUEST_END)

  final def recordInputProcessingStart():Unit = recordInvocation(RECORD_INPUT_PROCESSING_START,())
  final def findRecordInputProcessingStart():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_INPUT_PROCESSING_START)

  final def recordInputProcessingEnd():Unit = recordInvocation(RECORD_INPUT_PROCESSING_END,())
  final def findRecordInputProcessingEnd():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_INPUT_PROCESSING_END)

  final def recordActionStart():Unit = recordInvocation(RECORD_ACTION_START,())
  final def findRecordActionStart():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_ACTION_START)

  final def recordActionEnd():Unit = recordInvocation(RECORD_ACTION_END,())
  final def findRecordActionEnd():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_ACTION_END)

  final def recordOutputProcessingStart():Unit = recordInvocation(RECORD_OUTPUT_PROCESSING_START,())
  final def findRecordOutputProcessingStart():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_OUTPUT_PROCESSING_START)

  final def recordOutputProcessingEnd():Unit = recordInvocation(RECORD_OUTPUT_PROCESSING_END,())
  final def findRecordOutputProcessingEnd():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_OUTPUT_PROCESSING_END)

  final def recordHandlerNotFound():Unit = recordInvocation(RECORD_HANDLER_NOT_FOUND,())
  final def findRecordHandlerNotFound():Seq[InvocationTrace[Unit]] = find(_.method eq RECORD_HANDLER_NOT_FOUND)

  final def recordRouteRequestResult(result: PlayInstrumentation.RequestResult):Unit = recordInvocation(RECORD_ROUTE_REQUEST_RESULT,(result))
  final def findRecordRouteRequestResult():Seq[InvocationTrace[(PlayInstrumentation.RequestResult)]] = find(_.method eq RECORD_ROUTE_REQUEST_RESULT)

  final def recordError(error:PlayError):Unit = recordInvocation(RECORD_ERROR,error)
  final def findRecordError():Seq[InvocationTrace[PlayError]] = find(_.method eq RECORD_ERROR)

  final def recordBadRequest(error: String):Unit = recordInvocation(RECORD_BAD_REQUEST,(error))
  final def findRecordBadRequest():Seq[InvocationTrace[(String)]] = find(_.method eq RECORD_BAD_REQUEST)

  final def recordResolved(info:PlayResolved):Unit = recordInvocation(RECORD_RESOLVED,(info.getController,
                                                                                       info.getMethod,
                                                                                       info.getPath,
                                                                                       info.getVerb,
                                                                                       info.getComments))
  final def findRecordResolved():Seq[InvocationTrace[PlayResolved]] = find(_.method eq RECORD_RESOLVED)

  final def recordSimpleResult(code: Int):Unit = recordInvocation(RECORD_SIMPLE_RESULT,(code))
  final def findRecordSimpleResult():Seq[InvocationTrace[(Int)]] = find(_.method eq RECORD_SIMPLE_RESULT)

  final def recordChunkedResult(code: Int):Unit = recordInvocation(RECORD_CHUNKED_RESULT,(code))
  final def findRecordChunkedResult():Seq[InvocationTrace[(Int)]] = find(_.method eq RECORD_CHUNKED_RESULT)

  final def recordInputChunk(chunk: PlayHttpChunk):Unit = recordInvocation(RECORD_INPUT_CHUNK, chunk)
  final def findRecordInputChunk():Seq[InvocationTrace[PlayHttpChunk]] = find(_.method eq RECORD_INPUT_CHUNK)

  final def recordOutputChunk(chunk: PlayHttpChunk):Unit = recordInvocation(RECORD_OUTPUT_CHUNK, chunk)
  final def findRecordOutputChunk():Seq[InvocationTrace[PlayHttpChunk]] = find(_.method eq RECORD_OUTPUT_CHUNK)

  final def recordInputHeader(inputHeader: PlayInputHeader):Unit = recordInvocation(RECORD_INPUT_HEADER,(inputHeader.getHeaderSize,
                                                                                                         inputHeader.getMethod,
                                                                                                         inputHeader.getProtocolVersion,
                                                                                                         inputHeader.getUri,
                                                                                                         Map(inputHeader.getHeaders.map(x => x.getKey -> x.getValue):_*)))
  final def findRecordInputHeader():Seq[InvocationTrace[PlayInputHeader]] = find(_.method eq RECORD_INPUT_HEADER)

  final def recordExpectedInputBodyBytes(bytes: Long):Unit = recordInvocation(RECORD_EXPECTED_INPUT_BODY_BYTES,(bytes))
  final def findRecordExpectedInputBodyBytes():Seq[InvocationTrace[(Long)]] = find(_.method eq RECORD_EXPECTED_INPUT_BODY_BYTES)

  final def recordInputBodyBytes(bytes: Long) = recordInvocation(RECORD_INPUT_BODY_BYTES,(bytes))
  final def findRecordInputBodyBytes():Seq[InvocationTrace[(Long)]] = find(_.method eq RECORD_INPUT_BODY_BYTES)
  final def totalInputBodyBytes():Long = findRecordInputBodyBytes().view.map(_.args).sum

  final def recordOutputHeader(outputHeader:PlayOutputHeader):Unit = recordInvocation(RECORD_OUTPUT_HEADER,(outputHeader.getHeaderSize,
                                                                                                            outputHeader.getStatus,
                                                                                                            outputHeader.getProtocolVersion,
                                                                                                            Map(outputHeader.getHeaders.map(x => x.getKey -> x.getValue):_*)))
  final def findRecordOutputHeader():Seq[InvocationTrace[PlayOutputHeader]] = find(_.method eq RECORD_OUTPUT_HEADER)

  final def recordExpectedOutputBodyBytes(bytes: Long):Unit = recordInvocation(RECORD_EXPECTED_OUTPUT_BODY_BYTES,(bytes))
  final def findRecordExpectedOutputBodyBytes():Seq[InvocationTrace[(Long)]] = find(_.method eq RECORD_EXPECTED_OUTPUT_BODY_BYTES)

  final def recordOutputBodyBytes(bytes: Long) = recordInvocation(RECORD_OUTPUT_BODY_BYTES,(bytes))
  final def findRecordOuputBodyBytes():Seq[InvocationTrace[(Long)]] = find(_.method eq RECORD_OUTPUT_BODY_BYTES)
  final def totalOuputBodyBytes():Long = findRecordInputBodyBytes().view.map(_.args).sum
}

class ValidatingPlayInstrumentation(record:ValidationRecord) extends PlayInstrumentation {
  final def recordRequestStart():Unit = record.recordRequestStart()
  final def recordRequestEnd():Unit = record.recordRequestEnd()
  final def recordInputProcessingStart():Unit = record.recordInputProcessingStart()
  final def recordInputProcessingEnd():Unit = record.recordInputProcessingEnd()
  final def recordActionStart():Unit = record.recordActionStart()
  final def recordActionEnd():Unit = record.recordActionEnd()
  final def recordOutputProcessingStart():Unit = record.recordOutputProcessingStart()
  final def recordOutputProcessingEnd():Unit = record.recordOutputProcessingEnd()
  final def recordRouteRequestResult(result: PlayInstrumentation.RequestResult):Unit = record.recordRouteRequestResult(result)
  final def recordError(error:PlayError):Unit = record.recordError(error)
  final def recordHandlerNotFound():Unit = record.recordHandlerNotFound()
  final def recordBadRequest(error: String):Unit = record.recordBadRequest(error)
  final def recordResolved(info:PlayResolved):Unit = record.recordResolved(info)
  final def recordSimpleResult(code: Int):Unit = record.recordSimpleResult(code)
  final def recordChunkedResult(code: Int):Unit = record.recordChunkedResult(code)
  final def recordInputChunk(chunk: PlayHttpChunk):Unit = record.recordInputChunk(chunk)
  final def recordOutputChunk(chunk: PlayHttpChunk):Unit = record.recordOutputChunk(chunk)
  final def recordInputHeader(inputHeader:PlayInputHeader):Unit = record.recordInputHeader(inputHeader)
  final def recordExpectedInputBodyBytes(bytes: Long):Unit = record.recordExpectedInputBodyBytes(bytes)
  final def recordInputBodyBytes(bytes: Long):Unit = record.recordInputBodyBytes(bytes)
  final def recordOutputHeader(outputHeader:PlayOutputHeader):Unit = record.recordOutputHeader(outputHeader)
  final def recordExpectedOutputBodyBytes(bytes: Long):Unit = record.recordExpectedOutputBodyBytes(bytes)
  final def recordOutputBodyBytes(bytes: Long):Unit = record.recordOutputBodyBytes(bytes)
}


object ValidatingPlayInstrumentationFactory {
  final def createPlayInstrumentationFactory(record:ValidationRecord):PlayInstrumentationFactory = new PlayInstrumentationFactory {
    final def createPlayInstrumentation():PlayInstrumentation = new ValidatingPlayInstrumentation(record)
  }
}
