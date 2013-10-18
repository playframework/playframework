/*
 * Copyright (C) 2009-2013 Typesafe Inc. [http://www.typesafe.com>
 */
package play.core.utils

import play.instrumentation.spi._
import java.util.{ Map => JMap, List => JList, Set => JSet }
import scala.collection.JavaConversions._
import play.api.mvc.{ RequestHeader, SimpleResult, EssentialAction }
import scala.concurrent.{ ExecutionContext, Future }
import play.api.libs.iteratee.{ Step, Iteratee, Input }
import play.api.libs.iteratee.Input.El
import java.util.concurrent.atomic.AtomicLong
import org.jboss.netty.handler.codec.http.{ HttpChunk, HttpMessage, HttpResponse, HttpRequest }
import play.instrumentation.spi.PlayInstrumentation.RequestResult

object Instrumentation {
  final val REQUEST_START = "requestStart"
  final val REQUEST_END = "requestEnd"
  final val INPUT_PROCESSING_START = "inputProcessingStart"
  final val INPUT_PROCESSING_END = "inputProcessingEnd"
  final val ACTION_START = "actionStart"
  final val ACTION_END = "actionEnd"
  final val OUTPUT_PROCESSING_START = "outputProcessingStart"
  final val OUTPUT_PROCESSING_END = "outputProcessingEnd"
  final val ROUTE_REQUEST_RESULT = "routeRequestResult"
  final val ERROR = "error"
  final val HANDLER_NOT_FOUND = "handlerNotFound"
  final val BAD_REQUEST = "badRequest"
  final val RESOLVED = "resolved"
  final val SIMPLE_RESULT = "simpleResult"
  final val CHUNKED_RESULT = "chunkedResult"
  final val INPUT_CHUNK = "inputChunk"
  final val OUTPUT_CHUNK = "outputChunk"
  final val INPUT_HEADER = "inputHeader"
  final val EXPECTED_INPUT_BODY_BYTES = "expectedInputBodyBytes"
  final val INPUT_BODY_BYTES = "inputBodyBytes"
  final val OUTPUT_HEADER = "outputHeader"
  final val EXPECTED_OUTPUT_BODY_BYTES = "expectedOutputBodyBytes"
  final val OUTPUT_BODY_BYTES = "outputBodyBytes"

  sealed abstract class Annotation(val tag: String)
  sealed abstract class AnnotationWithData[+T](tag: String) extends Annotation(tag) {
    def data: T
  }
  case object RequestStart extends Annotation(REQUEST_START)
  case object RequestEnd extends Annotation(REQUEST_END)
  case object InputProcessingStart extends Annotation(INPUT_PROCESSING_START)
  case object InputProcessingEnd extends Annotation(INPUT_PROCESSING_END)
  case object ActionStart extends Annotation(ACTION_START)
  case object ActionEnd extends Annotation(ACTION_END)
  case object OutputProcessingStart extends Annotation(OUTPUT_PROCESSING_START)
  case object OutputProcessingEnd extends Annotation(OUTPUT_PROCESSING_END)
  case class RouteRequestResult(data: RequestResult) extends AnnotationWithData[RequestResult](ROUTE_REQUEST_RESULT)
  case class Error(data: PlayError) extends AnnotationWithData[PlayError](ERROR) {
    override def toString: String = s"Error(${data.getMessage}})"
  }
  case object HandlerNotFound extends Annotation(HANDLER_NOT_FOUND)
  case class BadRequest(data: String) extends AnnotationWithData[String](BAD_REQUEST)
  case class Resolved(data: PlayResolved) extends AnnotationWithData[PlayResolved](RESOLVED) {
    override def toString: String = s"Resolved(${data.getController},${data.getMethod},${data.getVerb},${data.getPath})"
  }
  case class SimpleResult(data: Int) extends AnnotationWithData[Int](SIMPLE_RESULT)
  case class ChunkedResult(data: Int) extends AnnotationWithData[Int](CHUNKED_RESULT)
  case class InputChunk(data: PlayHttpChunk) extends AnnotationWithData[PlayHttpChunk](INPUT_CHUNK) {
    override def toString: String = s"InputChunk(${data.getHeaderSize},${data.getChunkSize})"
  }
  case class OutputChunk(data: PlayHttpChunk) extends AnnotationWithData[PlayHttpChunk](OUTPUT_CHUNK) {
    override def toString: String = s"OutputChunk(${data.getHeaderSize},${data.getChunkSize})"
  }
  case class InputHeader(data: PlayInputHeader) extends AnnotationWithData[PlayInputHeader](INPUT_HEADER) {
    override def toString: String = s"InputHeader(${data.getHeaderSize},${data.getProtocolVersion},${data.getMethod},${data.getUri},${data.getHeaders.map(x => x.getKey + "->" + x.getValue).mkString(", ")})"
  }
  case class ExpectedInputBodyBytes(data: Long) extends AnnotationWithData[Long](EXPECTED_INPUT_BODY_BYTES)
  case class InputBodyBytes(data: Long) extends AnnotationWithData[Long](INPUT_BODY_BYTES)
  case class OutputHeader(data: PlayOutputHeader) extends AnnotationWithData[PlayOutputHeader](OUTPUT_HEADER) {
    override def toString: String = s"OutputHeader(${data.getHeaderSize},${data.getProtocolVersion},${data.getStatus},${data.getHeaders.map(x => x.getKey + "->" + x.getValue).mkString(", ")})"
  }
  case class ExpectedOutputBodyBytes(data: Long) extends AnnotationWithData[Long](EXPECTED_OUTPUT_BODY_BYTES)
  case class OutputBodyBytes(data: Long) extends AnnotationWithData[Long](OUTPUT_BODY_BYTES)
}

