import play.api.templates._

import scala.collection.JavaConverters._

package views.html.helper {

  case class FieldElements(id: String, field: play.api.data.Field, input: Html, label: Option[String], showInfo: Boolean = true)

  trait FieldConstructor extends NotNull {
    def apply(elts: FieldElements): Html
  }

  object FieldConstructor {

    implicit val defaultField = FieldConstructor(views.html.helper.defaultFieldHandler.f)

    def apply(f: (FieldElements) => Html): FieldConstructor = new FieldConstructor {
      def apply(elts: FieldElements) = f(elts)
    }

    implicit def inlineFieldConstructor(f: (FieldElements) => Html) = FieldConstructor(f)
    implicit def templateAsFieldConstructor(t: Template1[FieldElements, Html]) = FieldConstructor(t.render)

  }

  object Utils {

    def filter(args: Seq[(Symbol, Any)], keysWithDefault: (Symbol, String)*) = {
      val keys = keysWithDefault.map(_._1)
      val (values, remainingArgs) = args.partition(a => keys.contains(a._1))
      (keysWithDefault.toMap ++ values.map(e => e._1 -> e._2.toString).toMap) -> remainingArgs
    }

  }
  
  object repeat {
    
    def apply(field: play.api.data.Field, min: Int = 1)(f: play.api.data.Field => Html) = {
      (0 until math.max(if(field.indexes.isEmpty) 0 else field.indexes.max + 1, min)).map(i => f(field("[" + i + "]")))
    }
    
  }

  object options {

    def apply(options: (String, String)*) = options.toSeq
    def apply(options: Map[String, String]) = options.toSeq
    def apply(options: java.util.Map[String, String]) = options.asScala.toSeq
    def apply(options: Iterable[Any]) = options.map(v => v -> v)

  }

}