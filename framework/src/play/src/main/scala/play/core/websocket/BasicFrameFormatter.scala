/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.websocket

import play.api.mvc.WebSocket.FrameFormatter

/**
 * A FrameFormatter that produces BasicFrames.
 *
 * @param toFrame Function to convert to a BasicFrame.
 * @param fromFrame PartialFunction to convert from a BasicFrame.
 * @param handlesFromFrameClass Function to check if fromFrame would
 * handle a given BasicFrame class. Can be used instead of calling
 * fromFrame.isDefinedAt. The benefit of calling this function is that
 * it can avoid the need to construct a BasicFrame object, which can
 * save copying buffers.
 */
trait BasicFrameFormatter[A] extends FrameFormatter[A] {
  top =>

  def toFrame(value: A): BasicFrame
  /** Throws IllegalArgumentException if it can't handle the BasicFrame class */
  def fromFrame(frame: BasicFrame): A
  def fromFrameDefined(clazz: Class[_]): Boolean

  def transform[B](fba: B => A, fab: A => B): FrameFormatter[B] = new BasicFrameFormatter[B] {
    def toFrame(value: B): BasicFrame = top.toFrame(fba(value))
    def fromFrame(frame: BasicFrame): B = fab(top.fromFrame(frame))
    def fromFrameDefined(clazz: Class[_]): Boolean = top.fromFrameDefined(clazz)
  }

}

object BasicFrameFormatter {

  private val textFrameClass = classOf[TextFrame]
  private val binaryFrameClass = classOf[BinaryFrame]

  object textFrame extends BasicFrameFormatter[String] {
    def toFrame(text: String): BasicFrame = TextFrame(text)
    /** Throws IllegalArgumentException */
    def fromFrame(frame: BasicFrame): String = frame match {
      case TextFrame(text) => text
      case invalid => throw new IllegalArgumentException(s"Can only handle TextFrame; can't handle frame: $invalid")
    }
    def fromFrameDefined(clazz: Class[_]): Boolean = clazz match {
      case `textFrameClass` => true
      case _ => false
    }
  }

  object binaryFrame extends BasicFrameFormatter[Array[Byte]] {
    def toFrame(bytes: Array[Byte]): BasicFrame = BinaryFrame(bytes)
    def fromFrame(frame: BasicFrame): Array[Byte] = frame match {
      case BinaryFrame(bytes) => bytes
      case invalid => throw new IllegalArgumentException(s"Can only handle BinaryFrame; can't handle frame: $invalid")
    }
    def fromFrameDefined(clazz: Class[_]): Boolean = clazz match {
      case `binaryFrameClass` => true
      case _ => false
    }
  }

  object mixedFrame extends BasicFrameFormatter[Either[String, Array[Byte]]] {
    def toFrame(either: Either[String, Array[Byte]]): BasicFrame = either match {
      case Left(text) => TextFrame(text)
      case Right(bytes) => BinaryFrame(bytes)
    }
    def fromFrame(frame: BasicFrame): Either[String, Array[Byte]] = frame match {
      case TextFrame(text) => Left(text)
      case BinaryFrame(bytes) => Right(bytes)
    }
    def fromFrameDefined(clazz: Class[_]): Boolean = clazz match {
      case `textFrameClass` => true
      case `binaryFrameClass` => true
      case _ => false // shouldn't be reachable
    }
  }

}
