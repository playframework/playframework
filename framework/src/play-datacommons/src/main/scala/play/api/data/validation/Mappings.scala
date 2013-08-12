package play.api.data.validation

object Mappings {
  import scala.language.implicitConversions
  import play.api.libs.functional._
  import play.api.libs.functional.syntax._
  /*import play.api.mvc.Request*/

  implicit def monoidConstraint[T] = new Monoid[Constraint[T]] {
    def append(c1: Constraint[T], c2: Constraint[T]) = v => c1(v) *> (c2(v))
    def identity = Constraints.noConstraint[T]
  }

  implicit def IasI[I]: Mapping[ValidationError, I, I] = Success(_)

  implicit def seqAsO[O](implicit m: Mapping[ValidationError, String, O]): Mapping[ValidationError, Seq[String], O] =
    _.headOption.map(Success[ValidationError, String](_)).getOrElse(Failure[ValidationError, String](Seq(ValidationError("validation.required")))).flatMap(m)

  implicit def seqAsSeq[O](implicit m: Mapping[ValidationError, String, O]): Mapping[ValidationError, Seq[String], Seq[O]] =
    data => Validation.sequence(data.map(m))

  implicit def stringAsInt: Mapping[ValidationError, String, Int] =
    Constraints.validateWith("validation.type-mismatch", "Int"){ (_: String).matches("-?[0-9]+") }(_).map(_.toInt)

  /*
  import play.api.libs.json.{ KeyPathNode => JSKeyPathNode, IdxPathNode => JIdxPathNode, _ }
  private def pathToJsPath(p: Path[JsValue]) =
    play.api.libs.json.JsPath(p.path.map{
      case KeyPathNode(key) => JSKeyPathNode(key)
      case IdxPathNode(i) => JIdxPathNode(i)
    })

  implicit def jsonAsString: Mapping[String, JsValue, String] = {
    case JsString(v) => Success(v)
    case _ => Failure(Seq("validation.type-mismatch"))
  }

  implicit def jsonAsInt: Mapping[String, JsValue, Int] = {
    case JsNumber(v) => Success(v.toInt) // XXX
    case _ => Failure(Seq("validation.type-mismatch"))
  }

  implicit def jsonAsSeq[O](implicit m: Mapping[String, JsValue, O]): Mapping[String, JsValue, Seq[O]] = {
    case JsArray(vs) => Validation.sequence(vs.map(m))
    case _ => Failure(Seq("validation.type-mismatch"))
  }
  */

  /*
  implicit def pickInRequest[I, O](p: Path[Request[I]])(implicit pick: Path[I] => Mapping[String, I, O]): Mapping[String, Request[I], O] =
    request => pick(Path[I](p.path))(request.body)
  */

  implicit def pickOptional[I, O](p: Path[I])(implicit pick: Path[I] => Mapping[ValidationError, I, O]): Mapping[ValidationError, I, Option[O]] =
    d => pick(p)(d).map(Some.apply) | Success(None)

  /*
  implicit def pickInJson[O](p: Path[JsValue])(implicit m: Mapping[String, JsValue, O]): Mapping[String, JsValue, O] = { json =>
    val v: Validation[String, JsValue] = pathToJsPath(p)(json) match {
      case Nil => Failure(Seq("validation.required"))
      case js :: _ => Success(js)
    }
    v.flatMap(m)
  }
  */

  type M = Map[String, Seq[String]]

  private def toMapKey(p: Path[M]) = p.path.head.toString + p.path.tail.foldLeft("") {
    case (path, IdxPathNode(i)) => path + s"[$i]"
    case (path, KeyPathNode(k)) => path + "." + k
  }

  implicit def pickInMap[O](p: Path[M])(implicit m: Mapping[ValidationError, Seq[String], O]): Mapping[ValidationError, M, O] = {
    data =>
      val key = toMapKey(p)
      val validation: Validation[ValidationError, Seq[String]] =
        data.get(key).map(Success[ValidationError, Seq[String]](_)).getOrElse{ Failure[ValidationError, Seq[String]](Seq(ValidationError("validation.required"))) }
      validation.flatMap(m)
  }

  implicit def pickSInMap[O](p: Path[M])(implicit m: Mapping[ValidationError, String, O]): Mapping[ValidationError, M, Seq[O]] = { data =>
    val prefix = toMapKey(p)
    val r = prefix + """\[([0-9]+)\]"""

    // TODO: DRY
    val ss: Seq[String] = data.filterKeys(_.matches(r)).groupBy { case (k, v) =>
      val r.r(index) = k
      index.toInt
    }.toSeq.sortBy(_._1)
    .flatMap( _._2.toSeq.map{ case (k, v) => v }).flatten
    Validation.sequence(ss.map(m))
  }

  implicit def mapPickMap(p: Path[M]): Mapping[ValidationError, M, M] = { data =>
    val prefix = toMapKey(p) + "."
    val submap = data.filterKeys(_.startsWith(prefix)).map { case (k, v) =>
      k.substring(prefix.length) -> v
    }
    Success(submap)
  }

  implicit def mapPickSeqMap(p: Path[M]): Mapping[ValidationError, M, Seq[M]] = { data =>
    val prefix = toMapKey(p)
    val r = prefix + """\[([0-9]+)\]*\.(.*)"""

    // XXX: ugly and clearly not efficient
    val submaps: Seq[M] = data.filterKeys(_.matches(r)).groupBy { case (k, v) =>
      val r.r(index, name) = k
      index.toInt
    }.toSeq.sortBy(_._1).map(_._2).map( _.map{ case (k, v) =>
        val r.r(index, name) = k
        name -> v
    })
    Success(submaps)
  }

}