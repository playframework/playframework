package play.api.data.mapping

trait From[I] {
  def apply[O](f: Reader[I] => RuleLike[I, O]): Rule[I, O] = Rule.toRule(f(Reader[I]()))
}
object From {
  /**
   * {{{
   *   val r = From[UrlFormEncoded]{ __ =>
   *     ((__ \ "firstname").read(notEmpty) ~
   *      (__ \ "age").read(min(1)).tupled
   *   }
   *   r.validate(valid) == Success("Julien" -> 28)
   * }}}
   */
  def apply[I] = new From[I] {}

  /**
   * Validate type `I` as an  using the implicit `Write` w
   * {{{
   *   val m = Map(
   *     "name" -> Seq("bob"),
   *     "friend.name" -> Seq("bobby"))
   *   From[UrlFormEncoded, Person](m) == Success(Person(List("bob", "bobby")))
   * }}}
   */
  def apply[I, O](i: I)(implicit r: RuleLike[I, O]) =
    r.validate(i)
}

trait To[I] {
  def apply[O](f: Writer[I] => WriteLike[O, I]): Write[O, I] = Write.toWrite(f(Writer[I]()))
}
object To {

  /**
   * {{{
   *   val w = To[UrlFormEncoded] { __ =>
   *     ((__ \ "email").write[Option[String]] ~
   *      (__ \ "phone").write[String]).tupled
   *   }
   *
   *   val v =  Some("jto@foobar.com") -> "01.23.45.67.89"
   *
   *    w.writes(v) == Map(
   *      "email" -> Seq("jto@foobar.com"),
   *      "phone" -> Seq("01.23.45.67.89"))
   * }}}
   */
  def apply[I] = new To[I] {}

  /**
   * "Serialize" type `O` to type `I` using the implicit `Write` w
   * {{{
   *   To[Person2, UrlFormEncoded](Person(List("bob", "bobby"))) ==
   *      Map(
   *      "name" -> Seq("bob"),
   *      "friend.name" -> Seq("bobby"))
   * }}}
   */
  def apply[O, I](o: O)(implicit w: WriteLike[O, I]) =
    w.writes(o)
}

case class Reader[I](path: Path = Path(Nil)) {
  /**
   * When applied, the rule will lookup for data at the given path, and apply the `sub` Rule on it
   * {{{
   *   val json = Json.parse("""{
   *      "informations": {
   *        "label": "test"
   *      }
   *   }""")
   *   val infoValidation = From[JsValue]{ __ => (__ \ "label").read(nonEmptyText) }
   *   val v = From[JsValue]{ __ => (__ \ "informations").read(infoValidation)) }
   *   v.validate(json) == Success("test")
   * }}}
   * @param sub the constraint to apply on the subdata
   * @param l a lookup function. This function finds data in a structure of type I, and coerce it to type O
   * @return A Rule validating the existence and validity of data at `path`
   */
  def read[J, O](sub: => RuleLike[J, O])(implicit r: Path => RuleLike[I, J]): Rule[I, O] =
    Rule.toRule(r(path)).compose(path)(sub)

  /**
   * Try to convert the data at `Path` to type `O`
   * {{{
   *   val json = Json.parse("""{
   *      "informations": {
   *        "label": "test"
   *      }
   *   }""")
   *   implicit val infoValidation = From[JsValue]{ __ => (__ \ "label").read[String] }
   *   val v = From[JsValue]{ __ => (__ \ "informations").read[Informations]) }
   *   v.validate(json) == Success("test")
   * }}}
   * @param r a lookup function. This function finds data in a structure of type I, and coerce it to type O
   * @return A Rule validating the existence and validity of data at `path`.
   */
  def read[O](implicit r: Path => RuleLike[I, O]): Rule[I, O] =
    Rule { i => read(Rule.zero[O])(r).validate(i) } // makes it lazy evaluated. Allows recursive writes

  def \(key: String): Reader[I] = Reader(path \ key)
  def \(idx: Int): Reader[I] = Reader(path \ idx)
  def \(child: PathNode): Reader[I] = Reader(path \ child)
}

case class Writer[I](path: Path = Path(Nil)) {
  /**
   * Create a Write that convert data to type `I`, and put it at Path `path`
   * {{{
   *   val w = To[JsObject] { __ =>
   *      (__ \ "informations").write[Seq[String]])
   *   }
   *   w.writes(Seq("foo", "bar")) == Json.obj("informations" -> Seq("foo", "bar"))
   * }}}
   * @note This method works fine with recursive writes
   */
  def write[O](implicit w: Path => WriteLike[O, I]): Write[O, I] =
    Write { x => w(path).writes(x) } // makes it lazy evaluated. Allows recursive writes

  /**
   * Create a Write that convert data to type `I`, and put it at Path `path`
   * {{{
   *   val w = To[JsObject] { __ =>
   *      (__ \ "date").write(date("yyyy-MM-dd""))
   *   }
   *   w.writes(new Date()) == Json.obj("date" -> "2013-10-3")
   * }}}
   * @note This method works fine with recursive writes
   */
  def write[O, J](format: => WriteLike[O, J])(implicit w: Path => WriteLike[J, I]): Write[O, I] =
    Write.toWrite(w(path)).contramap(x => format.writes(x))

  def \(key: String): Writer[I] = Writer(path \ key)
  def \(idx: Int): Writer[I] = Writer(path \ idx)
  def \(child: PathNode): Writer[I] = Writer(path \ child)
}

trait Formatting[IR, IW] {
  def apply[O](f: Formatter[IR, IW] => Format[IR, IW, O]) = f(Formatter[IR, IW]())
}
object Formatting {
  def apply[IR, IW] = new Formatting[IR, IW] {}
}

case class Formatter[IR, IW](path: Path = Path(Nil)) {

  def format[JJ, J, O](subR: => RuleLike[J, O], subW: => WriteLike[O, JJ])(implicit r: Path => RuleLike[IR, J], w: Path => WriteLike[JJ, IW]): Format[IR, IW, O] = {
    Format[IR, IW, O](Reader(path).read(subR), Writer(path).write(subW))
  }

  def format[J, O](subR: => RuleLike[J, O])(implicit r: Path => RuleLike[IR, J], w: Path => WriteLike[O, IW]): Format[IR, IW, O] =
    format(subR, Write.zero[O])

  // def format[JJ, O](subW: => WriteLike[O, JJ])(implicit r: Path => RuleLike[I, O], w: Path => WriteLike[JJ, I]): Format[I, O] =
  //   format(Rule.zero[O], subW)

  def format[O](implicit r: Path => RuleLike[IR, O], w: Path => WriteLike[O, IW]): Format[IR, IW, O] = new Format[IR, IW, O] {
    lazy val f = format(Rule.zero[O], Write.zero[O])
    def validate(i: IR) = f.validate(i)
    def writes(o: O) = f.writes(o)
  }

  def \(key: String): Formatter[IR, IW] = Formatter(path \ key)
  def \(idx: Int): Formatter[IR, IW] = Formatter(path \ idx)
  def \(child: PathNode): Formatter[IR, IW] = Formatter(path \ child)
}