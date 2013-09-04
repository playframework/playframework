package play.api.data.mapping

trait Format[I, O] extends Rule[I, O] with Write[O, I]
object Format {
  def apply[I, O](r: Rule[I, O], w: Write[O, I]) = new Format[I, O] {
    def writes(o: O): I = w.writes(o)
    def validate(data: I): VA[I, O] = r.validate(data)
  }
}