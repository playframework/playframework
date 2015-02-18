/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data

import format._
import validation._

/*
  DO NOT EDIT THE SCALA CODE IN THIS FILE, IT IS GENERATED.

  The script below will generate this file.  Edit and run the script to edit the file.

#!/bin/sh
exec scala -savecompiled "$0" $0 $@
!#

def generate(times: Int) = {

    def g(format: String, sep: String) = (for (i <- 1 to times) yield format.replaceAll("%", i.toString).replaceAll("#", (i - 1).toString)).mkString(sep)

    def aParams = g("A%", ", ")

s"""
class ObjectMapping$times[R, $aParams](apply: Function$times[$aParams, R], unapply: Function1[R, Option[($aParams)]], ${g("f%: (String, Mapping[A%])", ", ")}, val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  ${g("val field% = f%._2.withPrefix(f%._1).withPrefix(key)", "\n\n  ")}

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(${g("field%.bind(data)", ", ")}) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          ${g("values(#).asInstanceOf[A%]", ",\n          ")}
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (${g("v%", ", ")}) = fields
      ${g("field%.unbind(v%)", " ++ ")}
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (${g("v%", ", ")}) = fields
      ${g("val a% = field%.unbindAndValidate(v%)", "\n      ")}

      (${g("a%._1", " ++ ")}) ->
        (${g("a%._2", " ++ ")})
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping$times[R, $aParams] = addPrefix(prefix).map(newKey => 
    new ObjectMapping$times(apply, unapply, ${g("f%", ", ")}, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping$times[R, $aParams] = {
    new ObjectMapping$times(apply, unapply, ${g("f%", ", ")}, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ ${g("field%.mappings", " ++ ")}

}
"""
}

val scriptSource = scala.io.Source.fromFile(argv(0)).getLines.mkString("\n")

println(s"""/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data

import format._
import validation._

/*
  DO NOT EDIT THE SCALA CODE IN THIS FILE, IT IS GENERATED.

  The script below will generate this file.  Edit and run the script to edit the file.

$scriptSource
 */
