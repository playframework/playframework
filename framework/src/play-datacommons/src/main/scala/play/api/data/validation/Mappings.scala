package play.api.data.validation

object Mappings {
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

  implicit def seqAsSeq[O](implicit r: Rule[String, O]): Rule[Seq[String], Seq[O]] =
    Constraints.seq(r)

  implicit def stringAsInt = Rule.fromMapping[String, Int] {
    Constraints.validateWith("validation.type-mismatch", "Int"){ (_: String).matches("-?[0-9]+") }(_: String).map(_.toInt)
  }


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

  implicit def pickInMap[O](p: Path[M])(implicit r: Rule[Seq[String], O]): Rule[M, O] =
    pickSInMap[String](p).compose(Path[M]())(r)

  def pickSInMap[O](p: Path[M])(implicit r: Rule[String, O]): Rule[M, Seq[O]] = Rule.fromMapping[M, Seq[String]] {
    data =>
      val key = toMapKey(p)
      val validation: Validation[ValidationError, Seq[String]] =
        data.get(key).map(Success[ValidationError, Seq[String]](_)).getOrElse{ Failure[ValidationError, Seq[String]](Seq(ValidationError("validation.required"))) }
      validation
  }.compose(Path[M]())(Constraints.seq(r))

  implicit def mapPickMap(p: Path[M]) = Rule.fromMapping[M, M] { data =>
    val prefix = toMapKey(p) + "."
    val submap = data.filterKeys(_.startsWith(prefix)).map { case (k, v) =>
      k.substring(prefix.length) -> v
    }
    Success(submap)
  }
}