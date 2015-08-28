/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import java.io.File
import java.util.concurrent.Executor

import play.api.libs.Files.TemporaryFile
import play.api.mvc.BodyParsers.TakeUpTo

import scala.collection.JavaConverters._
import play.api.mvc._

/**
 * provides Java centric BodyParsers
 */
object JavaParsers {

  // Java code can't access objects defined on traits, so we use this instead
  val parse = BodyParsers.parse

  def toJavaMultipartFormData[A](multipart: MultipartFormData[TemporaryFile]): play.mvc.Http.MultipartFormData[File] = {
    new play.mvc.Http.MultipartFormData[File] {
      lazy val asFormUrlEncoded = {
        multipart.asFormUrlEncoded.mapValues(_.toArray).asJava
      }
      lazy val getFiles = {
        multipart.files.map { file =>
          new play.mvc.Http.MultipartFormData.FilePart(
            file.key, file.filename, file.contentType.orNull, file.ref.file)
        }.asJava
      }
    }
  }

  def toJavaRaw(rawBuffer: RawBuffer): play.mvc.Http.RawBuffer = {
    new play.mvc.Http.RawBuffer {
      def size = rawBuffer.size
      def asBytes(maxLength: Int) = rawBuffer.asBytes(maxLength).orNull
      def asBytes = rawBuffer.asBytes().orNull
      def asFile = rawBuffer.asFile
      override def toString = rawBuffer.toString
    }
  }

  def trampoline: Executor = play.api.libs.iteratee.Execution.Implicits.trampoline

}
