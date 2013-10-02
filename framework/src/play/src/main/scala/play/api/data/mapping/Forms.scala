package play.api.data.mapping

import play.api.{ data => d }
import play.api.data.mapping.PM._

object Form {
  def fill[T](t: T)(implicit w: Write[T, Map[String, Seq[String]]]) =
    Form().fill(t)
}

case class Form[T](data: Map[String, Seq[String]] = Map.empty, validation: Validation[(Path, Seq[ValidationError]), T] = Failure(Nil)) {

  lazy val hasErrors: Boolean = !errors.isEmpty

  val dataP = PM.toPM(data)
  lazy val errors: Seq[(Path, Seq[ValidationError])] =
    validation.fold(identity, _ => Nil)
  lazy val value: Option[T] =
    validation.fold(_ => None, Some(_))

  def apply(key: String): Field = apply(PM.asPath(key))

  def apply(path: Path): Field = {
    val value = dataP.get(path).flatMap(_.headOption)
    Field(this, path, value)
  }

  def fill(t: T)(implicit w: Write[T, Map[String, Seq[String]]]) =
    this.copy(data = w.writes(t))

  def fold = validation.fold _

  lazy val globalError: Option[ValidationError] = globalErrors.headOption
  lazy val globalErrors: Seq[ValidationError] = errors.filter(_._1 == Path).flatMap(_._2)
}

class Field(private val form: Form[_], val path: Path, override val value: Option[String])
	extends d.Field {

  override val constraints = Nil
  override val format = None

  override def apply(key: String): Field =
    apply(PM.asPath(key))

  def apply(index: Int): Field =
    apply(Path \ index)

  override val errors =
    form.errors
      .flatMap {
        case (p, errs) if p.path.startsWith(path.path) =>
          errs.map(e => d.FormError(PM.asKey(p), e.message))
        case _ => Nil
      }

  def apply(_path: Path): Field = {
    val p = path ++ _path
    val d = PM.find(p)(form.dataP).get(Path).flatMap(_.headOption)
    Field(form, p, d)
  }

  override val name = PM.asKey(path)

  override lazy val indexes: Seq[Int] = {
    PM.find(path)(form.dataP).keys
      .flatMap{
        case Path(Seq(IdxPathNode(i))) \: _ => Seq(i)
        case _ => Seq()
      }.toSeq
  }

  override def toString = s"Field($form, $path, $value)"
}

object Field {
	def apply(form: Form[_], path: Path, value: Option[String]) = new Field(form, path, value)
  def unapply(f: Field) = Some((f.form, f.path, f.value))
}