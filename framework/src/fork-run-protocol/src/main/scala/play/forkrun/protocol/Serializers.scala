/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrun.protocol

import play.api.{ PlayException, UsefulException }
import play.forkrun.protocol.ForkConfig.SbtWatchService
import play.runsupport.Reloader.{ Source, CompileSuccess, CompileFailure, CompileResult }
import sbt.protocol._
import java.io.File
import scala.collection.immutable
import sbt.serialization.SbtSerializer
import sbt.serialization.functions._
import CoreProtocol._
import scala.pickling.{ FastTypeTag, PBuilder, PReader, PicklingException, SPickler, Unpickler }

object Serializers {

  implicit def tuple2Pickler[A, B](implicit pickerA: SPickler[A], pickerB: SPickler[B],
    unpickerA: Unpickler[A], unpickerB: Unpickler[B],
    tupleTag: FastTypeTag[Tuple2[A, B]], aTag: FastTypeTag[A], bTag: FastTypeTag[B]): SPickler[Tuple2[A, B]] with Unpickler[Tuple2[A, B]] =
    new SPickler[Tuple2[A, B]] with Unpickler[Tuple2[A, B]] {
      override def tag: FastTypeTag[Tuple2[A, B]] = tupleTag
      override def pickle(picklee: Tuple2[A, B], builder: PBuilder): Unit = {
        builder.pushHints()
        builder.hintTag(tag)
        builder.hintStaticallyElidedType()
        builder.beginEntry(picklee)

        builder.beginCollection(2)
        builder.hintTag(aTag)
        builder.putElement(b => pickerA.pickle(picklee._1, b))
        builder.hintTag(bTag)
        builder.putElement(b => pickerB.pickle(picklee._2, b))
        builder.endCollection()

        builder.endEntry()
        builder.popHints()
      }
      override def unpickle(tpe: String, reader: PReader): Any = {
        reader.pushHints()
        reader.hintStaticallyElidedType()
        reader.hintTag(tag)
        reader.hintStaticallyElidedType()
        reader.beginEntry()

        reader.beginCollection()
        reader.hintStaticallyElidedType()
        reader.hintTag(aTag)
        val a: A = unpickerA.unpickleEntry(reader.readElement()).asInstanceOf[A]
        reader.hintTag(bTag)
        val b: B = unpickerB.unpickleEntry(reader.readElement()).asInstanceOf[B]
        reader.endCollection()

        reader.endEntry()
        reader.popHints()
        (a, b)
      }
    }

  implicit val defaultWatchServiceSPickler: SPickler[ForkConfig.DefaultWatchService.type] = genPickler[ForkConfig.DefaultWatchService.type]
  implicit val defaultWatchServiceUnpickler: Unpickler[ForkConfig.DefaultWatchService.type] = genUnpickler[ForkConfig.DefaultWatchService.type]

  implicit val jDK7WatchServiceSPickler: SPickler[ForkConfig.JDK7WatchService.type] = genPickler[ForkConfig.JDK7WatchService.type]
  implicit val jDK7WatchServiceUnpickler: Unpickler[ForkConfig.JDK7WatchService.type] = genUnpickler[ForkConfig.JDK7WatchService.type]

  implicit val jNotifyWatchServiceSPickler: SPickler[ForkConfig.JNotifyWatchService.type] = genPickler[ForkConfig.JNotifyWatchService.type]
  implicit val jNotifyWatchServiceUnpickler: Unpickler[ForkConfig.JNotifyWatchService.type] = genUnpickler[ForkConfig.JNotifyWatchService.type]

  implicit val sbtWatchServiceSPickler: SPickler[ForkConfig.SbtWatchService] = genPickler[ForkConfig.SbtWatchService]
  implicit val sbtWatchServiceUnpickler: Unpickler[ForkConfig.SbtWatchService] = genUnpickler[ForkConfig.SbtWatchService]

  implicit val watchServiceSPicker: SPickler[ForkConfig.WatchService] = genPickler[ForkConfig.WatchService]
  implicit val watchServiceUnpickler: Unpickler[ForkConfig.WatchService] = genUnpickler[ForkConfig.WatchService]

  implicit val forkConfigPickler: SPickler[ForkConfig] = genPickler[ForkConfig]
  implicit val forkConfigUnpickler: Unpickler[ForkConfig] = genUnpickler[ForkConfig]

  implicit val sourceFilePicker: SPickler[Source] = genPickler[Source]
  implicit val sourceFileUnpickler: Unpickler[Source] = genUnpickler[Source]

  implicit val sourceMapPickler: SPickler[Map[String, Source]] with Unpickler[Map[String, Source]] = stringMapPickler[Source]

