package play.api.data

import format._
import validation._

case class ObjectMapping2[R <: Product, A1, A2](apply: Function2[A1, A2, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])

    (a1._1 ++ a2._1) ->
      (a1._2 ++ a2._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings

}

case class ObjectMapping3[R <: Product, A1, A2, A3](apply: Function3[A1, A2, A3, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])

    (a1._1 ++ a2._1 ++ a3._1) ->
      (a1._2 ++ a2._2 ++ a3._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings

}

case class ObjectMapping4[R <: Product, A1, A2, A3, A4](apply: Function4[A1, A2, A3, A4, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings

}

case class ObjectMapping5[R <: Product, A1, A2, A3, A4, A5](apply: Function5[A1, A2, A3, A4, A5, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings

}

case class ObjectMapping6[R <: Product, A1, A2, A3, A4, A5, A6](apply: Function6[A1, A2, A3, A4, A5, A6, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings

}

case class ObjectMapping7[R <: Product, A1, A2, A3, A4, A5, A6, A7](apply: Function7[A1, A2, A3, A4, A5, A6, A7, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings

}

case class ObjectMapping8[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8](apply: Function8[A1, A2, A3, A4, A5, A6, A7, A8, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings

}

case class ObjectMapping9[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9](apply: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings

}

case class ObjectMapping10[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](apply: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings

}

case class ObjectMapping11[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](apply: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings

}

case class ObjectMapping12[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](apply: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings

}

case class ObjectMapping13[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](apply: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  val field13 = f13._2.withPrefix(f13._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12],
          values(13).asInstanceOf[A13]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])
    val a13 = field13.unbind(value.productElement(12).asInstanceOf[A13])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings

}

case class ObjectMapping14[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](apply: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  val field13 = f13._2.withPrefix(f13._1).withPrefix(key)

  val field14 = f14._2.withPrefix(f14._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12],
          values(13).asInstanceOf[A13],
          values(14).asInstanceOf[A14]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])
    val a13 = field13.unbind(value.productElement(12).asInstanceOf[A13])
    val a14 = field14.unbind(value.productElement(13).asInstanceOf[A14])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings

}

case class ObjectMapping15[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](apply: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  val field13 = f13._2.withPrefix(f13._1).withPrefix(key)

  val field14 = f14._2.withPrefix(f14._1).withPrefix(key)

  val field15 = f15._2.withPrefix(f15._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12],
          values(13).asInstanceOf[A13],
          values(14).asInstanceOf[A14],
          values(15).asInstanceOf[A15]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])
    val a13 = field13.unbind(value.productElement(12).asInstanceOf[A13])
    val a14 = field14.unbind(value.productElement(13).asInstanceOf[A14])
    val a15 = field15.unbind(value.productElement(14).asInstanceOf[A15])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings

}

case class ObjectMapping16[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](apply: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  val field13 = f13._2.withPrefix(f13._1).withPrefix(key)

  val field14 = f14._2.withPrefix(f14._1).withPrefix(key)

  val field15 = f15._2.withPrefix(f15._1).withPrefix(key)

  val field16 = f16._2.withPrefix(f16._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12],
          values(13).asInstanceOf[A13],
          values(14).asInstanceOf[A14],
          values(15).asInstanceOf[A15],
          values(16).asInstanceOf[A16]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])
    val a13 = field13.unbind(value.productElement(12).asInstanceOf[A13])
    val a14 = field14.unbind(value.productElement(13).asInstanceOf[A14])
    val a15 = field15.unbind(value.productElement(14).asInstanceOf[A15])
    val a16 = field16.unbind(value.productElement(15).asInstanceOf[A16])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings

}

case class ObjectMapping17[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](apply: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  val field13 = f13._2.withPrefix(f13._1).withPrefix(key)

  val field14 = f14._2.withPrefix(f14._1).withPrefix(key)

  val field15 = f15._2.withPrefix(f15._1).withPrefix(key)

  val field16 = f16._2.withPrefix(f16._1).withPrefix(key)

  val field17 = f17._2.withPrefix(f17._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12],
          values(13).asInstanceOf[A13],
          values(14).asInstanceOf[A14],
          values(15).asInstanceOf[A15],
          values(16).asInstanceOf[A16],
          values(17).asInstanceOf[A17]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])
    val a13 = field13.unbind(value.productElement(12).asInstanceOf[A13])
    val a14 = field14.unbind(value.productElement(13).asInstanceOf[A14])
    val a15 = field15.unbind(value.productElement(14).asInstanceOf[A15])
    val a16 = field16.unbind(value.productElement(15).asInstanceOf[A16])
    val a17 = field17.unbind(value.productElement(16).asInstanceOf[A17])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings

}

