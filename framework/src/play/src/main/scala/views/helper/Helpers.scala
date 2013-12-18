import play.api.templates._

import scala.language.implicitConversions

import scala.collection.JavaConverters._

package views.html.helper {

  import scala.collection.immutable

  case class FieldElements(id: String, field: play.api.data.Field, input: Html, args: Map[Symbol, Any], lang: play.api.i18n.Lang) {

    def infos(implicit lang: play.api.i18n.Lang): Seq[String] = {
      args.get('_help).map(m => Seq(m.toString)).getOrElse {
        (if (args.get('_showConstraints) match {
          case Some(false) => false
          case _ => true
        }) {
          field.constraints.map(c => play.api.i18n.Messages(c._1, c._2: _*)) ++
            field.format.map(f => play.api.i18n.Messages(f._1, f._2: _*))
        } else Nil)
      }
    }

    def errors(implicit lang: play.api.i18n.Lang): Seq[String] = {
      (args.get('_error) match {
        case Some(Some(play.api.data.FormError(_, message, args))) => Some(Seq(play.api.i18n.Messages(message, args: _*)))
        case _ => None
      }).getOrElse {
        (if (args.get('_showErrors) match {
          case Some(false) => false
          case _ => true
        }) {
          field.errors.map(e => play.api.i18n.Messages(e.message, e.args: _*))
        } else Nil)
      }
    }

    def hasErrors: Boolean = {
      !errors.isEmpty
    }

    def label(implicit lang: play.api.i18n.Lang): Any = {
      args.get('_label).getOrElse(play.api.i18n.Messages(field.name))
    }

  }

  trait FieldConstructor extends NotNull {
    def apply(elts: FieldElements): Html
  }

  object FieldConstructor {

    implicit val defaultField = FieldConstructor(views.html.helper.defaultFieldConstructor.f)

    def apply(f: FieldElements => Html): FieldConstructor = new FieldConstructor {
      def apply(elts: FieldElements) = f(elts)
    }

    implicit def inlineFieldConstructor(f: (FieldElements) => Html) = FieldConstructor(f)
    implicit def templateAsFieldConstructor(t: Template1[FieldElements, Html]) = FieldConstructor(t.render)

  }

  object repeat {

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
    def apply(field: play.api.data.Field, min: Int = 1)(fieldRenderer: play.api.data.Field => Html): immutable.IndexedSeq[Html] = {
      val indexes = field.indexes match {
        case Nil => 0 until min
        case complete if complete.size >= min => field.indexes
        case partial =>
          // We don't have enough elements, append indexes starting from the largest
          val start = field.indexes.max + 1
          val needed = min - field.indexes.size
          field.indexes ++ (start until (start + needed))
      }

      indexes.map(i => fieldRenderer(field("[" + i + "]"))).toIndexedSeq
    }
  }

  object options {

    def apply(options: (String, String)*) = options.toSeq
    def apply(options: Map[String, String]) = options.toSeq
    def apply(options: java.util.Map[String, String]) = options.asScala.toSeq
    def apply(options: List[String]) = options.map(v => v -> v)
    def apply(options: java.util.List[String]) = options.asScala.map(v => v -> v)

  }

}
