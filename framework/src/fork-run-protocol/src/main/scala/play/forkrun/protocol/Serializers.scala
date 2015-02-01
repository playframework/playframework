/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrun.protocol

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.{ PlayException, UsefulException }
import play.forkrun.protocol.ForkConfig.PollingWatchService
import play.runsupport.Reloader.{ Source, CompileSuccess, CompileFailure, CompileResult }
import sbt.GenericSerializers._
import sbt.protocol._

object Serializers {

  implicit def tuple2Reads[A, B](implicit aReads: Reads[A], bReads: Reads[B]): Reads[(A, B)] = Reads[(A, B)] { i =>
    i.validate[JsArray].flatMap { arr =>
      val s = aReads.reads(arr(0))
      val f = bReads.reads(arr(1))
      (s, f) match {
        case (JsSuccess(a, _), JsSuccess(b, _)) => JsSuccess((a, b))
        case (a @ JsError(_), JsSuccess(_, _)) => a
        case (JsSuccess(_, _), b @ JsError(_)) => b
        case (a @ JsError(_), b @ JsError(_)) => a ++ b
      }
    }
  }

  implicit def tuple2Writes[A, B](implicit aWrites: Writes[A], bWrites: Writes[B]): Writes[(A, B)] =
    Writes[(A, B)] { case (s, f) => JsArray(Seq(aWrites.writes(s), bWrites.writes(f))) }

  val sbtWatchServiceReads: Reads[PollingWatchService] = Json.reads[PollingWatchService]
  val sbtWatchServiceWrites: Writes[PollingWatchService] = Json.writes[PollingWatchService]
  val sbtWatchServiceFormat: Format[PollingWatchService] = Format[PollingWatchService](sbtWatchServiceReads, sbtWatchServiceWrites)

  implicit val watchServiceReads: Reads[ForkConfig.WatchService] = new Reads[ForkConfig.WatchService] {
    def reads(json: JsValue): JsResult[ForkConfig.WatchService] = json match {
      case JsObject(Seq(("class", JsString(name)), ("data", data))) =>
        name match {
          case "DefaultWatchService" => JsSuccess(ForkConfig.DefaultWatchService)
          case "JDK7WatchService" => JsSuccess(ForkConfig.JDK7WatchService)
          case "JNotifyWatchService" => JsSuccess(ForkConfig.JNotifyWatchService)
          case "PollingWatchService" => Json.fromJson[PollingWatchService](data)(sbtWatchServiceFormat)
          case _ => JsError(s"Unknown class: '$name'")
        }
      case _ => JsError(s"Unexpected JSON value: $json")
    }
  }

  implicit val watchServiceWrites: Writes[ForkConfig.WatchService] = new Writes[ForkConfig.WatchService] {
    def writes(watchService: ForkConfig.WatchService): JsValue = {
      val (product: Product, data) = watchService match {
        case sbt: ForkConfig.PollingWatchService => (sbt, Json.toJson(sbt)(sbtWatchServiceFormat))
        case other => (other, JsNull)
      }
      JsObject(Seq("class" -> JsString(product.productPrefix), "data" -> data))
    }
  }

  implicit val watchServiceFormat: Format[ForkConfig.WatchService] = Format[ForkConfig.WatchService](watchServiceReads, watchServiceWrites)

  implicit val forkConfigReads: Reads[ForkConfig] = Json.reads[ForkConfig]
  implicit val forkConfigWrites: Writes[ForkConfig] = Json.writes[ForkConfig]
  implicit val forkConfigFormat: Format[ForkConfig] = Format[ForkConfig](forkConfigReads, forkConfigWrites)

  implicit val sourceFileReads: Reads[Source] = Json.reads[Source]
  implicit val sourceFileWrites: Writes[Source] = Json.writes[Source]
  implicit val sourceFileFormat: Format[Source] = Format[Source](sourceFileReads, sourceFileWrites)

  implicit val sourceMapReads: Reads[Map[String, Source]] = Reads.mapReads[Source]
  implicit val sourceMapWrites: Writes[Map[String, Source]] = Writes.mapWrites[Source]
  implicit val sourceMapFormat: Format[Map[String, Source]] = Format[Map[String, Source]](sourceMapReads, sourceMapWrites)

  val playExceptionReader = {
    implicit val throwableReads = sbt.GenericSerializers.throwableReads
    (
      (__ \ "id").read[String] and
      (__ \ "title").read[String] and
      (__ \ "description").read[String] and
      (__ \ "cause").readNullable[Throwable]
    )
  }

  val playExceptionWriter = {
    implicit val throwableWrites = sbt.GenericSerializers.throwableWrites
    (
      (__ \ "id").write[String] and
      (__ \ "title").write[String] and
      (__ \ "description").write[String] and
      (__ \ "cause").writeNullable[Throwable]
    )
  }

  val defaultPlayExceptionReads: Reads[PlayException] = {
    playExceptionReader { (id, title, description, cause) =>
      val exception = new PlayException(title, description, cause.orNull)
      exception.id = id
      exception
    }
  }

  val defaultPlayExceptionWrites: Writes[PlayException] = {
    playExceptionWriter { e => (e.id, e.title, e.description, Option(e.cause)) }
  }

  val defaultPlayExceptionFormat: Format[PlayException] = Format[PlayException](defaultPlayExceptionReads, defaultPlayExceptionWrites)

