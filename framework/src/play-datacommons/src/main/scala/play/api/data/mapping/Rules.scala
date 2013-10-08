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
    def int   = """\d""".r ^^ { _.toInt }
    def idx   = "[" ~> int <~ "]" ^^ { IdxPathNode(_) }
    def key   = rep1(not("." | idx) ~> ".".r) ^^ { ks => KeyPathNode(ks.mkString) }
    def node  = key ~ opt(idx) ^^ { case k ~ i => k :: i.toList }
    def path  = (opt(idx) ~ repsep(node, ".")) ^^ { case i ~ ns => Path(i.toList ::: ns.flatten) }

    def parse(s: String) = parseAll(path, new scala.util.parsing.input.CharArrayReader(s.toArray))
  }

  type PM = Map[Path, Seq[String]]

  /**
  * Find a sub-Map of all the elements at a Path starting with `path`
  * @param path The prefix to look for
  * @param data The map in which you want to lookup
  * @return a sub Map. If no key of `data` starts with `path`, this map will be empty
  */
  def find(path: Path)(data: PM): PM = data.flatMap {
    case (p, errs) if p.path.startsWith(path.path) =>
      Map(Path(p.path.drop(path.path.length)) -> errs)
    case _ =>
      Map.empty[Path, Seq[String]]
  }

  /**
  * Apply `f` to all the keys of `m`
  */
  def repathPM(m: PM, f: Path => Path): PM
    = m.map{ case (p, v) => f(p) -> v }

  /**
  * Apply `f` to all the keys of `m`
  */
  def repath(m: UrlFormEncoded, f: Path => Path): UrlFormEncoded
    = toM(repathPM(toPM(m), f))

  /**
  * Convert a Map[String, Seq[String]] to a Map[Path, Seq[String]]
  */
  def toPM(m: UrlFormEncoded): PM =
    m.map { case (p, v) => asPath(p) -> v }

  /**
  * Convert a Map[Path, Seq[String]] to a Map[String, Seq[String]]
  */
  def toM(m: PM): UrlFormEncoded =
    m.map { case (p, v) => asKey(p) -> v }

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
    case (path, n@IdxPathNode(i)) => path + asNodeKey(n)
    case (path, n@KeyPathNode(k)) => path + "." + asNodeKey(n)
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

  implicit def map[O](implicit r: Rule[Seq[String], O]): Rule[PM, Map[String, O]] =
    super.map[Seq[String], O](r, Rule.zero[PM].fmap{ toM(_).toSeq })

  implicit def option[O](implicit pick: Path => Rule[PM, PM], coerce: Rule[PM, O]): Path => Rule[PM, Option[O]] =
    opt(coerce)

  def option[J, O](r: Rule[J, O], noneValues: Rule[J, J]*)(implicit pick: Path => Rule[PM, J]): Path => Rule[UrlFormEncoded, Option[O]] =
    path => {
      val o = opt[J, O](r, noneValues: _*)(pick)(path)
      Rule.zero[UrlFormEncoded].fmap(toPM).compose(o)
    }

  implicit def parse[O](implicit r: Rule[String, O]): Rule[PM, O] =
    Rule.zero[PM]
      .fmap(_.get(Path).toSeq.flatten)
      .compose(headAs(r))

  implicit def inArray[O: scala.reflect.ClassTag](implicit r: Rule[Seq[PM], Array[O]]): Rule[PM, Array[O]] =
    inT[O, Traversable](r.fmap(_.toTraversable)).fmap(_.toArray)

  // PM should be Map[Path, String] and not Map[Path, Seq[String]]
  implicit def inT[O, T[_] <: Traversable[_]](implicit r: Rule[Seq[PM], T[O]]): Rule[PM, T[O]] =
    Rule.zero[PM].fmap { pm =>
      val grouped = pm.toSeq.flatMap {
        case (Path, vs) => Seq(0 -> Map(Path -> vs))
        case (Path(IdxPathNode(i) :: Nil) \: t, vs) => Seq(i -> Map(t -> vs))
        case _ => Nil
      }.groupBy(_._1).mapValues(_.map(_._2)) // returns all the submap, grouped by index

      val e: Seq[PM] = grouped.toSeq.map {
        case (i, ms) => i -> ms.foldLeft(Map.empty[Path, Seq[String]]) { _ ++ _ } // merge the submaps by index
      }
      .sortBy(_._1)
      .map(e => e._2)

      // split Root path
      e.flatMap { m =>
        val (roots, others) = m.partition(_._1 == Path)
        val rs: Seq[PM] = roots.toSeq.flatMap { case (_, vs) =>
          vs.map(v => Map(Path() -> Seq(v)))
        }

        (Seq(others) ++ rs).filter(!_.isEmpty)
      }
    }.compose(r)

  implicit def pickInPM[O](p: Path)(implicit r: Rule[PM, O]): Rule[PM, O] =
    Rule[PM, PM] { pm =>
      PM.find(p)(pm) match {
        case sub if sub.isEmpty => Failure(Seq(Path -> Seq(ValidationError("validation.required"))))
        case sub => Success(sub)
      }
    }.compose(r)

  // Convert Rules exploring PM, to Rules exploring UrlFormEncoded
  implicit def pickInM[O](p: Path)(implicit r: Path => Rule[PM, O]): Rule[UrlFormEncoded, O] =
    Rule.zero[UrlFormEncoded]
      .fmap(toPM)
      .compose(r(p))

  implicit def convert: Rule[PM, UrlFormEncoded] =
    Rule.zero[PM].fmap(toM)
}