object InstrumentationHelpers {
  final val NO_CONTROLLER = "<NO CONTROLLER>"
  final val NO_METHOD = "<NO METHOD>"
  final val NO_VERB = "<NO VERB>"
  final val NO_COMMENTS = "<NO COMMENTS>"
  final val NO_PATTERN = "<NO PATTERN>"

  final private val SEPERATOR_LENGTH: Int = 2
  final private val NEWLINE_LENGTH: Int = 2

  // This is not ideal.  I'm reconstructing the request and response header blocks.
  final private def requestHeaderLength(message: HttpRequest): Int = {
    message.getMethod.getName.getBytes("UTF-8").length + 1 + message.getUri.getBytes("UTF-8").length +
      message.getProtocolVersion.toString.getBytes("UTF-8").length + NEWLINE_LENGTH + httpHeadersLength(message)
  }
  final private def responseHeaderLength(message: HttpResponse): Int = {
    message.getProtocolVersion.toString.getBytes("UTF-8").length + 1 + message.getStatus.toString.getBytes("UTF-8").length + NEWLINE_LENGTH + httpHeadersLength(message)
  }
  final private def httpHeadersLength(message: HttpMessage): Int = {
    import scala.collection.JavaConversions._
    message.getHeaders.foldLeft(NEWLINE_LENGTH) {
      (s, v) => s + v.getKey.getBytes("UTF-8").length + SEPERATOR_LENGTH + v.getValue.getBytes("UTF-8").length + NEWLINE_LENGTH
    }
  }
  final private def chunkedHeaderLength(message: HttpChunk): Int = {
    message.getContent.readableBytes().toHexString.getBytes("UTF-8").length + NEWLINE_LENGTH
  }

  sealed private class WithHeaders(message: HttpMessage) extends PlayHasHeaders {
    private final val headers: JList[JMap.Entry[String, String]] = message.getHeaders
    def getHeader(name: String): String = headers.view.find(_.getKey.equalsIgnoreCase(name)).map(_.getValue) getOrElse null
    def getHeaders(name: String): JList[String] = headers.view.filter(_.getKey.equalsIgnoreCase(name)).map(_.getValue)
    def containsHeader(name: String): Boolean = headers.exists(_.getKey.equalsIgnoreCase(name))
    def getHeaders: JList[JMap.Entry[String, String]] = headers
    def getHeaderNames: JSet[String] = headers.map(_.getKey).toSet[String]
  }

