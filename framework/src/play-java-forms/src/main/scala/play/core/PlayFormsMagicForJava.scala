/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

/** Defines a magic helper for Play templates in a Java Forms context. */
object PlayFormsMagicForJava {

  import scala.collection.JavaConverters._
  import scala.language.implicitConversions

  /**
   * Implicit conversion of a Play Java form `Field` to a proper Scala form `Field`.
   */
  implicit def javaFieldtoScalaField(jField: play.data.Form.Field): play.api.data.Field = {
    new play.api.data.Field(
      null,
      jField.name,
      Option(jField.constraints).map(c => c.asScala.map { jT =>
        jT._1 -> jT._2.asScala
      }).getOrElse(Nil),
      Option(jField.format).map(f => f._1 -> f._2.asScala),
      Option(jField.errors).map(e => e.asScala.map { jE =>
        play.api.data.FormError(
          jE.key,
          jE.messages.asScala,
          jE.arguments.asScala)
      }).getOrElse(Nil),
      Option(jField.value)) {

      override def apply(key: String) = {
        javaFieldtoScalaField(jField.sub(key))
      }

      override lazy val indexes = jField.indexes.asScala.toSeq.map(_.toInt)

    }
  }

}
