/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

import play.api.libs.Files.TemporaryFile

import akka.stream.Materializer

import scala.collection.JavaConverters._
import play.api.mvc._
import play.libs.Files.DelegateTemporaryFile
import play.libs.Files.{ TemporaryFile => JTemporaryFile }

/**
 * provides Java centric BodyParsers
 */
object JavaParsers {
  def toJavaMultipartFormData[A](
      multipart: MultipartFormData[TemporaryFile]
  ): play.mvc.Http.MultipartFormData[JTemporaryFile] = {
    new play.mvc.Http.MultipartFormData[JTemporaryFile] {
      lazy val asFormUrlEncoded = {
        multipart.asFormUrlEncoded.mapValues(_.toArray).toMap.asJava
      }
      lazy val getFiles = {
        multipart.files.map { file =>
          new play.mvc.Http.MultipartFormData.FilePart(
            file.key,
            file.filename,
            file.contentType.orNull,
            new DelegateTemporaryFile(file.ref).asInstanceOf[JTemporaryFile],
            file.fileSize,
            file.dispositionType
          )
        }.asJava
      }
    }
  }

  def toJavaRaw(rawBuffer: RawBuffer): play.mvc.Http.RawBuffer = {
    new play.mvc.Http.RawBuffer {
      def size                    = rawBuffer.size
      def asBytes(maxLength: Int) = rawBuffer.asBytes(maxLength).orNull
      def asBytes                 = rawBuffer.asBytes().orNull
      def asFile                  = rawBuffer.asFile
      override def toString       = rawBuffer.toString
    }
  }

  def trampoline: Executor = play.core.Execution.Implicits.trampoline

  /**
   * Flattens the completion of body parser.
   *
   * @param underlying The completion stage of body parser.
   * @param materializer The stream materializer
   * @return A body parser
   */
  def flatten[A](
      underlying: CompletionStage[play.mvc.BodyParser[A]],
      materializer: Materializer
  ): play.mvc.BodyParser[A] = new Flattened[A](underlying, materializer)

  private class Flattened[A](underlying: CompletionStage[play.mvc.BodyParser[A]], materializer: Materializer)
      extends play.mvc.BodyParser.CompletableBodyParser[A](underlying, materializer) {}
}