  val exceptionSourceReads: Reads[PlayException.ExceptionSource] = {
    (playExceptionReader and
      (__ \ "line").read[Int] and
      (__ \ "position").read[Int] and
      (__ \ "input").read[String] and
      (__ \ "sourceName").read[String]) {
        (id, title, description, cause, _line, _position, _input, _sourceName) =>
          val exception = new PlayException.ExceptionSource(title, description, cause.orNull) {
            val line: java.lang.Integer = _line
            val position: java.lang.Integer = _position
            val input: String = _input
            val sourceName: String = _sourceName
          }
          exception.id = id
          exception
      }
  }

  val exceptionSourceWrites: Writes[PlayException.ExceptionSource] = {
    (playExceptionWriter and
      (__ \ "line").write[Int] and
      (__ \ "position").write[Int] and
      (__ \ "input").write[String] and
      (__ \ "sourceName").write[String]) { e =>
        (
          e.id,
          e.title,
          e.description,
          Option(e.cause),
          e.line,
          e.position,
          e.input,
          e.sourceName
        )
      }
  }

  val exceptionSourceFormat: Format[PlayException.ExceptionSource] = Format[PlayException.ExceptionSource](exceptionSourceReads, exceptionSourceWrites)

  implicit val playExceptionReads: Reads[PlayException] = new Reads[PlayException] {
    def reads(json: JsValue): JsResult[PlayException] = json match {
      case JsObject(Seq(("class", JsString(name)), ("data", data))) =>
        name match {
          case "ExceptionSource" => Json.fromJson[PlayException.ExceptionSource](data)(exceptionSourceFormat)
          case "PlayException" => Json.fromJson[PlayException](data)(defaultPlayExceptionFormat)
          case _ => JsError(s"Unknown class: '$name'")
        }
      case _ => JsError(s"Unexpected JSON value: $json")
    }
  }

  implicit val playExceptionWrites: Writes[PlayException] = new Writes[PlayException] {
    def writes(exception: PlayException): JsValue = {
      val (name, data) = exception match {
        case e: PlayException.ExceptionSource => ("ExceptionSource", Json.toJson(e)(exceptionSourceFormat))
        case e => ("PlayException", Json.toJson(e)(defaultPlayExceptionFormat))
      }
      JsObject(Seq("class" -> JsString(name), "data" -> data))
    }
  }

  implicit val playExceptionFormat: Format[PlayException] = Format[PlayException](playExceptionReads, playExceptionWrites)

  val compileSuccessReads: Reads[CompileSuccess] = Json.reads[CompileSuccess]
  val compileSuccessWrites: Writes[CompileSuccess] = Json.writes[CompileSuccess]
  val compileSuccessFormat: Format[CompileSuccess] = Format[CompileSuccess](compileSuccessReads, compileSuccessWrites)

  val compileFailureReads: Reads[CompileFailure] = Json.reads[CompileFailure]
  val compileFailureWrites: Writes[CompileFailure] = Json.writes[CompileFailure]
  val compileFailureFormat: Format[CompileFailure] = Format[CompileFailure](compileFailureReads, compileFailureWrites)

  implicit val compileResultReads: Reads[CompileResult] = new Reads[CompileResult] {
    def reads(json: JsValue): JsResult[CompileResult] = json match {
      case JsObject(Seq(("class", JsString(name)), ("data", data))) =>
        name match {
          case "CompileSuccess" => Json.fromJson[CompileSuccess](data)(compileSuccessFormat)
          case "CompileFailure" => Json.fromJson[CompileFailure](data)(compileFailureFormat)
          case _ => JsError(s"Unknown class: '$name'")
        }
      case _ => JsError(s"Unexpected JSON value: $json")
    }
  }

  implicit val compileResultWrites: Writes[CompileResult] = new Writes[CompileResult] {
    def writes(compileResult: CompileResult): JsValue = {
      val (product: Product, data) = compileResult match {
        case c: CompileSuccess => (c, Json.toJson(c)(compileSuccessFormat))
        case c: CompileFailure => (c, Json.toJson(c)(compileFailureFormat))
      }
      JsObject(Seq("class" -> JsString(product.productPrefix), "data" -> data))
    }
  }

  implicit val compileResultFormat: Format[CompileResult] = Format[CompileResult](compileResultReads, compileResultWrites)

  implicit val playServerStartedReads: Reads[PlayServerStarted] = Json.reads[PlayServerStarted]
  implicit val playServerStartedWrites: Writes[PlayServerStarted] = Json.writes[PlayServerStarted]
  implicit val playServerStartedFormat: Format[PlayServerStarted] = Format[PlayServerStarted](playServerStartedReads, playServerStartedWrites)

  sealed trait LocalRegisteredFormat {
    type T
    def manifest: Manifest[T]
    def format: Format[T]
  }

  object LocalRegisteredFormat {
    def fromFormat[U](f: Format[U])(implicit mf: Manifest[U]): LocalRegisteredFormat =
      new LocalRegisteredFormat {
        type T = U
        val manifest = mf
        val format = f
      }
  }

  val formats: Seq[LocalRegisteredFormat] = List(
    LocalRegisteredFormat.fromFormat(forkConfigFormat),
    LocalRegisteredFormat.fromFormat(compileResultFormat),
    LocalRegisteredFormat.fromFormat(playServerStartedFormat))
}
