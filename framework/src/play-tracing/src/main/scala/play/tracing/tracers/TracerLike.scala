/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.tracers

import play.instrumentation.spi._
import play.instrumentation.spi.PlayInstrumentation.RequestResult

trait TracerLike[R] extends PlayInstrumentation {
  import play.core.utils.Instrumentation._

  protected def genResult: R

  /**
   * This method is called when the end of the request has been detected.  The ''end'' of
   * a request being when Play has fully sent its response.  This said, it is possible to
   * receive more incoming events after Play has finished.  This can be true if Play does not
   * consume all of the input sent from the client before returning a result.
   */
  def endHook: R => Unit

  /**
   * Record an event into a store.
   *
   * NOTE: this method can be called across thread boundaries.
   *
   * @param event
   */
  protected def recordInvocation(event: Annotation): Unit

  final def recordOutputBodyBytes(bytes: Long) = recordInvocation(OutputBodyBytes(bytes))
  final def recordExpectedOutputBodyBytes(bytes: Long) = recordInvocation(ExpectedOutputBodyBytes(bytes))
  final def recordOutputHeader(outputHeader: PlayOutputHeader) = recordInvocation(OutputHeader(outputHeader))
  final def recordInputBodyBytes(bytes: Long) = recordInvocation(InputBodyBytes(bytes))
  final def recordExpectedInputBodyBytes(bytes: Long) = recordInvocation(ExpectedOutputBodyBytes(bytes))
  final def recordInputHeader(inputHeader: PlayInputHeader) = recordInvocation(InputHeader(inputHeader))
  final def recordOutputChunk(chunk: PlayHttpChunk) = recordInvocation(OutputChunk(chunk))
  final def recordInputChunk(chunk: PlayHttpChunk) = recordInvocation(InputChunk(chunk))
  final def recordChunkedResult(code: Int) = recordInvocation(ChunkedResult(code))
  final def recordSimpleResult(code: Int) = recordInvocation(SimpleResult(code))
  final def recordResolved(resolved: PlayResolved) = recordInvocation(Resolved(resolved))
  final def recordBadRequest(error: String) = recordInvocation(BadRequest(error))
  final def recordHandlerNotFound() = recordInvocation(HandlerNotFound)
  final def recordError(error: PlayError) = recordInvocation(Error(error))
  final def recordRouteRequestResult(result: RequestResult) = recordInvocation(RouteRequestResult(result))
  final def recordOutputProcessingEnd() = recordInvocation(OutputProcessingEnd)
  final def recordOutputProcessingStart() = recordInvocation(OutputProcessingStart)
  final def recordActionEnd() = recordInvocation(ActionEnd)
  final def recordActionStart() = recordInvocation(ActionStart)
  final def recordInputProcessingEnd() = recordInvocation(InputProcessingEnd)
  final def recordInputProcessingStart() = recordInvocation(InputProcessingStart)
  final def recordRequestEnd() = {
    recordInvocation(RequestEnd)
    endHook(genResult)
  }
  final def recordRequestStart() = recordInvocation(RequestStart)
}