  def toPlayInputHeader(request: HttpRequest): PlayInputHeader = new WithHeaders(request) with PlayInputHeader {
    val getHeaderSize: Int = requestHeaderLength(request)
    val getMethod: PlayHttpMethod = PlayHttpMethod.valueOf(request.getMethod.getName)
    val getUri: String = request.getUri
    val getProtocolVersion: PlayHttpVersion = PlayHttpVersion.valueOf(request.getProtocolVersion.getText)
  }

  def toPlayOutputHeader(response: HttpResponse): PlayOutputHeader = new WithHeaders(response) with PlayOutputHeader {
    val getHeaderSize: Int = responseHeaderLength(response)
    val getStatus: PlayHttpResponseStatus = PlayHttpResponseStatus.valueOf(response.getStatus.getCode)
    val getProtocolVersion: PlayHttpVersion = PlayHttpVersion.valueOf(response.getProtocolVersion.getText)
  }

  def toPlayChunk(chunk: HttpChunk): PlayHttpChunk = new PlayHttpChunk {
    val getChunkSize: Int = chunk.getContent.readableBytes()
    val getHeaderSize: Int = chunkedHeaderLength(chunk)
  }

  def toPlayError(exception: Exception): PlayError = new PlayError {
    lazy val getMessage: String = exception.getMessage
    lazy val getStackTrace: JList[String] = exception.getStackTrace.map(_.toString).toList
  }

  def toPlayResolved(request: RequestHeader): PlayResolved = new PlayResolved {
    lazy val getController: String = request.tags.get(play.api.Routes.ROUTE_CONTROLLER).getOrElse(NO_CONTROLLER)
    lazy val getMethod: String = request.tags.get(play.api.Routes.ROUTE_ACTION_METHOD).getOrElse(NO_METHOD)
    lazy val getParameterTypes: JList[String] = Seq.empty[String]
    lazy val getVerb: String = request.tags.get(play.api.Routes.ROUTE_VERB).getOrElse(NO_VERB)
    lazy val getComments: String = request.tags.get(play.api.Routes.ROUTE_COMMENTS).getOrElse(NO_COMMENTS)
    lazy val getPath: String = request.tags.get(play.api.Routes.ROUTE_PATTERN).getOrElse(NO_PATTERN)
  }

  def toPlayInvoked(requestHeader: RequestHeader): PlayInvoked = new PlayInvoked {
    lazy val getController: String = requestHeader.tags.get(play.api.Routes.ROUTE_CONTROLLER).getOrElse(NO_CONTROLLER)
    lazy val getMethod: String = requestHeader.tags.get(play.api.Routes.ROUTE_ACTION_METHOD).getOrElse(NO_METHOD)
    lazy val getPattern: String = requestHeader.tags.get(play.api.Routes.ROUTE_PATTERN).getOrElse(NO_PATTERN)
    lazy val getId: Long = requestHeader.id
    lazy val getUri: String = requestHeader.uri
    lazy val getPath: String = requestHeader.path
    lazy val getHttpMethod: String = requestHeader.method
    lazy val getVersion: String = requestHeader.version
    lazy val getRemoteAddress: String = requestHeader.remoteAddress
    lazy val getHost: String = requestHeader.host
    lazy val getDomain: String = requestHeader.domain
    lazy val getSession: JMap[String, String] = requestHeader.session.data
  }
}

object InstrumentationLoader {
  def getFactory(className: Option[String]): PlayInstrumentationFactory = className match {
    case None => DevNullPlayInstrumentationFactory
    case Some(cn) =>
      val clazz = Class.forName(cn)
      val factory = clazz.newInstance.asInstanceOf[PlayInstrumentationFactory]
      factory
  }
}

trait WithInstrumentation {
  import InstrumentationHelpers._

