package play.api.data

import format._
import validation._

case class ObjectMapping2[R, A1, A2](apply: Function2[A1, A2, R], unapply: Function1[R, Option[(A1, A2)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)

      (a1._1 ++ a2._1) ->
        (a1._2 ++ a2._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping2[R, A1, A2] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping2[R, A1, A2] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings

}

case class ObjectMapping3[R, A1, A2, A3](apply: Function3[A1, A2, A3, R], unapply: Function1[R, Option[(A1, A2, A3)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)

      (a1._1 ++ a2._1 ++ a3._1) ->
        (a1._2 ++ a2._2 ++ a3._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping3[R, A1, A2, A3] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping3[R, A1, A2, A3] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings

}

case class ObjectMapping4[R, A1, A2, A3, A4](apply: Function4[A1, A2, A3, A4, R], unapply: Function1[R, Option[(A1, A2, A3, A4)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping4[R, A1, A2, A3, A4] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping4[R, A1, A2, A3, A4] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings

}

case class ObjectMapping5[R, A1, A2, A3, A4, A5](apply: Function5[A1, A2, A3, A4, A5, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping5[R, A1, A2, A3, A4, A5] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping5[R, A1, A2, A3, A4, A5] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings

}

case class ObjectMapping6[R, A1, A2, A3, A4, A5, A6](apply: Function6[A1, A2, A3, A4, A5, A6, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping6[R, A1, A2, A3, A4, A5, A6] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping6[R, A1, A2, A3, A4, A5, A6] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings

}

case class ObjectMapping7[R, A1, A2, A3, A4, A5, A6, A7](apply: Function7[A1, A2, A3, A4, A5, A6, A7, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping7[R, A1, A2, A3, A4, A5, A6, A7] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping7[R, A1, A2, A3, A4, A5, A6, A7] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings

}

case class ObjectMapping8[R, A1, A2, A3, A4, A5, A6, A7, A8](apply: Function8[A1, A2, A3, A4, A5, A6, A7, A8, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping8[R, A1, A2, A3, A4, A5, A6, A7, A8] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping8[R, A1, A2, A3, A4, A5, A6, A7, A8] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings

}

case class ObjectMapping9[R, A1, A2, A3, A4, A5, A6, A7, A8, A9](apply: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  val field3 = f3._2.withPrefix(f3._1).withPrefix(key)

  val field4 = f4._2.withPrefix(f4._1).withPrefix(key)

  val field5 = f5._2.withPrefix(f5._1).withPrefix(key)

  val field6 = f6._2.withPrefix(f6._1).withPrefix(key)

  val field7 = f7._2.withPrefix(f7._1).withPrefix(key)

  val field8 = f8._2.withPrefix(f8._1).withPrefix(key)

  val field9 = f9._2.withPrefix(f9._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping9[R, A1, A2, A3, A4, A5, A6, A7, A8, A9] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping9[R, A1, A2, A3, A4, A5, A6, A7, A8, A9] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings

}

case class ObjectMapping10[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](apply: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping10[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping10[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings

}

case class ObjectMapping11[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](apply: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping11[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping11[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings

}

case class ObjectMapping12[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](apply: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11],
          values(11).asInstanceOf[A12]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)
      val a12 = field12.unbind(v12)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping12[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping12[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings

}

case class ObjectMapping13[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](apply: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11],
          values(11).asInstanceOf[A12],
          values(12).asInstanceOf[A13]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)
      val a12 = field12.unbind(v12)
      val a13 = field13.unbind(v13)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping13[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping13[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings

}

case class ObjectMapping14[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](apply: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11],
          values(11).asInstanceOf[A12],
          values(12).asInstanceOf[A13],
          values(13).asInstanceOf[A14]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)
      val a12 = field12.unbind(v12)
      val a13 = field13.unbind(v13)
      val a14 = field14.unbind(v14)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping14[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping14[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings

}

case class ObjectMapping15[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](apply: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11],
          values(11).asInstanceOf[A12],
          values(12).asInstanceOf[A13],
          values(13).asInstanceOf[A14],
          values(14).asInstanceOf[A15]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)
      val a12 = field12.unbind(v12)
      val a13 = field13.unbind(v13)
      val a14 = field14.unbind(v14)
      val a15 = field15.unbind(v15)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping15[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping15[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings

}

case class ObjectMapping16[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](apply: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11],
          values(11).asInstanceOf[A12],
          values(12).asInstanceOf[A13],
          values(13).asInstanceOf[A14],
          values(14).asInstanceOf[A15],
          values(15).asInstanceOf[A16]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)
      val a12 = field12.unbind(v12)
      val a13 = field13.unbind(v13)
      val a14 = field14.unbind(v14)
      val a15 = field15.unbind(v15)
      val a16 = field16.unbind(v16)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping16[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping16[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings

}

case class ObjectMapping17[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](apply: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11],
          values(11).asInstanceOf[A12],
          values(12).asInstanceOf[A13],
          values(13).asInstanceOf[A14],
          values(14).asInstanceOf[A15],
          values(15).asInstanceOf[A16],
          values(16).asInstanceOf[A17]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)
      val a12 = field12.unbind(v12)
      val a13 = field13.unbind(v13)
      val a14 = field14.unbind(v14)
      val a15 = field15.unbind(v15)
      val a16 = field16.unbind(v16)
      val a17 = field17.unbind(v17)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping17[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping17[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings

}

case class ObjectMapping18[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](apply: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data), field18.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(

          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2],
          values(2).asInstanceOf[A3],
          values(3).asInstanceOf[A4],
          values(4).asInstanceOf[A5],
          values(5).asInstanceOf[A6],
          values(6).asInstanceOf[A7],
          values(7).asInstanceOf[A8],
          values(8).asInstanceOf[A9],
          values(9).asInstanceOf[A10],
          values(10).asInstanceOf[A11],
          values(11).asInstanceOf[A12],
          values(12).asInstanceOf[A13],
          values(13).asInstanceOf[A14],
          values(14).asInstanceOf[A15],
          values(15).asInstanceOf[A16],
          values(16).asInstanceOf[A17],
          values(17).asInstanceOf[A18]))
      }
    }
  }

  def unbind(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) = fields
      val a1 = field1.unbind(v1)
      val a2 = field2.unbind(v2)
      val a3 = field3.unbind(v3)
      val a4 = field4.unbind(v4)
      val a5 = field5.unbind(v5)
      val a6 = field6.unbind(v6)
      val a7 = field7.unbind(v7)
      val a8 = field8.unbind(v8)
      val a9 = field9.unbind(v9)
      val a10 = field10.unbind(v10)
      val a11 = field11.unbind(v11)
      val a12 = field12.unbind(v12)
      val a13 = field13.unbind(v13)
      val a14 = field14.unbind(v14)
      val a15 = field15.unbind(v15)
      val a16 = field16.unbind(v16)
      val a17 = field17.unbind(v17)
      val a18 = field18.unbind(v18)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2)
    }.getOrElse(Map.empty -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping18[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18] = addPrefix(prefix).map(newKey => this.copy(key = newKey)).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping18[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18] = {
    this.copy(constraints = constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings

}

