package play.api.libs

import play.api._
import play.api.libs.concurrent._

import _root_.akka.dispatch._

import akka.Akka._

/**
 * Defines convenient helpers to work with Akka from Play.
 */
package object akka {

  /**
   * Implicit conversion of Future to AkkaFuture, supporting the asPromise operation.
   */
  implicit def akkaToPlay[A](future: Future[A]) = new AkkaFuture(future)
  
}