  implicit object playExceptionPickler extends SPickler[PlayException] with Unpickler[PlayException] {
    override def tag: FastTypeTag[PlayException] = implicitly[FastTypeTag[PlayException]]
    private val stringOptUnpickler = implicitly[Unpickler[Option[String]]]
    private val intOptUnpickler = implicitly[Unpickler[Option[Int]]]
    private val throwableOptUnpickler = implicitly[Unpickler[Option[Throwable]]]

    override def pickle(picklee: PlayException, builder: PBuilder): Unit = {
      def writeIntField(key: String, value: Int): Unit = builder.putField(key, (b => intPickler.pickle(value, b)))
      def writeStringField(key: String, value: String): Unit = builder.putField(key, (b => stringPickler.pickle(value, b)))
      def writeThrowableField(key: String, value: Throwable): Unit = builder.putField(key, (b => throwablePicklerUnpickler.pickle(value, b)))

      builder.pushHints()
      builder.hintTag(tag)
      builder.hintStaticallyElidedType()
      builder.beginEntry(picklee)
      writeStringField("id", picklee.id)
      writeStringField("title", picklee.title)
      writeStringField("description", picklee.description)
      if (picklee.cause != null) writeThrowableField("cause", picklee.cause)
      picklee match {
        case x: PlayException.ExceptionSource =>
          writeIntField("line", x.line)
          writeIntField("position", x.position)
          writeStringField("input", x.input)
          writeStringField("sourceName", x.sourceName)
        case _ =>
      }
      builder.endEntry()
      builder.popHints()
    }

    override def unpickle(tpe: String, reader: PReader): Any = {
      def readIntField(key: String): Int = intPickler.unpickleEntry(reader.readField(key)).asInstanceOf[Int]
      def readIntOptField(key: String): Option[Int] = intOptUnpickler.unpickleEntry(reader.readField(key)).asInstanceOf[Option[Int]]
      def readStringField(key: String): String = stringPickler.unpickleEntry(reader.readField(key)).asInstanceOf[String]
      def readStringOptField(key: String): Option[String] = stringOptUnpickler.unpickleEntry(reader.readField(key)).asInstanceOf[Option[String]]
      def readThrowableOptField(key: String): Option[Throwable] = throwableOptUnpickler.unpickleEntry(reader.readField(key)).asInstanceOf[Option[Throwable]]

      reader.pushHints()
      reader.hintStaticallyElidedType()
      reader.hintTag(tag)
      reader.hintStaticallyElidedType()
      reader.beginEntry()
      val id = readStringField("id")
      val title = readStringField("title")
      val description = readStringField("description")
      val cause = readThrowableOptField("cause")
      val line = readIntOptField("line")
      val result = line match {
        case Some(l) =>
          new PlayException.ExceptionSource(title, description, cause.orNull) {
            val line: java.lang.Integer = l
            val position: java.lang.Integer = readIntField("position")
            val input: String = readStringField("input")
            val sourceName: String = readStringField("sourceName")
          }
        case None => new PlayException(title, description, cause.orNull)
      }
      result.id = id
      reader.endEntry()
      reader.popHints()
      result
    }

  }

  implicit val compileFailureSPicker: SPickler[CompileFailure] = genPickler[CompileFailure]
  implicit val compileFailureUnpicker: Unpickler[CompileFailure] = genUnpickler[CompileFailure]

  implicit val compileSuccessSPicker: SPickler[CompileSuccess] = genPickler[CompileSuccess]
  implicit val compileSuccessUnpicker: Unpickler[CompileSuccess] = genUnpickler[CompileSuccess]

  implicit val compileResultSPickler: SPickler[CompileResult] = genPickler[CompileResult]
  implicit val compileResultUnpickler: Unpickler[CompileResult] = genUnpickler[CompileResult]

  implicit val playServerStartedSPickler: SPickler[PlayServerStarted] = genPickler[PlayServerStarted]
  implicit val playServerStartedUnpicker: Unpickler[PlayServerStarted] = genUnpickler[PlayServerStarted]

  sealed trait LocalRegisteredSerializer {
    type T
    def manifest: Manifest[T]
    def serializer: SbtSerializer[T]
  }

  object LocalRegisteredSerializer {
    def fromSbtSerializer[U](s: SbtSerializer[U])(implicit mf: Manifest[U]): LocalRegisteredSerializer =
      new LocalRegisteredSerializer {
        type T = U
        val manifest = mf
        val serializer = s
      }
  }

  val serializers: Seq[LocalRegisteredSerializer] = List(
    LocalRegisteredSerializer.fromSbtSerializer(SbtSerializer(forkConfigPickler, forkConfigUnpickler)),
    LocalRegisteredSerializer.fromSbtSerializer(SbtSerializer(compileResultSPickler, compileResultUnpickler)),
    LocalRegisteredSerializer.fromSbtSerializer(SbtSerializer(playServerStartedSPickler, playServerStartedUnpicker)))
}
