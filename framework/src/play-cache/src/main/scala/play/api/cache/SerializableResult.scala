/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.cache

import java.io._
import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{ Enumerator, Iteratee }
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Wraps a Result to make it Serializable.
 */
private[play] final class SerializableResult(constructorResult: Result) extends Externalizable {

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
    assert(in.readByte() == SerializableResult.encodingVersion)

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
      val numberOfChunks: Int = in.readInt()
      val chunks: Array[Array[Byte]] = new Array[Array[Byte]](numberOfChunks)
      for (i <- 0 until numberOfChunks) {
        val chunkLength = in.readInt()
        val chunk = new Array[Byte](chunkLength)
        @tailrec
        def readBytes(offset: Int, length: Int): Unit = {
          if (length > 0) {
            val readLength = in.read(chunk, offset, length)
            readBytes(offset + readLength, length - readLength)
          }
        }
        readBytes(0, chunkLength)
        chunks(i) = chunk
      }
      Enumerator(chunks: _*) >>> Enumerator.eof
    }

    val connection = {
      val connectionInt: Int = in.readInt()
      connectionInt match {
        case 1 => HttpConnection.KeepAlive
        case 2 => HttpConnection.Close
        case unknown => throw new IOException(s"Can't deserialize HttpConnection coded as: $unknown")
      }
    }

    cachedResult = Result(
      header = ResponseHeader(status, headerMap),
      body = body,
      connection = connection
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
      val bodyChunks: List[Array[Byte]] = Await.result(cachedResult.body |>>> Iteratee.getChunks[Array[Byte]], Duration.Inf)
      out.writeInt(bodyChunks.length)
      for ((chunk, i) <- bodyChunks.zipWithIndex) {
        out.writeInt(chunk.length)
        out.write(chunk)
      }
    }

    {
      val connectionInt: Int = result.connection match {
        case HttpConnection.KeepAlive => 1
        case HttpConnection.Close => 2
        case unknown => throw new IOException(s"Can't serialize HttpConnection value: $unknown")
      }
      out.writeInt(connectionInt)
    }
  }
}

private[play] object SerializableResult {
  val encodingVersion = 1.toByte
}
