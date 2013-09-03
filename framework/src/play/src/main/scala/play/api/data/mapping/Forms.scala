package play.api.data.mapping

import play.api.{ data => d }

object Form {

	import scala.util.parsing.combinator.{ Parsers, RegexParsers }
	object PathParser extends RegexParsers {
	  override type Elem = Char
	  def int 	= """\d""".r ^^ { _.toInt }
	  def idx 	= "[" ~> int <~ "]" ^^ { IdxPathNode(_) }
    def key   = rep1(not("." | idx) ~> ".".r) ^^ { ks => KeyPathNode(ks.mkString) }
	  def node  = key ~ opt(idx) ^^ { case k ~ i => k :: i.toList }
	  def path  = (opt(idx) ~ repsep(node, ".")) ^^ { case i ~ ns => Path(i.toList ::: ns.flatten) }

    def parse(s: String) = parseAll(path, new scala.util.parsing.input.CharArrayReader(s.toArray))
	}

  def toPM(m: Map[String, Seq[String]]) =
    m.map { case (p, v) => asPath(p) -> v }

  def asNodeKey(n: PathNode): String = n match {
    case IdxPathNode(i) => s"[$i]"
    case KeyPathNode(k) => k
  }

  def asKey(p: Path): String = p.path.headOption.toList.map(asNodeKey).mkString ++ p.path.tail.foldLeft("") {
    case (path, n@IdxPathNode(i)) => path + asNodeKey(n)
    case (path, n@KeyPathNode(k)) => path + "." + asNodeKey(n)
  }

  def asPath(k: String): Path = PathParser.parse(k) match {
    case PathParser.Failure(m, _) => throw new RuntimeException(s"Invalid field name $k: $m")
    case PathParser.Error(m, _) => throw new RuntimeException(s"Invalid field name $k: $m")
    case PathParser.Success(r, _) => r
  }

  // def fill[T](t: T)(implicit w: Writes[T, Map[Path, Seq[String]]]) =
  //   Form(w.writes(t))
}

case class Form[T](data: Map[String, Seq[String]] = Map.empty, validation: Validation[(Path, Seq[ValidationError]), T] = Failure(Nil)) {lazy val hasErrors: Boolean = !errors.isEmpty

  val dataP = Form.toPM(data)
  lazy val errors: Seq[(Path, Seq[ValidationError])] =
    validation.fold(identity, _ => Nil)
  lazy val value: Option[T] =
    validation.fold(_ => None, Some(_))

  def apply(key: String): Field = apply(Form.asPath(key))

  def apply(path: Path): Field = {
    val value = dataP.get(path).flatMap(_.headOption)
    Field(this, path, value)
  }

  // def fill(t: T)(implicit w: Writes[T, Map[Path, Seq[String]]]) =
  //   Form.fill(t)
}

class Field(private val form: Form[_], val path: Path, override val value: Option[String])
	extends d.Field {

  override val constraints = Nil
  override val format = None

  override def apply(key: String): Field =
    apply(Form.asPath(key))

  def apply(index: Int): Field =
    apply(Path() \ index)

  override val errors =
    form.errors
      .flatMap {
        case (p, errs) if p.path.startsWith(path.path) =>
          errs.map(e => d.FormError(Form.asKey(p), e.message))
        case _ => Nil
      }

  def apply(_path: Path): Field = {
    val p = path ++ _path
    val d = form.dataP.flatMap {
      case (pa, errs) if pa.path.startsWith(p.path) => errs
      case _ => Nil
    }
    Field(form, p, d.headOption)
  }

  override val name = Form.asKey(path)

  override lazy val indexes: Seq[Int] = {
    form.dataP.keys
      .collect {
        case p if p.path.startsWith(path.path) =>
          p.path.drop(path.path.length).head
      }
      .flatMap{
        case IdxPathNode(i) => Seq(i)
        case _ => Seq()
      }.toSeq
  }

  override def toString = s"Field($form, ${path.path}, $value)"
}

object Field {
	def apply(form: Form[_], path: Path, value: Option[String]) = new Field(form, path, value)
  def unapply(f: Field) = Some((f.form, f.path, f.value))
}