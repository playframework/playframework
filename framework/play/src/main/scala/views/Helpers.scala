import play.api.templates._

import scala.collection.JavaConverters._

package views.html.helper {

  object Utils {

    def filter(args: Seq[(Symbol, Any)], keysWithDefault: (Symbol, String)*) = {
      val keys = keysWithDefault.map(_._1)
      val (values, remainingArgs) = args.partition(a => keys.contains(a._1))
      (keysWithDefault.toMap ++ values.map(e => e._1 -> e._2.toString).toMap) -> remainingArgs
    }

  }

  object select {

    type InputHandler = (Html, Html, Seq[String], Seq[String]) => Html
    type HtmlArgs = (Symbol, Any)

    def apply(field: play.api.data.Field, options: Map[Any, Any], args: HtmlArgs*)(handler: InputHandler): Html = {
      genericSelect(field, options.map { o =>
        Option(o._1).map(_.toString).getOrElse("") -> Option(o._2).map(_.toString).getOrElse("")
      }.toSeq, args: _*)(handler)
    }

    def apply(field: play.api.data.Field, options: java.util.Map[_, _], args: HtmlArgs*)(handler: InputHandler): Html = {
      apply(field, options.asScala, args: _*)(handler)
    }

    def apply(field: play.api.data.Field, options: Iterable[Any], args: HtmlArgs*)(handler: InputHandler): Html = {
      genericSelect(field, options.map {
        case (k, v) => Option(k).map(_.toString).getOrElse("") -> Option(v).map(_.toString).getOrElse("")
        case o => Option(o).map(_.toString).getOrElse("") -> Option(o).map(_.toString).getOrElse("")
      }.toSeq, args: _*)(handler)
    }

  }

  object `package` {
    val defaultInput = defaultInputHandler.f
  }

}