case class ObjectMapping18[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](apply: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  val field13 = f13._2.withPrefix(f13._1).withPrefix(key)

  val field14 = f14._2.withPrefix(f14._1).withPrefix(key)

  val field15 = f15._2.withPrefix(f15._1).withPrefix(key)

  val field16 = f16._2.withPrefix(f16._1).withPrefix(key)

  val field17 = f17._2.withPrefix(f17._1).withPrefix(key)

  val field18 = f18._2.withPrefix(f18._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data), field18.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12],
          values(13).asInstanceOf[A13],
          values(14).asInstanceOf[A14],
          values(15).asInstanceOf[A15],
          values(16).asInstanceOf[A16],
          values(17).asInstanceOf[A17],
          values(18).asInstanceOf[A18]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])
    val a13 = field13.unbind(value.productElement(12).asInstanceOf[A13])
    val a14 = field14.unbind(value.productElement(13).asInstanceOf[A14])
    val a15 = field15.unbind(value.productElement(14).asInstanceOf[A15])
    val a16 = field16.unbind(value.productElement(15).asInstanceOf[A16])
    val a17 = field17.unbind(value.productElement(16).asInstanceOf[A17])
    val a18 = field18.unbind(value.productElement(17).asInstanceOf[A18])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2)

  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings

}

case class ObjectMapping19[R <: Product, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](apply: Function19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), f19: (String, Mapping[A19]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  val field10 = f10._2.withPrefix(f10._1).withPrefix(key)

  val field11 = f11._2.withPrefix(f11._1).withPrefix(key)

  val field12 = f12._2.withPrefix(f12._1).withPrefix(key)

  val field13 = f13._2.withPrefix(f13._1).withPrefix(key)

  val field14 = f14._2.withPrefix(f14._1).withPrefix(key)

  val field15 = f15._2.withPrefix(f15._1).withPrefix(key)

  val field16 = f16._2.withPrefix(f16._1).withPrefix(key)

  val field17 = f17._2.withPrefix(f17._1).withPrefix(key)

  val field18 = f18._2.withPrefix(f18._1).withPrefix(key)

  val field19 = f19._2.withPrefix(f19._1).withPrefix(key)

  def bind(data: Map[String, String]) = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data), field18.bind(data), field19.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(1).asInstanceOf[A1],
          values(2).asInstanceOf[A2],
          values(3).asInstanceOf[A3],
          values(4).asInstanceOf[A4],
          values(5).asInstanceOf[A5],
          values(6).asInstanceOf[A6],
          values(7).asInstanceOf[A7],
          values(8).asInstanceOf[A8],
          values(9).asInstanceOf[A9],
          values(10).asInstanceOf[A10],
          values(11).asInstanceOf[A11],
          values(12).asInstanceOf[A12],
          values(13).asInstanceOf[A13],
          values(14).asInstanceOf[A14],
          values(15).asInstanceOf[A15],
          values(16).asInstanceOf[A16],
          values(17).asInstanceOf[A17],
          values(18).asInstanceOf[A18],
          values(19).asInstanceOf[A19]))
      }
    }
  }

  def unbind(value: R) = {
    val a1 = field1.unbind(value.productElement(0).asInstanceOf[A1])
    val a2 = field2.unbind(value.productElement(1).asInstanceOf[A2])
    val a3 = field3.unbind(value.productElement(2).asInstanceOf[A3])
    val a4 = field4.unbind(value.productElement(3).asInstanceOf[A4])
    val a5 = field5.unbind(value.productElement(4).asInstanceOf[A5])
    val a6 = field6.unbind(value.productElement(5).asInstanceOf[A6])
    val a7 = field7.unbind(value.productElement(6).asInstanceOf[A7])
    val a8 = field8.unbind(value.productElement(7).asInstanceOf[A8])
    val a9 = field9.unbind(value.productElement(8).asInstanceOf[A9])
    val a10 = field10.unbind(value.productElement(9).asInstanceOf[A10])
    val a11 = field11.unbind(value.productElement(10).asInstanceOf[A11])
    val a12 = field12.unbind(value.productElement(11).asInstanceOf[A12])
    val a13 = field13.unbind(value.productElement(12).asInstanceOf[A13])
    val a14 = field14.unbind(value.productElement(13).asInstanceOf[A14])
    val a15 = field15.unbind(value.productElement(14).asInstanceOf[A15])
    val a16 = field16.unbind(value.productElement(15).asInstanceOf[A16])
    val a17 = field17.unbind(value.productElement(16).asInstanceOf[A17])
    val a18 = field18.unbind(value.productElement(17).asInstanceOf[A18])
    val a19 = field19.unbind(value.productElement(18).asInstanceOf[A19])

    (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1 ++ a19._1) ->
      (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2 ++ a19._2)
  }

  def withPrefix(prefix: String) = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*) = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings ++ field19.mappings

}
