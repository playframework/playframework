/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import akka.util.ByteString
import org.specs2.mutable.Specification
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.utils.PlayIO

class RawBufferSpec extends Specification {
  val tempFileCreator = SingletonTemporaryFileCreator

  "RawBuffer" should {
    implicit def stringToBytes(s: String): ByteString = ByteString(s, "utf-8")

    "work in memory" in {
      val buffer = RawBuffer(100, tempFileCreator)
      buffer.size must_== 0
      buffer.push("hello")
      buffer.push(" ")
      buffer.push("world")
      buffer.size must_== 11
      buffer.asBytes() must beSome[ByteString].like {
        case bytes => bytes.utf8String must_== "hello world"
      }
    }

    "write out to a file" in {
      val buffer = RawBuffer(10, tempFileCreator)
      buffer.push("hello")
      buffer.push(" ")
      buffer.push("world")
      buffer.size must_== 11
      buffer.close()
      buffer.asBytes() must beNone
      buffer.asBytes(11) must beSome[ByteString].like {
        case bytes => bytes.utf8String must_== "hello world"
      }
    }

    def rand(size: Int) = {
      new String(scala.util.Random.alphanumeric.take(size).toArray[Char])
    }

    "extend the size by a small amount" in {
      val buffer = RawBuffer(1024 * 100, tempFileCreator)
      // RawBuffer starts with 8192 buffer size, write 8000 bytes, then another 400, make sure that works
      val big   = rand(8000)
      val small = rand(400)
      buffer.push(big)
      buffer.push(small)
      buffer.size must_== 8400
      buffer.asBytes() must beSome[ByteString].like {
        case bytes => bytes.utf8String must_== (big + small)
      }
    }

    "extend the size by a large amount" in {
      val buffer = RawBuffer(1024 * 100, tempFileCreator)
      // RawBuffer starts with 8192 buffer size, write 8000 bytes, then another 8000, make sure that works
      val big = rand(8000)
      buffer.push(big)
      buffer.push(big)
      buffer.size must_== 16000
      buffer.asBytes() must beSome[ByteString].like {
        case bytes => bytes.utf8String must_== (big + big)
      }
    }

    "allow something that fits in memory to be accessed as a file" in {
      val buffer = RawBuffer(20, tempFileCreator)
      buffer.push("hello")
      buffer.push(" ")
      buffer.push("world")
      buffer.size must_== 11
      val file = buffer.asFile.toPath
      PlayIO.readFileAsString(file) must_== "hello world"
    }
  }
}
