package play.runsupport

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import play.runsupport.protocol._
import PlayExceptions._
import sbt.GenericSerializers._
import sbt.protocol._
import play.api.PlayException

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

  private implicit val throwableReads = sbt.GenericSerializers.throwableReads
  private implicit val throwableWrites = sbt.GenericSerializers.throwableWrites

  implicit val sourceMapTargetWrites: Writes[SourceMapTarget] = Json.writes[SourceMapTarget]
  implicit val sourceMapTargetReads: Reads[SourceMapTarget] = Json.reads[SourceMapTarget]
  implicit val sourceMapTargetFormat: Format[SourceMapTarget] = Format[SourceMapTarget](sourceMapTargetReads, sourceMapTargetWrites)

  implicit val sourceMapWrites: Writes[Map[String, SourceMapTarget]] = Writes.mapWrites[SourceMapTarget]
  implicit val sourceMapReads: Reads[Map[String, SourceMapTarget]] = Reads.mapReads[SourceMapTarget]
  implicit val sourceMapformat: Format[Map[String, SourceMapTarget]] = Format[Map[String, SourceMapTarget]](sourceMapReads, sourceMapWrites)

  implicit val playForkSupportResultWrites: Writes[PlayForkSupportResult] = Json.writes[PlayForkSupportResult]
  implicit val playForkSupportResultReads: Reads[PlayForkSupportResult] = Json.reads[PlayForkSupportResult]
  implicit val playForkSupportResultFormat: Format[PlayForkSupportResult] = Format[PlayForkSupportResult](playForkSupportResultReads, playForkSupportResultWrites)

  implicit val playExceptionNoSourceReads: Reads[PlayExceptionNoSource] = Json.reads[PlayExceptionNoSource]
  implicit val playExceptionNoSourceWrites: Writes[PlayExceptionNoSource] = Json.writes[PlayExceptionNoSource]
  implicit val playExceptionNoSourceFormat: Format[PlayExceptionNoSource] = Format[PlayExceptionNoSource](playExceptionNoSourceReads, playExceptionNoSourceWrites)

  implicit val playExceptionWithSourceReads: Reads[PlayExceptionWithSource] = Json.reads[PlayExceptionWithSource]
  implicit val playExceptionWithSourceWrites: Writes[PlayExceptionWithSource] = Json.writes[PlayExceptionWithSource]
  implicit val playExceptionWithSourceFormat: Format[PlayExceptionWithSource] = Format[PlayExceptionWithSource](playExceptionWithSourceReads, playExceptionWithSourceWrites)

  implicit val playServerStartedReads: Reads[PlayServerStarted] = Json.reads[PlayServerStarted]
  implicit val playServerStartedWrites: Writes[PlayServerStarted] = Json.writes[PlayServerStarted]
  implicit val playServerStartedFormat: Format[PlayServerStarted] = Format[PlayServerStarted](playServerStartedReads, playServerStartedWrites)

  val throwableDeserializers = ThrowableDeserializers.empty
    .add[PlayExceptionWithSource]
    .add[PlayExceptionNoSource]

  val formats: Seq[LocalRegisteredFormat] = List(LocalRegisteredFormat.fromFormat(playForkSupportResultFormat),
    LocalRegisteredFormat.fromFormat(sourceMapTargetFormat),
    LocalRegisteredFormat.fromFormat(sourceMapformat),
    LocalRegisteredFormat.fromFormat(playExceptionWithSourceFormat),
    LocalRegisteredFormat.fromFormat(playExceptionNoSourceFormat),
    LocalRegisteredFormat.fromFormat(playServerStartedFormat))
}

