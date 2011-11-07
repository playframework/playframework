import play.api.templates._

import scala.collection.JavaConverters._

package views.html.helper {

  case class InputElements(label: Html, input: Html, errors: Seq[String], infos: Seq[String])

  trait InputConstructor extends NotNull {
    def apply(elts: InputElements): Html
  }

  object InputConstructor {

    implicit val defaultInput = InputConstructor(views.html.helper.defaultInputHandler.f)

    def apply(f: (InputElements) => Html): InputConstructor = new InputConstructor {
      def apply(elts: InputElements) = f(elts)
    }

    implicit def inlineInputConstructor(f: (InputElements) => Html) = InputConstructor(f)
    implicit def templateAsInputConstructor(t: Template1[InputElements, Html]) = InputConstructor(t.render)

  }

  object Utils {

    def filter(args: Seq[(Symbol, Any)], keysWithDefault: (Symbol, String)*) = {
      val keys = keysWithDefault.map(_._1)
      val (values, remainingArgs) = args.partition(a => keys.contains(a._1))
      (keysWithDefault.toMap ++ values.map(e => e._1 -> e._2.toString).toMap) -> remainingArgs
    }

  }

  object select {

    type HtmlArgs = (Symbol, Any)

    def apply(field: play.api.data.Field, options: Map[String, String], args: HtmlArgs*)(implicit handler: InputConstructor): Html = {
      genericSelect(field, options.map { o => o._1 -> o._2 }.toSeq, args: _*)(handler)
    }

    def apply(field: play.api.data.Field, options: java.util.Map[_, _], args: HtmlArgs*)(implicit handler: InputConstructor): Html = {
      apply(field, options.asScala.map { o =>
        Option(o._1).map(_.toString).getOrElse("") -> Option(o._2).map(_.toString).getOrElse("")
      }, args: _*)(handler)
    }

    def apply(field: play.api.data.Field, options: Iterable[Any], args: HtmlArgs*)(implicit handler: InputConstructor): Html = {
      genericSelect(field, options.map {
        case (k, v) => Option(k).map(_.toString).getOrElse("") -> Option(v).map(_.toString).getOrElse("")
        case o => Option(o).map(_.toString).getOrElse("") -> Option(o).map(_.toString).getOrElse("")
      }.toSeq, args: _*)(handler)
    }

  }

}