package play.api.data.mapping

import play.api.{ data => d }

object Form {

	import scala.util.parsing.combinator.RegexParsers
	object PathParser extends RegexParsers {
	  override type Elem = Char
	  def int 	= """\d""".r ^^ { _.toInt }
	  def key  = """\w+""".r ^^ { KeyPathNode(_) }
	  def idx 	= "[" ~> int <~ "]" ^^ { IdxPathNode(_) }
	  def node  = key ~ opt(idx) ^^ { case k ~ i => k :: i.toList }
	  def path  = repsep(node, ".") ^^ { ns => Path(ns.flatten) }

	  def parse(s: String) = parseAll(path, new scala.util.parsing.input.CharArrayReader(s.toArray))
	}

  def asKey(p: Path) = p.path.head.toString + p.path.tail.foldLeft("") {
    case (path, IdxPathNode(i)) => path + s"[$i]"
    case (path, KeyPathNode(k)) => path + "." + k
  }

  def asPath(k: String) = PathParser.parse(k).get

  def fill[T](t: T)(implicit w: Writes[T, Map[String, Seq[String]]]) = Form(w.writes(t), Nil, Some(t))

  def apply[T](data: Map[String, Seq[String]], validation: VA[Map[String, Seq[String]], T]): Form[T] = validation match {
  	case Success(d) => Form(data, Nil, Some(d))
  	case Failure(es) => Form(data, es, None)
  }
}

case class Form[T](data: Map[String, Seq[String]] = Map.empty, errors: Seq[(Path, Seq[ValidationError])] = Nil, value: Option[T] = None) {
  lazy val hasErrors: Boolean = !errors.isEmpty

  def apply(key: String): Field = apply(Form.asPath(key))

  def apply(path: Path): Field = {
    val value = data.get(Form.asKey(path)).flatMap(_.headOption)
    Field(this, path, value)
  }
}

class Field(private val form: Form[_], val path: Path, override val value: Option[String])
	extends d.Field(null, Form.asKey(path), Nil, None, Nil, value) { // TODO: find something better than this ugly null, handle constraints and format

  override def apply(key: String): Field =
    apply(Form.asPath(key))

  def apply(index: Int): Field =
    apply(Path() \ index)

  override val errors = form.errors.filter(_._1.path.startsWith(path.path))
    .flatMap { case (p, errs) =>
      errs.map(e => d.FormError(Form.asKey(p), e.message))
    }

  def apply(_path: Path): Field = {
    val expanded = path.compose(_path)
    val prefix = Form.asKey(expanded)
    val d = form.data.filter(_._1.startsWith(prefix)).map(_._2).flatten
    Field(form, expanded, d.headOption)
  }

  override val name = Form.asKey(path)

  override lazy val indexes: Seq[Int] = {
    form.data.keys.map(Form.asPath)
      .filter(_.path.startsWith(path.path))
      .map(_.path.drop(path.path.length).head)
      .flatMap{
        case IdxPathNode(i) => Seq(i)
        case _ => Seq()
      }.toSeq
  }
}

object Field {
	def apply(form: Form[_], path: Path, value: Option[String]) =
		new Field(form, path, value)
}