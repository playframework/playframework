/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.runsupport

import sbt.protocol.Problem
import sbt.IO
import play.api._

sealed trait ForkRunnerException { this: PlayException =>
  def genericTitle: String
  def message: String
  def wrapped: Throwable
  def category: String
  def severity: xsbti.Severity
}

case class PlayExceptionNoSource(genericTitle: String,
  message: String,
  category: String,
  severity: xsbti.Severity,
  wrapped: Throwable) extends PlayException(genericTitle, message, wrapped) with ForkRunnerException

case class PlayExceptionWithSource(genericTitle: String,
    message: String,
    category: String,
    severity: xsbti.Severity,
    wrapped: Throwable,
    row: Int,
    column: Int,
    sourceFile: java.io.File) extends PlayException.ExceptionSource(genericTitle, message, wrapped) with ForkRunnerException {
  def line = row
  def position = column
  def input = IO.read(sourceFile)
  def sourceName = sourceFile.getAbsolutePath
}

