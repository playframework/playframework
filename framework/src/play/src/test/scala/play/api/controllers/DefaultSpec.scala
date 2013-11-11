/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api.mvc.{Action, AnyContent}
import play.api.ControllerWithFakeApplicationSpecification
import play.api.http.Status._

class DefaultSpec extends ControllerWithFakeApplicationSpecification {

  def is = s2"""
  Default controller must provide
    the status for routes using poorly named shortcuts
      todo                              $statusToDo
      redirect                          $statusRedirect
      error                             $statusError

    the status for routes wanting
      OK                                $statusOK
      OK (with empty content)           $statusEmptyOK
      No Content                        $statusNoContent
      Reset Content                     $statusResetContent
      Moved Permanently                 $statusMovedPermanently
      Found                             $statusFound
      See Other                         $statusSeeOther
      Temporary Redirect                $statusTemporaryRedirect
      Permanent Redirect                $statusPermanentRedirect
      Not Found                         $statusNotFound
      Gone                              $statusGone
      Internal Server Error             $statusInternalServerError
      Not Implemented                   $statusNotImplemented
      Too Many Requests                 $statusTooManyRequests
      Too Many Requests (retryAfter)    $statusRetryAfterTooManyRequests
      Service Unavailable               $statusServiceUnavailable
  """

  def statusToDo = statusFrom(Default.todo) === NOT_IMPLEMENTED
  def statusRedirect = statusFrom(Default.redirect("http://localhost/")) === SEE_OTHER
  def statusError = statusFrom(Default.error) === INTERNAL_SERVER_ERROR

  def statusOK = statusFrom(Default.ok("hello")) === OK
  def statusEmptyOK = statusFrom(Default.ok) === OK
  def statusNoContent = statusFrom(Default.noContent) === NO_CONTENT
  def statusResetContent = statusFrom(Default.resetContent) === RESET_CONTENT
  def statusMovedPermanently = statusFrom(Default.movedPermanently("http://localhost/")) === MOVED_PERMANENTLY
  def statusFound = statusFrom(Default.found("http://localhost/")) === FOUND
  def statusSeeOther = statusFrom(Default.seeOther("http://localhost/")) === SEE_OTHER
  def statusTemporaryRedirect = statusFrom(Default.temporaryRedirect("http://localhost/")) === TEMPORARY_REDIRECT
  def statusPermanentRedirect = statusFrom(Default.permanentRedirect("http://localhost/")) === PERMANENT_REDIRECT
  def statusNotFound = statusFrom(Default.notFound) === NOT_FOUND
  def statusGone = statusFrom(Default.gone) === GONE
  def statusTooManyRequests = statusFrom(Default.tooManyRequests) === TOO_MANY_REQUEST
  def statusRetryAfterTooManyRequests = statusFrom(Default.tooManyRequests("1")) === TOO_MANY_REQUEST
  def statusInternalServerError = statusFrom(Default.internalServerError) === INTERNAL_SERVER_ERROR
  def statusNotImplemented = statusFrom(Default.notImplemented) === NOT_IMPLEMENTED
  def statusServiceUnavailable = statusFrom(Default.serviceUnavailable) === SERVICE_UNAVAILABLE

  private def statusFrom(f: Action[AnyContent]) = {
    import scala.concurrent.Await
    import scala.concurrent.ExecutionContext.Implicits._

    val response = f.apply(defaultTestRequest).map(_.header).run
    Await.result(response, timeout).status
  }
}



