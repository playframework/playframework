/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.io._
import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.mvc._
import scala.annotation.tailrec

/**
 * Wraps a Result to make it Serializable.
 */
private[play] final class SerializableResult(constructorResult: Result) extends Externalizable {

  assert(
    Option(constructorResult).forall(_.body.isInstanceOf[HttpEntity.Strict]),
    "Only strict entities can be cached, streamed entities cannot be cached")

  /**
   * Create an empty object. Must call `readExternal` after calling
   * this method. This constructor is invoked by the Java
   * deserialization code.
   */
  def this() = this(null)

  /**
   * Hold the Result. Will either be supplied by the constructor or
   * set by `readExternal`.
   */
  private var cachedResult: Result = constructorResult

  def result: Result = {
    assert(cachedResult != null, "Result should have been provided in constructor or when deserializing")
    cachedResult
  }
  override def readExternal(in: ObjectInput): Unit = {
    assert(in.readByte() == SerializableResult.encodingVersion, "Result was serialised from a different version of Play")

    val status = in.readInt()

    val headerMap = {
      val headerLength = in.readInt()
      val mapBuilder = Map.newBuilder[String, String]
      for (_ <- 0 until headerLength) {
        val name = in.readUTF()
        val value = in.readUTF()
        mapBuilder += ((name, value))
      }
      mapBuilder.result()
    }

    val body = {
      val hasContentType = in.readBoolean()
      val contentType = if (hasContentType) {
        Some(in.readUTF())
      } else {
        None
      }
      val sizeOfBody: Int = in.readInt()
      val buffer = new Array[Byte](sizeOfBody)
      @tailrec
      def readBytes(offset: Int, length: Int): Unit = {
        if (length > 0) {
          val readLength = in.read(buffer, offset, length)
          readBytes(offset + readLength, length - readLength)
        }
      }
      readBytes(0, sizeOfBody)
      HttpEntity.Strict(ByteString(buffer), contentType)
    }

    cachedResult = Result(
      header = ResponseHeader(status, headerMap),
      body = body
    )
  }
  override def writeExternal(out: ObjectOutput): Unit = {
    out.writeByte(SerializableResult.encodingVersion)

    out.writeInt(cachedResult.header.status)

    {
      val headerMap = cachedResult.header.headers
      out.writeInt(headerMap.size)
      for ((name, value) <- headerMap) {
        out.writeUTF(name)
        out.writeUTF(value)
      }
    }

    {
      out.writeBoolean(cachedResult.body.contentType.nonEmpty)
      cachedResult.body.contentType.foreach { ct =>
        out.writeUTF(ct)
      }
      val body = cachedResult.body match {
        case HttpEntity.Strict(data, _) => data
        case other => throw new IllegalStateException("Non strict body cannot be materialized")
      }
      out.writeInt(body.length)
      out.write(body.toArray)
    }
  }
}

private[play] object SerializableResult {
  val encodingVersion = 2.toByte
}
