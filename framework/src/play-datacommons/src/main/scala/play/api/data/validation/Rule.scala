package play.api.data.validation

case class Rule[I, O](p: Path[I], m: Path[I] => Mapping[(Path[I], Seq[ValidationError]), I, O], v: Constraint[O] = Constraints.noConstraint[O]) {
  def validate(data: I): VA[I, O] =
    m(p)(data).flatMap(v(_).fail.map{ errs => Seq(p -> errs) })
}