  final def recordRequestStart()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordRequestStart()
  final def recordRequestEnd()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordRequestEnd()
  final def recordInputProcessingStart()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordInputProcessingStart()
  final def recordInputProcessingEnd()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordInputProcessingEnd()
  final def recordActionStart()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordActionStart()
  final def recordActionEnd()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordActionEnd()
  final def recordOutputProcessingStart()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordOutputProcessingStart()
  final def recordOutputProcessingEnd()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordOutputProcessingEnd()
  final def recordRouteRequestResult(result: PlayInstrumentation.RequestResult)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordRouteRequestResult(result)
  final def recordError(exception: Exception)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordError(toPlayError(exception))
  final def recordHandlerNotFound()(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordHandlerNotFound()
  final def recordBadRequest(error: String)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordBadRequest(error)
  final def recordResolved(request: RequestHeader)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordResolved(toPlayResolved(request))
  final def recordSimpleResult(code: Int)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordSimpleResult(code)
  final def recordChunkedResult(code: Int)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordChunkedResult(code)
  final def recordInputChunk(chunk: PlayHttpChunk)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordInputChunk(chunk)
  final def recordOutputChunk(chunk: PlayHttpChunk)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordOutputChunk(chunk)
  final def recordInputHeader(inputHeader: PlayInputHeader)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordInputHeader(inputHeader)
  final def recordExpectedInputBodyBytes(bytes: Long)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordExpectedInputBodyBytes(bytes)
  final def recordInputBodyBytes(bytes: Long)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordInputBodyBytes(bytes)
  final def recordOutputHeader(outputHeader: PlayOutputHeader)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordOutputHeader(outputHeader)
  final def recordExpectedOutputBodyBytes(bytes: Long)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordExpectedOutputBodyBytes(bytes)
  final def recordOutputBodyBytes(bytes: Long)(implicit instrumentation: PlayInstrumentation): Unit =
    instrumentation.recordOutputBodyBytes(bytes)
}

object DevNullPlayInstrumentation extends PlayInstrumentation {
  final def recordRequestStart(): Unit = {}
  final def recordRequestEnd(): Unit = {}
  final def recordInputProcessingStart(): Unit = {}
  final def recordInputProcessingEnd(): Unit = {}
  final def recordActionStart(): Unit = {}
  final def recordActionEnd(): Unit = {}
  final def recordOutputProcessingStart(): Unit = {}
  final def recordOutputProcessingEnd(): Unit = {}
  final def recordRouteRequestResult(result: PlayInstrumentation.RequestResult): Unit = {}
  final def recordError(error: PlayError): Unit = {}
  final def recordHandlerNotFound(): Unit = {}
  final def recordBadRequest(error: String): Unit = {}
  final def recordResolved(info: PlayResolved): Unit = {}
  final def recordSimpleResult(code: Int): Unit = {}
  final def recordChunkedResult(code: Int): Unit = {}
  final def recordInputChunk(chunk: PlayHttpChunk): Unit = {}
  final def recordOutputChunk(chunk: PlayHttpChunk): Unit = {}
  final def recordInputHeader(inputHeader: PlayInputHeader): Unit = {}
  final def recordExpectedInputBodyBytes(bytes: Long): Unit = {}
  final def recordInputBodyBytes(bytes: Long) = {}
  final def recordOutputHeader(outputHeader: PlayOutputHeader): Unit = {}
  final def recordExpectedOutputBodyBytes(bytes: Long): Unit = {}
  final def recordOutputBodyBytes(bytes: Long) = {}
}

class PrintLikePlayInstrumentation(doPrint: String => Unit) extends PlayInstrumentation {
  private final def outputWithTimestamp(label: String, args: String*): Unit = {
    doPrint(s"""${label}[${System.nanoTime}](${args.mkString(", ")})""")
  }
  final def recordRequestStart(): Unit = { outputWithTimestamp("requestStart") }
  final def recordRequestEnd(): Unit = { outputWithTimestamp("requestEnd") }
  final def recordInputProcessingStart(): Unit = { outputWithTimestamp("inputProcessingStart") }
  final def recordInputProcessingEnd(): Unit = { outputWithTimestamp("inputProcessingEnd") }
  final def recordActionStart(): Unit = { outputWithTimestamp("actionStart") }
  final def recordActionEnd(): Unit = { outputWithTimestamp("actionEnd") }
  final def recordOutputProcessingStart(): Unit = { outputWithTimestamp("outputProcessingStart") }
  final def recordOutputProcessingEnd(): Unit = { outputWithTimestamp("outputProcessingEnd") }
  final def recordRouteRequestResult(result: PlayInstrumentation.RequestResult): Unit = { outputWithTimestamp("routeRequestResult", result.toString) }
  final def recordError(error: PlayError): Unit = { outputWithTimestamp("error", error.getMessage, error.getStackTrace.toList.toString) }
  final def recordHandlerNotFound(): Unit = { outputWithTimestamp("handlerNotFound") }
  final def recordBadRequest(error: String): Unit = { outputWithTimestamp("badRequest", error) }
  final def recordResolved(info: PlayResolved): Unit = {
    outputWithTimestamp("resolved",
      info.getController,
      info.getMethod,
      info.getVerb,
      info.getComments,
      info.getPath)
  }
  final def recordSimpleResult(code: Int): Unit = { outputWithTimestamp("simpleResult", code.toString) }
  final def recordChunkedResult(code: Int): Unit = { outputWithTimestamp("chunkedResult", code.toString) }
  final def recordInputChunk(chunk: PlayHttpChunk): Unit = { outputWithTimestamp("inputChunk", chunk.getHeaderSize.toString, chunk.getChunkSize.toString) }
  final def recordOutputChunk(chunk: PlayHttpChunk): Unit = { outputWithTimestamp("outputChunk", chunk.getHeaderSize.toString, chunk.getChunkSize.toString) }
  final def recordInputHeader(inputHeader: PlayInputHeader): Unit = {
    outputWithTimestamp("inputHeader",
      inputHeader.getHeaderSize.toString,
      inputHeader.getMethod.toString,
      inputHeader.getProtocolVersion.toString,
      inputHeader.getUri,
      Map(inputHeader.getHeaders.map(x => x.getKey -> x.getValue): _*).toString)
  }
  final def recordExpectedInputBodyBytes(bytes: Long): Unit = { outputWithTimestamp("expectedInputBodyBytes", bytes.toString) }
  final def recordInputBodyBytes(bytes: Long) = { outputWithTimestamp("inputBodyBytes", bytes.toString) }
  final def recordOutputHeader(outputHeader: PlayOutputHeader): Unit = {
    outputWithTimestamp("outputHeader",
      outputHeader.getHeaderSize.toString,
      outputHeader.getStatus.toString,
      outputHeader.getProtocolVersion.toString,
      Map(outputHeader.getHeaders.map(x => x.getKey -> x.getValue): _*).toString)
  }
  final def recordExpectedOutputBodyBytes(bytes: Long): Unit = { outputWithTimestamp("expectedOutputBodyBytes", bytes.toString) }
  final def recordOutputBodyBytes(bytes: Long) = { outputWithTimestamp("outputBodyBytes", bytes.toString) }
}

object PrintLnPlayInstrumentation extends PrintLikePlayInstrumentation(println)

class LoggingPlayInstrumentation(reqIdx: Long) extends PrintLikePlayInstrumentation(s => play.api.Logger.logger.info(s"request: $reqIdx -> $s"))

object DevNullPlayInstrumentationFactory extends PlayInstrumentationFactory {
  final def createPlayInstrumentation: PlayInstrumentation = {
    DevNullPlayInstrumentation
  }
}

object PrintLnPlayInstrumentationFactory extends PlayInstrumentationFactory {
  final def createPlayInstrumentation: PlayInstrumentation = {
    println("--------------------------------")
    PrintLnPlayInstrumentation
  }
}

class LoggingPlayInstrumentationFactoryBase extends PlayInstrumentationFactory {
  final val counter = new AtomicLong(0)
  final def createPlayInstrumentation: PlayInstrumentation = {
    val current = counter.getAndIncrement()
    play.api.Logger.logger.info(s"---------------- $current ----------------")
    new LoggingPlayInstrumentation(current)
  }
}

object LoggingPlayInstrumentationFactory extends LoggingPlayInstrumentationFactoryBase

class StepActions[E, A] {
  def onDone(done: Step.Done[A, E]): Unit = ()
  def onCont(k: Input[E] => Iteratee[E, A], iterateeActions: IterateeActions[E, A]): Input[E] => Iteratee[E, A] = new InputProxy[E, A](k, iterateeActions)
  def onError(error: Step.Error[E]): Unit = ()
}

class InputActions[E] {
  def onEl(el: Input.El[E]): Unit = ()
  def onEmpty(): Unit = ()
  def onEOF(): Unit = ()
}

case class IterateeActions[E, A](stepActions: StepActions[E, A] = new StepActions[E, A],
  inputActions: InputActions[E] = new InputActions[E])

class EssentialActionProxy(underlying: EssentialAction)(implicit instrumentation: PlayInstrumentation) extends EssentialAction with WithInstrumentation {
  def apply(request: RequestHeader): Iteratee[Array[Byte], SimpleResult] = {
    val ia = new InputActions[Array[Byte]] {

      final override def onEl(el: El[Array[Byte]]): Unit = ()
      final override def onEmpty(): Unit = ()
      final override def onEOF(): Unit = ()
    }
    val sa = new StepActions[Array[Byte], SimpleResult] {

      final override def onCont(k: (Input[Array[Byte]]) => Iteratee[Array[Byte], SimpleResult], iterateeActions: IterateeActions[Array[Byte], SimpleResult]): (Input[Array[Byte]]) => Iteratee[Array[Byte], SimpleResult] = {
        super.onCont(k, iterateeActions)
      }

      final override def onDone(done: Step.Done[SimpleResult, Array[Byte]]): Unit = {
        recordInputProcessingEnd()
        recordActionStart()
      }

      final override def onError(error: Step.Error[Array[Byte]]): Unit = {
        recordInputProcessingEnd()
        recordActionStart()
      }
    }
    recordInputProcessingStart()
    new IterateeProxy[Array[Byte], SimpleResult](underlying(request), IterateeActions[Array[Byte], SimpleResult](inputActions = ia, stepActions = sa))
  }
}

class IterateeProxy[E, +A](underlying: Iteratee[E, A], iterateeActions: IterateeActions[E, A]) extends Iteratee[E, A] with WithInstrumentation {
  def fold[B](folder: (Step[E, A]) => Future[B])(implicit ec: ExecutionContext): Future[B] =
    underlying.fold(new StepProxy(folder, iterateeActions))
}

class StepProxy[E, A, B](folder: Step[E, A] => Future[B], iterateeActions: IterateeActions[E, A]) extends ((Step[E, A]) => Future[B]) {
  def apply(step: Step[E, A]): Future[B] = {
    val newStep: Step[E, A] = step match {
      case x: Step.Done[A, E] =>
        iterateeActions.stepActions.onDone(x); x
      case Step.Cont(k) => Step.Cont(iterateeActions.stepActions.onCont(k, iterateeActions))
      case x: Step.Error[E] => iterateeActions.stepActions.onError(x); x
    }
    folder(newStep)
  }
}

class InputProxy[E, +A](k: Input[E] ⇒ Iteratee[E, A], iterateeActions: IterateeActions[E, A]) extends (Input[E] => Iteratee[E, A]) {
  def apply(in: Input[E]): Iteratee[E, A] = {
    in match {
      case x: Input.El[E] => iterateeActions.inputActions.onEl(x)
      case Input.Empty => iterateeActions.inputActions.onEmpty()
      case Input.EOF => iterateeActions.inputActions.onEOF()
    }
    new IterateeProxy(k(in), iterateeActions)
  }
}

object ActionProxy {
  def apply(underlying: EssentialAction)(implicit instrumentation: PlayInstrumentation): EssentialAction =
    new EssentialActionProxy(underlying)
}

