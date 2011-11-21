import play.api.templates._

import scala.collection.JavaConverters._

package views.html.helper {

  case class InputElements(label: Html, input: Html, errors: Seq[String], infos: Seq[String])
  case class RadioElements(input: Html, label: Html)

  trait ElementsConstructor[A] extends NotNull {
    def apply(elts: A): Html
  }

  trait InputConstructor extends ElementsConstructor[InputElements]
  trait RadioConstructor extends ElementsConstructor[RadioElements]

  object InputConstructor {

    implicit val defaultInput = InputConstructor(views.html.helper.defaultInputHandler.f)

    def apply(f: (InputElements) => Html): InputConstructor = new InputConstructor {
      def apply(elts: InputElements) = f(elts)
    }

    implicit def inlineInputConstructor(f: (InputElements) => Html) = InputConstructor(f)
    implicit def templateAsInputConstructor(t: Template1[InputElements, Html]) = InputConstructor(t.render)

  }

  object RadioConstructor {

    implicit val defaultRadio = RadioConstructor(views.html.helper.defaultRadioHandler.f)

    def apply(f: (RadioElements) => Html): RadioConstructor = new RadioConstructor {
      def apply(elts: RadioElements) = f(elts)
    }

    implicit def inlineRadioConstructor(f: (RadioElements) => Html) = RadioConstructor(f)
    implicit def templateAsRadioConstructor(t: Template1[RadioElements, Html]) = RadioConstructor(t.render)

  }

  object Utils {

    def filter(args: Seq[(Symbol, Any)], keysWithDefault: (Symbol, String)*) = {
      val keys = keysWithDefault.map(_._1)
      val (values, remainingArgs) = args.partition(a => keys.contains(a._1))
      (keysWithDefault.toMap ++ values.map(e => e._1 -> e._2.toString).toMap) -> remainingArgs
    }

  }

  object options {

    def apply(options: (String, String)*) = options.toSeq
    def apply(options: Map[String, String]) = options.toSeq
    def apply(options: java.util.Map[String, String]) = options.asScala.toSeq
    def apply(options: Iterable[Any]) = options.map(v => v -> v)

  }

}