/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.websocket

import org.specs2.mutable.Specification

object BasicFrameFormatterSpec extends Specification {

  "BasicFrameFormatter.textFrame" should {
    "translate strings to TextFrames" in {
      BasicFrameFormatter.textFrame.toFrame("hello") must_== TextFrame("hello")
    }
    "translate TextFrames to strings" in {
      BasicFrameFormatter.textFrame.fromFrame(TextFrame("hello")) must_== "hello"
    }
    "not translate BinaryFrames" in {
      BasicFrameFormatter.textFrame.fromFrame(BinaryFrame(Array[Byte](1, 2, 3))) must throwAn[IllegalArgumentException]
    }
    "say that it handles TextFrames" in {
      BasicFrameFormatter.textFrame.fromFrameDefined(classOf[TextFrame]) must beTrue
    }
    "say that it doesn't handle BinaryFrames" in {
      BasicFrameFormatter.textFrame.fromFrameDefined(classOf[BinaryFrame]) must beFalse
    }
  }

  "BasicFrameFormatter.binaryFrame" should {
    "translate byte arrays to BinaryFrames" in {
      BasicFrameFormatter.binaryFrame.toFrame(Array[Byte](1, 2, 3)) must beLike {
        case BinaryFrame(bytes) => bytes.to[Seq] must_== Seq[Byte](1, 2, 3)
      }
    }
    "translate BinaryFrames to byte arrays" in {
      BasicFrameFormatter.binaryFrame.fromFrame(BinaryFrame(Array[Byte](1, 2, 3))).to[Seq] must_== Seq[Byte](1, 2, 3)
    }
    "not translate TextFrames" in {
      BasicFrameFormatter.binaryFrame.fromFrame(TextFrame("foo")) must throwAn[IllegalArgumentException]
    }
    "say that it handles BinaryFrames" in {
      BasicFrameFormatter.binaryFrame.fromFrameDefined(classOf[BinaryFrame]) must beTrue
    }
    "say that it doesn't handle TextFrames" in {
      BasicFrameFormatter.binaryFrame.fromFrameDefined(classOf[TextFrame]) must beFalse
    }
  }

  "BasicFrameFormatter.mixedFrame" should {
    "translate strings to TextFrames" in {
      BasicFrameFormatter.mixedFrame.toFrame(Left("banana")) must_== TextFrame("banana")
    }
    "translate byte arrays to BinaryFrames" in {
      BasicFrameFormatter.mixedFrame.toFrame(Right(Array[Byte](1, 2, 3))) must beLike {
        case BinaryFrame(bytes) => bytes.to[Seq] must_== Seq[Byte](1, 2, 3)
      }
    }
    "translate TextFrames to strings" in {
      BasicFrameFormatter.mixedFrame.fromFrame(TextFrame("elephant")) must_== Left("elephant")
    }
    "translate BinaryFrames to byte arrays" in {
      BasicFrameFormatter.mixedFrame.fromFrame(BinaryFrame(Array[Byte](1, 2, 3))) must beRight.like {
        case bytes => bytes.to[Seq] must_== Seq[Byte](1, 2, 3)
      }
    }
    "say that it handles BinaryFrames" in {
      BasicFrameFormatter.mixedFrame.fromFrameDefined(classOf[BinaryFrame]) must beTrue
    }
    "say that it handles TextFrames" in {
      BasicFrameFormatter.mixedFrame.fromFrameDefined(classOf[TextFrame]) must beTrue
    }
  }

}