""")

println((for (i <- 1 to 22) yield generate(i)).mkString(""))
 */

class ObjectMapping1[R, A1](apply: Function1[A1, R], unapply: Function1[R, Option[(A1)]], f1: (String, Mapping[A1]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A1]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1) = fields
      field1.unbind(v1)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1) = fields
      val a1 = field1.unbindAndValidate(v1)

      (a1._1) ->
        (a1._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping1[R, A1] = addPrefix(prefix).map(newKey =>
    new ObjectMapping1(apply, unapply, f1, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping1[R, A1] = {
    new ObjectMapping1(apply, unapply, f1, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings

}

class ObjectMapping2[R, A1, A2](apply: Function2[A1, A2, R], unapply: Function1[R, Option[(A1, A2)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

  val field1 = f1._2.withPrefix(f1._1).withPrefix(key)

  val field2 = f2._2.withPrefix(f2._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data)) match {
      case Left(errors) => Left(errors)
      case Right(values) => {
        applyConstraints(apply(
          values(0).asInstanceOf[A1],
          values(1).asInstanceOf[A2]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2) = fields
      field1.unbind(v1) ++ field2.unbind(v2)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)

      (a1._1 ++ a2._1) ->
        (a1._2 ++ a2._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping2[R, A1, A2] = addPrefix(prefix).map(newKey =>
    new ObjectMapping2(apply, unapply, f1, f2, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping2[R, A1, A2] = {
    new ObjectMapping2(apply, unapply, f1, f2, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings

}

class ObjectMapping3[R, A1, A2, A3](apply: Function3[A1, A2, A3, R], unapply: Function1[R, Option[(A1, A2, A3)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(2).asInstanceOf[A3]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)

      (a1._1 ++ a2._1 ++ a3._1) ->
        (a1._2 ++ a2._2 ++ a3._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping3[R, A1, A2, A3] = addPrefix(prefix).map(newKey =>
    new ObjectMapping3(apply, unapply, f1, f2, f3, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping3[R, A1, A2, A3] = {
    new ObjectMapping3(apply, unapply, f1, f2, f3, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings

}

class ObjectMapping4[R, A1, A2, A3, A4](apply: Function4[A1, A2, A3, A4, R], unapply: Function1[R, Option[(A1, A2, A3, A4)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(3).asInstanceOf[A4]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping4[R, A1, A2, A3, A4] = addPrefix(prefix).map(newKey =>
    new ObjectMapping4(apply, unapply, f1, f2, f3, f4, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping4[R, A1, A2, A3, A4] = {
    new ObjectMapping4(apply, unapply, f1, f2, f3, f4, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings

}

class ObjectMapping5[R, A1, A2, A3, A4, A5](apply: Function5[A1, A2, A3, A4, A5, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(4).asInstanceOf[A5]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping5[R, A1, A2, A3, A4, A5] = addPrefix(prefix).map(newKey =>
    new ObjectMapping5(apply, unapply, f1, f2, f3, f4, f5, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping5[R, A1, A2, A3, A4, A5] = {
    new ObjectMapping5(apply, unapply, f1, f2, f3, f4, f5, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings

}

class ObjectMapping6[R, A1, A2, A3, A4, A5, A6](apply: Function6[A1, A2, A3, A4, A5, A6, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(5).asInstanceOf[A6]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping6[R, A1, A2, A3, A4, A5, A6] = addPrefix(prefix).map(newKey =>
    new ObjectMapping6(apply, unapply, f1, f2, f3, f4, f5, f6, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping6[R, A1, A2, A3, A4, A5, A6] = {
    new ObjectMapping6(apply, unapply, f1, f2, f3, f4, f5, f6, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings

}

class ObjectMapping7[R, A1, A2, A3, A4, A5, A6, A7](apply: Function7[A1, A2, A3, A4, A5, A6, A7, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(6).asInstanceOf[A7]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping7[R, A1, A2, A3, A4, A5, A6, A7] = addPrefix(prefix).map(newKey =>
    new ObjectMapping7(apply, unapply, f1, f2, f3, f4, f5, f6, f7, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping7[R, A1, A2, A3, A4, A5, A6, A7] = {
    new ObjectMapping7(apply, unapply, f1, f2, f3, f4, f5, f6, f7, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings

}

class ObjectMapping8[R, A1, A2, A3, A4, A5, A6, A7, A8](apply: Function8[A1, A2, A3, A4, A5, A6, A7, A8, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(7).asInstanceOf[A8]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping8[R, A1, A2, A3, A4, A5, A6, A7, A8] = addPrefix(prefix).map(newKey =>
    new ObjectMapping8(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping8[R, A1, A2, A3, A4, A5, A6, A7, A8] = {
    new ObjectMapping8(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings

}

class ObjectMapping9[R, A1, A2, A3, A4, A5, A6, A7, A8, A9](apply: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(8).asInstanceOf[A9]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping9[R, A1, A2, A3, A4, A5, A6, A7, A8, A9] = addPrefix(prefix).map(newKey =>
    new ObjectMapping9(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping9[R, A1, A2, A3, A4, A5, A6, A7, A8, A9] = {
    new ObjectMapping9(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings

}

class ObjectMapping10[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](apply: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(9).asInstanceOf[A10]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping10[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10] = addPrefix(prefix).map(newKey =>
    new ObjectMapping10(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping10[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10] = {
    new ObjectMapping10(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings

}

class ObjectMapping11[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](apply: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(10).asInstanceOf[A11]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping11[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11] = addPrefix(prefix).map(newKey =>
    new ObjectMapping11(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping11[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11] = {
    new ObjectMapping11(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings

}

class ObjectMapping12[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](apply: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(11).asInstanceOf[A12]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping12[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12] = addPrefix(prefix).map(newKey =>
    new ObjectMapping12(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping12[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12] = {
    new ObjectMapping12(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings

}

class ObjectMapping13[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](apply: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(12).asInstanceOf[A13]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping13[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13] = addPrefix(prefix).map(newKey =>
    new ObjectMapping13(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping13[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13] = {
    new ObjectMapping13(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings

}

class ObjectMapping14[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](apply: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(13).asInstanceOf[A14]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping14[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14] = addPrefix(prefix).map(newKey =>
    new ObjectMapping14(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping14[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14] = {
    new ObjectMapping14(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings

}

class ObjectMapping15[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](apply: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(14).asInstanceOf[A15]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping15[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15] = addPrefix(prefix).map(newKey =>
    new ObjectMapping15(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping15[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15] = {
    new ObjectMapping15(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings

}

class ObjectMapping16[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](apply: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(15).asInstanceOf[A16]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15) ++ field16.unbind(v16)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)
      val a16 = field16.unbindAndValidate(v16)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping16[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16] = addPrefix(prefix).map(newKey =>
    new ObjectMapping16(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping16[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16] = {
    new ObjectMapping16(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings

}

class ObjectMapping17[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](apply: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(16).asInstanceOf[A17]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15) ++ field16.unbind(v16) ++ field17.unbind(v17)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)
      val a16 = field16.unbindAndValidate(v16)
      val a17 = field17.unbindAndValidate(v17)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping17[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17] = addPrefix(prefix).map(newKey =>
    new ObjectMapping17(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping17[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17] = {
    new ObjectMapping17(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings

}

class ObjectMapping18[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](apply: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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
          values(17).asInstanceOf[A18]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15) ++ field16.unbind(v16) ++ field17.unbind(v17) ++ field18.unbind(v18)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)
      val a16 = field16.unbindAndValidate(v16)
      val a17 = field17.unbindAndValidate(v17)
      val a18 = field18.unbindAndValidate(v18)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping18[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18] = addPrefix(prefix).map(newKey =>
    new ObjectMapping18(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping18[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18] = {
    new ObjectMapping18(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings

}

class ObjectMapping19[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](apply: Function19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), f19: (String, Mapping[A19]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data), field18.bind(data), field19.bind(data)) match {
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
          values(17).asInstanceOf[A18],
          values(18).asInstanceOf[A19]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15) ++ field16.unbind(v16) ++ field17.unbind(v17) ++ field18.unbind(v18) ++ field19.unbind(v19)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)
      val a16 = field16.unbindAndValidate(v16)
      val a17 = field17.unbindAndValidate(v17)
      val a18 = field18.unbindAndValidate(v18)
      val a19 = field19.unbindAndValidate(v19)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1 ++ a19._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2 ++ a19._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping19[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19] = addPrefix(prefix).map(newKey =>
    new ObjectMapping19(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping19[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19] = {
    new ObjectMapping19(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings ++ field19.mappings

}

class ObjectMapping20[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](apply: Function20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), f19: (String, Mapping[A19]), f20: (String, Mapping[A20]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  val field20 = f20._2.withPrefix(f20._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data), field18.bind(data), field19.bind(data), field20.bind(data)) match {
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
          values(17).asInstanceOf[A18],
          values(18).asInstanceOf[A19],
          values(19).asInstanceOf[A20]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15) ++ field16.unbind(v16) ++ field17.unbind(v17) ++ field18.unbind(v18) ++ field19.unbind(v19) ++ field20.unbind(v20)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)
      val a16 = field16.unbindAndValidate(v16)
      val a17 = field17.unbindAndValidate(v17)
      val a18 = field18.unbindAndValidate(v18)
      val a19 = field19.unbindAndValidate(v19)
      val a20 = field20.unbindAndValidate(v20)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1 ++ a19._1 ++ a20._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2 ++ a19._2 ++ a20._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping20[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20] = addPrefix(prefix).map(newKey =>
    new ObjectMapping20(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping20[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20] = {
    new ObjectMapping20(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings ++ field19.mappings ++ field20.mappings

}

class ObjectMapping21[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](apply: Function21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), f19: (String, Mapping[A19]), f20: (String, Mapping[A20]), f21: (String, Mapping[A21]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  val field20 = f20._2.withPrefix(f20._1).withPrefix(key)

  val field21 = f21._2.withPrefix(f21._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data), field18.bind(data), field19.bind(data), field20.bind(data), field21.bind(data)) match {
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
          values(17).asInstanceOf[A18],
          values(18).asInstanceOf[A19],
          values(19).asInstanceOf[A20],
          values(20).asInstanceOf[A21]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15) ++ field16.unbind(v16) ++ field17.unbind(v17) ++ field18.unbind(v18) ++ field19.unbind(v19) ++ field20.unbind(v20) ++ field21.unbind(v21)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)
      val a16 = field16.unbindAndValidate(v16)
      val a17 = field17.unbindAndValidate(v17)
      val a18 = field18.unbindAndValidate(v18)
      val a19 = field19.unbindAndValidate(v19)
      val a20 = field20.unbindAndValidate(v20)
      val a21 = field21.unbindAndValidate(v21)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1 ++ a19._1 ++ a20._1 ++ a21._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2 ++ a19._2 ++ a20._2 ++ a21._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping21[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21] = addPrefix(prefix).map(newKey =>
    new ObjectMapping21(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping21[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21] = {
    new ObjectMapping21(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings ++ field19.mappings ++ field20.mappings ++ field21.mappings

}

class ObjectMapping22[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22](apply: Function22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R], unapply: Function1[R, Option[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)]], f1: (String, Mapping[A1]), f2: (String, Mapping[A2]), f3: (String, Mapping[A3]), f4: (String, Mapping[A4]), f5: (String, Mapping[A5]), f6: (String, Mapping[A6]), f7: (String, Mapping[A7]), f8: (String, Mapping[A8]), f9: (String, Mapping[A9]), f10: (String, Mapping[A10]), f11: (String, Mapping[A11]), f12: (String, Mapping[A12]), f13: (String, Mapping[A13]), f14: (String, Mapping[A14]), f15: (String, Mapping[A15]), f16: (String, Mapping[A16]), f17: (String, Mapping[A17]), f18: (String, Mapping[A18]), f19: (String, Mapping[A19]), f20: (String, Mapping[A20]), f21: (String, Mapping[A21]), f22: (String, Mapping[A22]), val key: String = "", val constraints: Seq[Constraint[R]] = Nil) extends Mapping[R] with ObjectMapping {

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

  val field20 = f20._2.withPrefix(f20._1).withPrefix(key)

  val field21 = f21._2.withPrefix(f21._1).withPrefix(key)

  val field22 = f22._2.withPrefix(f22._1).withPrefix(key)

  def bind(data: Map[String, String]): Either[Seq[FormError], R] = {
    merge(field1.bind(data), field2.bind(data), field3.bind(data), field4.bind(data), field5.bind(data), field6.bind(data), field7.bind(data), field8.bind(data), field9.bind(data), field10.bind(data), field11.bind(data), field12.bind(data), field13.bind(data), field14.bind(data), field15.bind(data), field16.bind(data), field17.bind(data), field18.bind(data), field19.bind(data), field20.bind(data), field21.bind(data), field22.bind(data)) match {
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
          values(17).asInstanceOf[A18],
          values(18).asInstanceOf[A19],
          values(19).asInstanceOf[A20],
          values(20).asInstanceOf[A21],
          values(21).asInstanceOf[A22]
        ))
      }
    }
  }

  def unbind(value: R): Map[String, String] = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22) = fields
      field1.unbind(v1) ++ field2.unbind(v2) ++ field3.unbind(v3) ++ field4.unbind(v4) ++ field5.unbind(v5) ++ field6.unbind(v6) ++ field7.unbind(v7) ++ field8.unbind(v8) ++ field9.unbind(v9) ++ field10.unbind(v10) ++ field11.unbind(v11) ++ field12.unbind(v12) ++ field13.unbind(v13) ++ field14.unbind(v14) ++ field15.unbind(v15) ++ field16.unbind(v16) ++ field17.unbind(v17) ++ field18.unbind(v18) ++ field19.unbind(v19) ++ field20.unbind(v20) ++ field21.unbind(v21) ++ field22.unbind(v22)
    }.getOrElse(Map.empty)
  }

  def unbindAndValidate(value: R): (Map[String, String], Seq[FormError]) = {
    unapply(value).map { fields =>
      val (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22) = fields
      val a1 = field1.unbindAndValidate(v1)
      val a2 = field2.unbindAndValidate(v2)
      val a3 = field3.unbindAndValidate(v3)
      val a4 = field4.unbindAndValidate(v4)
      val a5 = field5.unbindAndValidate(v5)
      val a6 = field6.unbindAndValidate(v6)
      val a7 = field7.unbindAndValidate(v7)
      val a8 = field8.unbindAndValidate(v8)
      val a9 = field9.unbindAndValidate(v9)
      val a10 = field10.unbindAndValidate(v10)
      val a11 = field11.unbindAndValidate(v11)
      val a12 = field12.unbindAndValidate(v12)
      val a13 = field13.unbindAndValidate(v13)
      val a14 = field14.unbindAndValidate(v14)
      val a15 = field15.unbindAndValidate(v15)
      val a16 = field16.unbindAndValidate(v16)
      val a17 = field17.unbindAndValidate(v17)
      val a18 = field18.unbindAndValidate(v18)
      val a19 = field19.unbindAndValidate(v19)
      val a20 = field20.unbindAndValidate(v20)
      val a21 = field21.unbindAndValidate(v21)
      val a22 = field22.unbindAndValidate(v22)

      (a1._1 ++ a2._1 ++ a3._1 ++ a4._1 ++ a5._1 ++ a6._1 ++ a7._1 ++ a8._1 ++ a9._1 ++ a10._1 ++ a11._1 ++ a12._1 ++ a13._1 ++ a14._1 ++ a15._1 ++ a16._1 ++ a17._1 ++ a18._1 ++ a19._1 ++ a20._1 ++ a21._1 ++ a22._1) ->
        (a1._2 ++ a2._2 ++ a3._2 ++ a4._2 ++ a5._2 ++ a6._2 ++ a7._2 ++ a8._2 ++ a9._2 ++ a10._2 ++ a11._2 ++ a12._2 ++ a13._2 ++ a14._2 ++ a15._2 ++ a16._2 ++ a17._2 ++ a18._2 ++ a19._2 ++ a20._2 ++ a21._2 ++ a22._2)
    }.getOrElse(Map.empty[String, String] -> Seq(FormError(key, "unbind.failed")))
  }

  def withPrefix(prefix: String): ObjectMapping22[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22] = addPrefix(prefix).map(newKey =>
    new ObjectMapping22(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, f22, newKey, constraints)
  ).getOrElse(this)

  def verifying(addConstraints: Constraint[R]*): ObjectMapping22[R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22] = {
    new ObjectMapping22(apply, unapply, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, f22, key, constraints ++ addConstraints.toSeq)
  }

  val mappings = Seq(this) ++ field1.mappings ++ field2.mappings ++ field3.mappings ++ field4.mappings ++ field5.mappings ++ field6.mappings ++ field7.mappings ++ field8.mappings ++ field9.mappings ++ field10.mappings ++ field11.mappings ++ field12.mappings ++ field13.mappings ++ field14.mappings ++ field15.mappings ++ field16.mappings ++ field17.mappings ++ field18.mappings ++ field19.mappings ++ field20.mappings ++ field21.mappings ++ field22.mappings

}

