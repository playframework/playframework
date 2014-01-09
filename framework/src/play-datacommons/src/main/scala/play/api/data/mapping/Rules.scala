package play.api.data.mapping

/**
 * Play provides you a `Map[String, Seq[String]]` (aliased as `UrlFormEncoded`) in request body for urlFormEncoded requests.
 * It's generally a lot more convenient to work on `Map[Path, Seq[String]]` to define Rules.
 * This object contains methods used to convert `Map[String, Seq[String]]` <-> `Map[Path, Seq[String]]`
 * @note We use the alias `UrlFormEncoded`, which is just a `Map[String, Seq[String]]`
 */
object PM {

  import scala.util.parsing.combinator.{ Parsers, RegexParsers }
  /**
   * A parser converting a key of a Map[String, [Seq[String]]] to a Path instance
   * `foo.bar[0].baz` becomes `Path \ "foo" \ "bar" \ 0 \ "baz"`
   */
  object PathParser extends RegexParsers {
    override type Elem = Char
    def int = """\d""".r ^^ { _.toInt }
    def idx = "[" ~> int <~ "]" ^^ { IdxPathNode(_) }
    def key = rep1(not("." | idx) ~> ".".r) ^^ { ks => KeyPathNode(ks.mkString) }
    def node = key ~ opt(idx) ^^ { case k ~ i => k :: i.toList }
    def path = (opt(idx) ~ repsep(node, ".")) ^^ { case i ~ ns => Path(i.toList ::: ns.flatten) }

    def parse(s: String) = parseAll(path, new scala.util.parsing.input.CharArrayReader(s.toArray))
  }

  type PM = Map[Path, String]

  /**
   * Find a sub-Map of all the elements at a Path starting with `path`
   * @param path The prefix to look for
   * @param data The map in which you want to lookup
   * @return a sub Map. If no key of `data` starts with `path`, this map will be empty
   */
  def find(path: Path)(data: PM): PM = data.flatMap {
    case (p, v) if p.path.startsWith(path.path) =>
      Map(Path(p.path.drop(path.path.length)) -> v)
    case _ =>
      Map.empty[Path, String]
  }

  /**
   * Apply `f` to all the keys of `m`
   */
  def repathPM(m: PM, f: Path => Path): PM = m.map { case (p, v) => f(p) -> v }

  /**
   * Apply `f` to all the keys of `m`
   */
  def repath(m: UrlFormEncoded, f: Path => Path): UrlFormEncoded = toM(repathPM(toPM(m), f))

  /**
   * Convert a Map[String, Seq[String]] to a Map[Path, Seq[String]]
   */
  def toPM(m: UrlFormEncoded): PM =
    m.toSeq.flatMap {
      case (p, vs) =>
        vs match {
          case Seq(v) => vs.map { asPath(p) -> _ }
          case _ => vs.zipWithIndex.map { case (v, i) => (asPath(p) \ i) -> v }
        }
    }.toMap

  /**
   * Convert a Map[Path, Seq[String]] to a Map[String, Seq[String]]
   */
  def toM(m: PM): UrlFormEncoded =
    m.map { case (p, v) => asKey(p) -> Seq(v) }

  private def asNodeKey(n: PathNode): String = n match {
    case IdxPathNode(i) => s"[$i]"
    case KeyPathNode(k) => k
  }

  /**
   * Convert a Path to a String key
   * @param p The path to convert
   * @return A String representation of `p`
   */
  def asKey(p: Path): String = p.path.headOption.toList.map(asNodeKey).mkString ++ p.path.tail.foldLeft("") {
    case (path, n @ IdxPathNode(i)) => path + asNodeKey(n)
    case (path, n @ KeyPathNode(k)) => path + "." + asNodeKey(n)
  }

  /**
   * Convert a String key to a Path using `PathParser`
   * @param k The String representation of path to convert
   * @return a `Path`
   */
  def asPath(k: String): Path = PathParser.parse(k) match {
    case PathParser.Failure(m, _) => throw new RuntimeException(s"Invalid field name $k: $m")
    case PathParser.Error(m, _) => throw new RuntimeException(s"Invalid field name $k: $m")
    case PathParser.Success(r, _) => r
  }
}

