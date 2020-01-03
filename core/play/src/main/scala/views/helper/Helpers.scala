/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import play.api.data.FormError
import play.api.templates.PlayMagic.translate
import play.twirl.api._

import scala.collection.JavaConverters._
import scala.language.implicitConversions

package views.html.helper {
  case class FieldElements(
      id: String,
      field: play.api.data.Field,
      input: Html,
      args: Map[Symbol, Any],
      p: play.api.i18n.MessagesProvider
  ) {
    def infos: Seq[Any] = {
      args.get(Symbol("_help")).map(m => Seq(translate(m)(p))).getOrElse {
        (if (args.get(Symbol("_showConstraints")) match {
               case Some(false) => false
               case _           => true
             }) {
           field.constraints.map(c => p.messages(c._1, c._2.map(a => translate(a)(p)): _*)) ++
             field.format.map(f => p.messages(f._1, f._2.map(a => translate(a)(p)): _*))
         } else Nil)
      }
    }

    def errors: Seq[Any] = {
      (args.get(Symbol("_error")) match {
        case Some(Some(FormError(_, message, args))) =>
          Some(p.messages(message, args.map(a => translate(a)(p)): _*))
        case Some(FormError(_, message, args)) =>
          Some(p.messages(message, args.map(a => translate(a)(p)): _*))
        case Some(None)  => None
        case Some(value) => Some(translate(value)(p))
        case _           => None
      }).map(Seq(_)).getOrElse {
        (if (args.get(Symbol("_showErrors")) match {
               case Some(false) => false
               case _           => true
             }) {
           field.errors.map(e => p.messages(e.message, e.args.map(a => translate(a)(p)): _*))
         } else Nil)
      }
    }

    def hasErrors: Boolean = {
      !errors.isEmpty
    }

    def label: Any = {
      args.get(Symbol("_label")).map(l => translate(l)(p)).getOrElse(p.messages(field.label))
    }

    def hasName: Boolean = args.get(Symbol("_name")).isDefined

    def name: Any = {
      args.get(Symbol("_name")).map(n => translate(n)(p)).getOrElse(p.messages(field.label))
    }
  }

  trait FieldConstructor {
    def apply(elts: FieldElements): Html
  }

  object FieldConstructor {
    implicit val defaultField: FieldConstructor = FieldConstructor(views.html.helper.defaultFieldConstructor.f)

    def apply(f: FieldElements => Html): FieldConstructor = new FieldConstructor {
      def apply(elts: FieldElements) = f(elts)
    }

    implicit def inlineFieldConstructor(f: (FieldElements) => Html): FieldConstructor = FieldConstructor(f)
    implicit def templateAsFieldConstructor(t: Template1[FieldElements, Html]): FieldConstructor =
      FieldConstructor(t.render)
  }

  object repeat extends RepeatHelper {

    /**
     * Render a field a repeated number of times.
     *
     * Useful for repeated fields in forms.
     *
     * @param field The field to repeat.
     * @param min The minimum number of times the field should be repeated.
     * @param fieldRenderer A function to render the field.
     * @return The sequence of rendered fields.
     */
    def apply(field: play.api.data.Field, min: Int = 1)(fieldRenderer: play.api.data.Field => Html): Seq[Html] = {
      indexes(field, min).map(i => fieldRenderer(field("[" + i + "]")))
    }
  }

  object repeatWithIndex extends RepeatHelper {

    /**
     * Render a field a repeated number of times.
     *
     * Useful for repeated fields in forms.
     *
     * @param field The field to repeat.
     * @param min The minimum number of times the field should be repeated.
     * @param fieldRenderer A function to render the field.
     * @return The sequence of rendered fields.
     */
    def apply(field: play.api.data.Field, min: Int = 1)(
        fieldRenderer: (play.api.data.Field, Int) => Html
    ): Seq[Html] = {
      indexes(field, min).map(i => fieldRenderer(field("[" + i + "]"), i))
    }
  }

  trait RepeatHelper {
    protected def indexes(field: play.api.data.Field, min: Int): Seq[Int] = field.indexes match {
      case Nil                              => 0 until min
      case complete if complete.size >= min => field.indexes
      case partial                          =>
        // We don't have enough elements, append indexes starting from the largest
        val start  = field.indexes.max + 1
        val needed = min - field.indexes.size
        field.indexes ++ (start until (start + needed))
    }
  }

  object options {
    def apply(options: (String, String)*): Seq[(String, String)]             = options.toSeq
    def apply(options: Map[String, String]): Seq[(String, String)]           = options.toSeq
    def apply(options: java.util.Map[String, String]): Seq[(String, String)] = options.asScala.toSeq
    def apply(options: List[String]): List[(String, String)]                 = options.map(v => v -> v)
    def apply(options: java.util.List[String]): Seq[(String, String)]        = options.asScala.toSeq.map(v => v -> v)
  }

  object Implicits {
    implicit def toAttributePair(pair: (String, String)): (Symbol, String) = Symbol(pair._1) -> pair._2
  }
}
