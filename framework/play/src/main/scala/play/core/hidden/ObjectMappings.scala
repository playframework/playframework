package play.api.data

import format._
import validation._

case class ObjectMapping2[T <: Product, A, B](apply: Function2[A, B, T], fa: (String, Mapping[A]), fb: (String, Mapping[B]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)
  val fieldB = fb._2.withPrefix(fb._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(fieldA.bind(data), fieldB.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A],
          values(1).asInstanceOf[B]))
      }
    }
  }

  def unbind(value: T) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    val b = fieldB.unbind(value.productElement(1).asInstanceOf[B])
    (a._1 ++ b._1) -> (a._2 ++ b._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ fieldA.mappings ++ fieldB.mappings

}

case class ObjectMapping3[T <: Product, A, B, C](apply: Function3[A, B, C, T], fa: (String, Mapping[A]), fb: (String, Mapping[B]), fc: (String, Mapping[C]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)
  val fieldB = fb._2.withPrefix(fb._1).withPrefix(key)
  val fieldC = fc._2.withPrefix(fc._1).withPrefix(key)

  def bind(data: Map[String, String]) = {

    merge(fieldA.bind(data), fieldB.bind(data), fieldC.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A],
          values(1).asInstanceOf[B],
          values(2).asInstanceOf[C]))
      }
    }

  }

  def unbind(value: T) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    val b = fieldB.unbind(value.productElement(1).asInstanceOf[B])
    val c = fieldC.unbind(value.productElement(2).asInstanceOf[C])
    (a._1 ++ b._1 ++ c._1) -> (a._2 ++ b._2 ++ c._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ fieldA.mappings ++ fieldB.mappings ++ fieldC.mappings

}

case class ObjectMapping4[T <: Product, A, B, C, D](apply: Function4[A, B, C, D, T], fa: (String, Mapping[A]), fb: (String, Mapping[B]), fc: (String, Mapping[C]), fd: (String, Mapping[D]), val key: String = "", val constraints: Seq[Constraint[T]] = Nil) extends Mapping[T] with ObjectMapping {

  val fieldA = fa._2.withPrefix(fa._1).withPrefix(key)
  val fieldB = fb._2.withPrefix(fb._1).withPrefix(key)
  val fieldC = fc._2.withPrefix(fc._1).withPrefix(key)
  val fieldD = fd._2.withPrefix(fd._1).withPrefix(key)

  def bind(data: Map[String, String]) = {

    merge(fieldA.bind(data), fieldB.bind(data), fieldC.bind(data), fieldD.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A],
          values(1).asInstanceOf[B],
          values(2).asInstanceOf[C],
          values(3).asInstanceOf[D]))
      }
    }

  }

  def unbind(value: T) = {
    val a = fieldA.unbind(value.productElement(0).asInstanceOf[A])
    val b = fieldB.unbind(value.productElement(1).asInstanceOf[B])
    val c = fieldC.unbind(value.productElement(2).asInstanceOf[C])
    val d = fieldD.unbind(value.productElement(3).asInstanceOf[D])
    (a._1 ++ b._1 ++ c._1 ++ d._1) -> (a._2 ++ b._2 ++ c._2 ++ d._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[T]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ fieldA.mappings ++ fieldB.mappings ++ fieldC.mappings ++ fieldD.mappings

}

