/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http.websocket

/**
 * WebSocket close codes
 */
object CloseCodes {
  val Regular = 1000
  val GoingAway = 1001
  val ProtocolError = 1002
  val Unacceptable = 1003
  val NoStatus = 1005
  val ConnectionAbort = 1006
  val InconsistentData = 1007
  val PolicyViolated = 1008
  val TooBig = 1009
  val ClientRejectsExtension = 1010
  val UnexpectedCondition = 1011
  val TLSHandshakeFailure = 1015
}
