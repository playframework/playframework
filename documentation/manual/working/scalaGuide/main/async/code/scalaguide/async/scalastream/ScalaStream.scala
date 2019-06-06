/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.async.scalastream

import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.inject.Inject

import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc.ResponseHeader
import play.api.mvc.Result

import scala.concurrent.ExecutionContext

class ScalaStreamController @Inject()(val controllerComponents: ControllerComponents)(
    implicit executionContext: ExecutionContext
) extends BaseController {

  //#by-default
  def index = Action {
    Ok("Hello World")
  }
  //#by-default

  //#by-default-http-entity
  def action = Action {
    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Strict(ByteString("Hello world"), Some("text/plain"))
    )
  }
  //#by-default-http-entity

  private def createSourceFromFile = {
    //#create-source-from-file
    val file                          = new java.io.File("/tmp/fileToServe.pdf")
    val path: java.nio.file.Path      = file.toPath
    val source: Source[ByteString, _] = FileIO.fromPath(path)
    //#create-source-from-file
  }

  //#streaming-http-entity
  def streamed = Action {

    val file                          = new java.io.File("/tmp/fileToServe.pdf")
    val path: java.nio.file.Path      = file.toPath
    val source: Source[ByteString, _] = FileIO.fromPath(path)

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Streamed(source, None, Some("application/pdf"))
    )
  }
  //#streaming-http-entity

  //#streaming-http-entity-with-content-length
  def streamedWithContentLength = Action {

    val file                          = new java.io.File("/tmp/fileToServe.pdf")
    val path: java.nio.file.Path      = file.toPath
    val source: Source[ByteString, _] = FileIO.fromPath(path)

    val contentLength = Some(file.length())

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Streamed(source, contentLength, Some("application/pdf"))
    )
  }
  //#streaming-http-entity-with-content-length

  //#serve-file
  def file = Action {
    Ok.sendFile(new java.io.File("/tmp/fileToServe.pdf"))
  }
  //#serve-file

  //#serve-file-with-name
  def fileWithName = Action {
    Ok.sendFile(
      content = new java.io.File("/tmp/fileToServe.pdf"),
      fileName = _ => "termsOfService.pdf"
    )
  }
  //#serve-file-with-name

  //#serve-file-attachment
  def fileAttachment = Action {
    Ok.sendFile(
      content = new java.io.File("/tmp/fileToServe.pdf"),
      inline = false
    )
  }
  //#serve-file-attachment

  private def getDataStream: InputStream = new ByteArrayInputStream("hello".getBytes())

  private def sourceFromInputStream = {
    //#create-source-from-input-stream
    val data                               = getDataStream
    val dataContent: Source[ByteString, _] = StreamConverters.fromInputStream(() => data)
    //#create-source-from-input-stream
  }

  //#chunked-from-input-stream
  def chunked = Action {
    val data                               = getDataStream
    val dataContent: Source[ByteString, _] = StreamConverters.fromInputStream(() => data)
    Ok.chunked(dataContent)
  }
  //#chunked-from-input-stream

  //#chunked-from-source
  def chunkedFromSource = Action {
    val source = Source.apply(List("kiki", "foo", "bar"))
    Ok.chunked(source)
  }
  //#chunked-from-source
}
