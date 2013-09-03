package play.api.data.mapping

object PM {
  import Rules.PM

  import scala.util.parsing.combinator.{ Parsers, RegexParsers }
  object PathParser extends RegexParsers {
    override type Elem = Char
    def int   = """\d""".r ^^ { _.toInt }
    def idx   = "[" ~> int <~ "]" ^^ { IdxPathNode(_) }
    def key   = rep1(not("." | idx) ~> ".".r) ^^ { ks => KeyPathNode(ks.mkString) }
    def node  = key ~ opt(idx) ^^ { case k ~ i => k :: i.toList }
    def path  = (opt(idx) ~ repsep(node, ".")) ^^ { case i ~ ns => Path(i.toList ::: ns.flatten) }

    def parse(s: String) = parseAll(path, new scala.util.parsing.input.CharArrayReader(s.toArray))
  }

  def find(path: Path)(data: PM): PM = data.flatMap {
    case (p, errs) if p.path.startsWith(path.path) =>
      Map(Path(p.path.drop(path.path.length)) -> errs)
    case _ =>
      Map.empty[Path, Seq[String]]
  }

  def toPM(m: Map[String, Seq[String]]) =
    m.map { case (p, v) => asPath(p) -> v }

  def toM(m: Map[Path, Seq[String]]) =
    m.map { case (p, v) => asKey(p) -> v }

  private def asNodeKey(n: PathNode): String = n match {
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
}

object Rules extends DefaultRules[Map[String, Seq[String]]] {
  import scala.language.implicitConversions
  import play.api.libs.functional._
  import play.api.libs.functional.syntax._
  // import play.api.mvc.Request

  def string = IasI[String]

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

  type M  = Map[String, Seq[String]]
  type PM = Map[Path, Seq[String]]

  def map[O](r: Rule[Seq[String], O]): Rule[M, Map[String, O]] = {
    val toSeq = Rule.zero[M].fmap(_.toSeq)
    super.map[Seq[String], O](r,  toSeq)
  }

  implicit def pickInMap(p: Path) = Rule.fromMapping[M, Seq[String]] {
    data =>
      PM.find(p)(PM.toPM(data)).toSeq.flatMap {
        case (Path(Nil) | Path(Seq(IdxPathNode(_))), ds) => ds
        case _ => Nil
      } match {
        case Nil => Failure[ValidationError, Seq[String]](Seq(ValidationError("validation.required")))
        case m => Success[ValidationError, Seq[String]](m)
      }
  }

  private def seqAsString = Rule.fromMapping[Seq[String], String] {
    _.headOption.map(Success[ValidationError, String](_)).getOrElse(Failure[ValidationError, String](Seq(ValidationError("validation.required"))))
  }

  implicit def pickOne[O](p: Path): Rule[M, String] =
    pickInMap(p) compose seqAsString

  implicit def mapPickMap(path: Path) = Rule.fromMapping[M, M] { data =>
    Success(PM.toM(PM.find(path)(PM.toPM(data))))
  }

  implicit def mapPickSeqMap(p: Path) = Rule.fromMapping[M, Seq[M]]({ data =>
    val grouped = PM.find(p)(PM.toPM(data)).toSeq.flatMap {
      case (Path(IdxPathNode(i) :: Nil) \: t, vs) => Seq(i -> Map(t -> vs))
      case _ => Nil
    }.groupBy(_._1).mapValues(_.map(_._2)) // returns all the submap, grouped by index

    val submaps = grouped.toSeq.map {
      case (i, ms) => i -> ms.foldLeft(Map.empty[Path, Seq[String]]) { _ ++ _ } // merge the submaps by index
    }.sortBy(_._1).map(e => PM.toM(e._2))

    Success(submaps)
  })

}