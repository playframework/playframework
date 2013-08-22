package play.api.data.validation

object Rules extends DefaultRules {
  import scala.language.implicitConversions
  import play.api.libs.functional._
  import play.api.libs.functional.syntax._
  // import play.api.mvc.Request

  def string: Rule[String, String] = IasI

  private def stringAs[T](f: PartialFunction[BigDecimal, Validation[ValidationError, T]])(args: Any*) =
    Rule.fromMapping[String, T]{
      val toB: PartialFunction[String, BigDecimal] = { case s if s.matches("""[-+]?[0-9]*\.?[0-9]+""") => BigDecimal(s) }
      toB.lift(_)
        .flatMap(f.lift)
        .getOrElse(Failure(Seq(ValidationError("validation.type-mismatch", args: _*))))
    }

  def int = stringAs {
    case s if s.isValidInt => Success(s.toInt)
  }("Int")

  def short = stringAs {
    case s if s.isValidShort => Success(s.toShort)
  }("Short")

  def boolean = Rule.fromMapping[String, Boolean]{
    pattern("""(?iu)true|false""".r)(_: String)
      .map(java.lang.Boolean.parseBoolean)
      .fail.map(_ => Seq(ValidationError("validation.type-mismatch", "Boolean")))
  }

  def long = stringAs {
    case s if s.isValidLong => Success(s.toLong)
  }("Long")

   // BigDecimal.isValidFloat is buggy, see [SI-6699]
  import java.{lang => jl}
  private def isValidFloat(bd: BigDecimal) = {
    val d = bd.toFloat
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Float.toString(d), bd.mc)) == 0
  }
  def float = stringAs {
    case s if isValidFloat(s) => Success(s.toFloat)
  }("Float")

  // BigDecimal.isValidDouble is buggy, see [SI-6699]
  private def isValidDouble(bd: BigDecimal) = {
    val d = bd.toDouble
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Double.toString(d), bd.mc)) == 0
  }
  def double = stringAs {
    case s if isValidDouble(s) => Success(s.toDouble)
  }("Double")

  import java.{ math => jm }
  def javaBigDecimal = stringAs {
    case s => Success(s.bigDecimal)
  }("BigDecimal")

  def bigDecimal = stringAs {
    case s => Success(s)
  }("BigDecimal")


  // implicit def pickInRequest[I, O](p: Path[Request[I]])(implicit pick: Path[I] => Mapping[String, I, O]): Mapping[String, Request[I], O] =
  //   request => pick(Path[I](p.path))(request.body)

  type M = Map[String, Seq[String]]

  private def toMapKey(p: Path) = p.path.head.toString + p.path.tail.foldLeft("") {
    case (path, IdxPathNode(i)) => path + s"[$i]"
    case (path, KeyPathNode(k)) => path + "." + k
  }

  implicit def pickInMap(p: Path) = Rule.fromMapping[M, Seq[String]] {
    data =>
      val key = toMapKey(p)
      val validation: Validation[ValidationError, Seq[String]] =
        data.get(key).map(Success[ValidationError, Seq[String]](_)).getOrElse{ Failure[ValidationError, Seq[String]](Seq(ValidationError("validation.required"))) }
      validation
  }

  private def seqAsString = Rule.fromMapping[Seq[String], String] {
    _.headOption.map(Success[ValidationError, String](_)).getOrElse(Failure[ValidationError, String](Seq(ValidationError("validation.required"))))
  }

  implicit def pickOne[O](p: Path) =  pickInMap(p) compose seqAsString


  implicit def mapPickMap(p: Path) = Rule.fromMapping[M, M] { data =>
    val prefix = toMapKey(p) + "."
    val submap = data.filterKeys(_.startsWith(prefix)).map { case (k, v) =>
      k.substring(prefix.length) -> v
    }
    Success(submap)
  }

}