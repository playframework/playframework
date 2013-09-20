package play.api.data.mapping

trait From[I] {
	def apply[O](f: Formatter[I] => Rule[I, O]) = f(Formatter[I]())
}
object From { def apply[I] = new From[I]{} }

trait To[I] {
	def apply[O](f: Formatter[I] => Write[O, I]) = f(Formatter[I]())
}
object To { def apply[I] = new To[I]{} }


case class Formatter[I](path: Path = Path(Nil)) {
  /**
  * When applied, the rule will lookup for data at the given path, and apply the given Rule on it
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
  def read[J, O](sub: Rule[J, O])(implicit r: Path => Rule[I, J]): Rule[I, O] =
    r(path).compose(path)(sub)


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
  def read[O](implicit r: Path => Rule[I, O]): Rule[I, O] =
    read(Rule.zero[O])(r)

  def write[O](implicit w: Path => Write[O, I]): Write[O, I] = w(path)
  def write[O, J](format: Write[O, J])(implicit w: Path => Write[J, I]): Write[O, I] =
    Write((w(path).writes _) compose (format.writes _))

  def \(key: String): Formatter[I] = Formatter(path \ key)
  def \(idx: Int): Formatter[I] = Formatter(path \ idx)
  def \(child: PathNode): Formatter[I] = Formatter(path \ child)

}