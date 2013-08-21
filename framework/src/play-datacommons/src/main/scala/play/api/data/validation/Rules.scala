package play.api.data.validation

object Rules {
  import scala.language.implicitConversions
  import play.api.libs.functional._
  import play.api.libs.functional.syntax._
  // import play.api.mvc.Request

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = Constraints.noConstraint[T]
  }

  implicit def seqAsO[O](implicit r: Rule[String, O]) = Rule.fromMapping[Seq[String], String] {
    _.headOption.map(Success[ValidationError, String](_)).getOrElse(Failure[ValidationError, String](Seq(ValidationError("validation.required"))))
  }.compose(Path[Seq[String]]())(r)

  private def stringAs[T](f: PartialFunction[BigDecimal, Validation[ValidationError, T]])(args: Any*) =
    Rule.fromMapping[String, T]{
      val toB: PartialFunction[String, BigDecimal] = { case s if s.matches("""[-+]?[0-9]*\.?[0-9]+""") => BigDecimal(s) }
      toB.lift(_)
        .flatMap(f.lift)
        .getOrElse(Failure(Seq(ValidationError("validation.type-mismatch", args: _*))))
    }

  implicit def stringAsInt = stringAs {
    case s if s.isValidInt => Success(s.toInt)
  }("Int")

  implicit def stringAsShort = stringAs {
    case s if s.isValidShort => Success(s.toShort)
  }("Short")

  implicit def stringAsLong = stringAs {
    case s if s.isValidLong => Success(s.toLong)
  }("Long")

   // BigDecimal.isValidFloat is buggy, see [SI-6699]
  import java.{lang => jl}
  private def isValidFloat(bd: BigDecimal) = {
    val d = bd.toFloat
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Float.toString(d), bd.mc)) == 0
  }
  implicit def stringAsFloat = stringAs {
    case s if isValidFloat(s) => Success(s.toFloat)
  }("Float")

  // BigDecimal.isValidDouble is buggy, see [SI-6699]
  private def isValidDouble(bd: BigDecimal) = {
    val d = bd.toDouble
    !d.isInfinity && bd.bigDecimal.compareTo(new java.math.BigDecimal(jl.Double.toString(d), bd.mc)) == 0
  }
  implicit def stringAsDouble = stringAs {
    case s if isValidDouble(s) => Success(s.toDouble)
  }("Double")

  import java.{ math => jm }
  implicit def stringAsJavaBigDecimal = stringAs {
    case s => Success(s.bigDecimal)
  }("BigDecimal")

   implicit def stringAsBigDecimal = stringAs {
    case s => Success(s)
  }("BigDecimal")


  // implicit def pickInRequest[I, O](p: Path[Request[I]])(implicit pick: Path[I] => Mapping[String, I, O]): Mapping[String, Request[I], O] =
  //   request => pick(Path[I](p.path))(request.body)

  def opt[I, J, O](path: Path[I])(implicit pick: Path[I] => Rule[I, J], c:  Rule[J, O]) =
    Rule[I, Option[O]] {
      (d: I) =>
        (pick(path).validate(d).map(Some.apply) | Success(None))
          .flatMap {
            case None => Success(None)
            case Some(i) => c.validate(i).map(Some.apply)
          }.fail.map(_.map{ case (p, es) => p.as[I] -> es })
  }

  implicit def optR[I, O](path: Path[I])(implicit pick: Path[I] => Rule[I, I], c:  Rule[I, O]) =
    opt[I, I, O](path)(pick, c)

  implicit def optMap[O](path: Path[M])(implicit pick: Path[M] => Rule[M, Seq[String]], c:  Rule[Seq[String], O]) =
    opt[M, Seq[String], O](path)(pick, c)

  type M = Map[String, Seq[String]]

  private def toMapKey(p: Path[M]) = p.path.head.toString + p.path.tail.foldLeft("") {
    case (path, IdxPathNode(i)) => path + s"[$i]"
    case (path, KeyPathNode(k)) => path + "." + k
  }

  implicit def pickInMap[O](p: Path[M])(implicit r: Rule[Seq[String], O]): Rule[M, O] = Rule.fromMapping[M, Seq[String]] {
    data =>
      val key = toMapKey(p)
      val validation: Validation[ValidationError, Seq[String]] =
        data.get(key).map(Success[ValidationError, Seq[String]](_)).getOrElse{ Failure[ValidationError, Seq[String]](Seq(ValidationError("validation.required"))) }
      validation
  }.compose(Path[M]())(r)

  implicit def mapPickMap(p: Path[M]) = Rule.fromMapping[M, M] { data =>
    val prefix = toMapKey(p) + "."
    val submap = data.filterKeys(_.startsWith(prefix)).map { case (k, v) =>
      k.substring(prefix.length) -> v
    }
    Success(submap)
  }
}