/**
 * This object provides Rules for Map[String, Seq[String]]
 */
object Rules extends DefaultRules[PM.PM] with ParsingRules {
  import scala.language.implicitConversions
  import scala.language.higherKinds

  import play.api.libs.functional._
  import play.api.libs.functional.syntax._

  import PM._

  implicit def mapR[O](implicit r: RuleLike[Seq[String], O]): Rule[PM, Map[String, O]] =
    super.mapR[Seq[String], O](r, Rule.zero[PM].fmap { toM(_).toSeq })

  private val isEmpty = validateWith[PM]("validation.empty") { pm =>
    pm.filter { case (_, vs) => !vs.isEmpty }.isEmpty
  }
  implicit def optionR[O](implicit pick: Path => RuleLike[PM, PM], coerce: RuleLike[PM, O]): Path => Rule[PM, Option[O]] =
    opt(coerce, isEmpty)

  def optionR[J, O](r: => RuleLike[J, O], noneValues: RuleLike[PM, PM]*)(implicit pick: Path => RuleLike[PM, PM], coerce: RuleLike[PM, J]): Path => Rule[UrlFormEncoded, Option[O]] =
    path => {
      val nones = isEmpty +: noneValues
      val o = opt[J, O](r, nones: _*)(pick, coerce)(path)
      Rule.zero[UrlFormEncoded].fmap(toPM).compose(o)
    }

  implicit def parseString[O](implicit r: RuleLike[String, O]): Rule[PM, O] = {
    val find = Rule[Option[String], String] {
      _.map(Success(_)).getOrElse(Failure(Seq(Path -> Seq(ValidationError("error.required")))))
    }
    Rule.zero[PM]
      .fmap(_.get(Path))
      .compose(find)
      .compose(r)
  }

  implicit def inArray[O: scala.reflect.ClassTag](implicit r: RuleLike[Seq[PM], Array[O]]): Path => Rule[PM, Array[O]] =
    inT[O, Traversable](Rule.toRule(r).fmap(_.toTraversable))(_).fmap(_.toArray)

  implicit def inT[O, T[_] <: Traversable[_]](implicit r: RuleLike[Seq[PM], T[O]]): Path => Rule[PM, T[O]] =
    path =>
      pickInPM(path)(Rule.zero).orElse(Rule[PM, PM](_ => Success(Map.empty)))
        .fmap { pm =>
          val (root, others) = pm.partition(_._1 == Path)
          val arrays = others.toSeq.flatMap {
            case (Path(IdxPathNode(i) :: Nil) \: t, v) => Seq(i -> Map(t -> v))
            case _ => Nil
          }
            .groupBy(_._1).toSeq.sortBy(_._1)
            .map {
              case (i, pms) =>
                pms.map(_._2).foldLeft(Map.empty[Path, String]) { _ ++ _ }
            }

          (root +: arrays).filter(!_.isEmpty)
        }.compose(r)

  implicit def pickInPM[O](p: Path)(implicit r: RuleLike[PM, O]): Rule[PM, O] =
    Rule[PM, PM] { pm =>
      PM.find(p)(pm) match {
        case sub if sub.isEmpty => Failure(Seq(Path -> Seq(ValidationError("error.required"))))
        case sub => Success(sub)
      }
    }.compose(r)

  // Convert Rules exploring PM, to Rules exploring UrlFormEncoded
  implicit def convertToInM[O](p: Path)(implicit r: Path => RuleLike[PM, O]): Rule[UrlFormEncoded, O] =
    Rule.zero[UrlFormEncoded]
      .fmap(toPM)
      .compose(r(p))

  implicit def convertRule[O](implicit r: RuleLike[UrlFormEncoded, O]): Rule[PM, O] =
    Rule.zero[PM].fmap(toM).compose(r)
}