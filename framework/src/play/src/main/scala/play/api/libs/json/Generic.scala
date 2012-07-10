package play.api.libs.json

import Json._

/**
 * Generic Json formatters to handle Product types.
 */
object Generic {
  import JsResultHelpers._

  def productFormat2[S, T1, T2](lbl1: String, lbl2: String)(apply: (T1, T2) => S)(unapply: S => Option[Product2[T1, T2]])(implicit f1: Format[T1], f2: Format[T2]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          Json.obj(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2)
          )
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat3[S, T1, T2, T3](lbl1: String, lbl2: String, lbl3: String)(apply: (T1, T2, T3) => S)(unapply: S => Option[Product3[T1, T2, T3]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat4[S, T1, T2, T3, T4](lbl1: String, lbl2: String, lbl3: String, lbl4: String)(apply: (T1, T2, T3, T4) => S)(unapply: S => Option[Product4[T1, T2, T3, T4]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat5[S, T1, T2, T3, T4, T5](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String)(apply: (T1, T2, T3, T4, T5) => S)(unapply: S => Option[Product5[T1, T2, T3, T4, T5]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat6[S, T1, T2, T3, T4, T5, T6](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String)(apply: (T1, T2, T3, T4, T5, T6) => S)(unapply: S => Option[Product6[T1, T2, T3, T4, T5, T6]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat7[S, T1, T2, T3, T4, T5, T6, T7](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String)(apply: (T1, T2, T3, T4, T5, T6, T7) => S)(unapply: S => Option[Product7[T1, T2, T3, T4, T5, T6, T7]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat8[S, T1, T2, T3, T4, T5, T6, T7, T8](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8) => S)(unapply: S => Option[Product8[T1, T2, T3, T4, T5, T6, T7, T8]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat9[S, T1, T2, T3, T4, T5, T6, T7, T8, T9](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => S)(unapply: S => Option[Product9[T1, T2, T3, T4, T5, T6, T7, T8, T9]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat10[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => S)(unapply: S => Option[Product10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat11[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => S)(unapply: S => Option[Product11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat12[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => S)(unapply: S => Option[Product12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat13[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => S)(unapply: S => Option[Product13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat14[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => S)(unapply: S => Option[Product14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat15[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => S)(unapply: S => Option[Product15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat16[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => S)(unapply: S => Option[Product16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15),
            lbl16 -> toJson(product._16)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15),
          fromJson[T16](o \ lbl16)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat17[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => S)(unapply: S => Option[Product17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15),
            lbl16 -> toJson(product._16),
            lbl17 -> toJson(product._17)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15),
          fromJson[T16](o \ lbl16),
          fromJson[T17](o \ lbl17)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat18[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => S)(unapply: S => Option[Product18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15),
            lbl16 -> toJson(product._16),
            lbl17 -> toJson(product._17),
            lbl18 -> toJson(product._18)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15),
          fromJson[T16](o \ lbl16),
          fromJson[T17](o \ lbl17),
          fromJson[T18](o \ lbl18)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat19[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => S)(unapply: S => Option[Product19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15),
            lbl16 -> toJson(product._16),
            lbl17 -> toJson(product._17),
            lbl18 -> toJson(product._18),
            lbl19 -> toJson(product._19)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15),
          fromJson[T16](o \ lbl16),
          fromJson[T17](o \ lbl17),
          fromJson[T18](o \ lbl18),
          fromJson[T19](o \ lbl19)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat20[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String, lbl20: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => S)(unapply: S => Option[Product20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19], f20: Format[T20]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15),
            lbl16 -> toJson(product._16),
            lbl17 -> toJson(product._17),
            lbl18 -> toJson(product._18),
            lbl19 -> toJson(product._19),
            lbl20 -> toJson(product._20)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15),
          fromJson[T16](o \ lbl16),
          fromJson[T17](o \ lbl17),
          fromJson[T18](o \ lbl18),
          fromJson[T19](o \ lbl19),
          fromJson[T20](o \ lbl20)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat21[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String, lbl20: String, lbl21: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21) => S)(unapply: S => Option[Product21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19], f20: Format[T20], f21: Format[T21]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15),
            lbl16 -> toJson(product._16),
            lbl17 -> toJson(product._17),
            lbl18 -> toJson(product._18),
            lbl19 -> toJson(product._19),
            lbl20 -> toJson(product._20),
            lbl21 -> toJson(product._21)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15),
          fromJson[T16](o \ lbl16),
          fromJson[T17](o \ lbl17),
          fromJson[T18](o \ lbl18),
          fromJson[T19](o \ lbl19),
          fromJson[T20](o \ lbl20),
          fromJson[T21](o \ lbl21)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }

def productFormat22[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String, lbl20: String, lbl21: String, lbl22: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22) => S)(unapply: S => Option[Product22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19], f20: Format[T20], f21: Format[T21], f22: Format[T22]) = new Format[S] {
    def writes(s: S) = {
      unapply(s) match {
        case Some(product) =>
          JsObject(Seq(
            lbl1 -> toJson(product._1),
            lbl2 -> toJson(product._2),
            lbl3 -> toJson(product._3),
            lbl4 -> toJson(product._4),
            lbl5 -> toJson(product._5),
            lbl6 -> toJson(product._6),
            lbl7 -> toJson(product._7),
            lbl8 -> toJson(product._8),
            lbl9 -> toJson(product._9),
            lbl10 -> toJson(product._10),
            lbl11 -> toJson(product._11),
            lbl12 -> toJson(product._12),
            lbl13 -> toJson(product._13),
            lbl14 -> toJson(product._14),
            lbl15 -> toJson(product._15),
            lbl16 -> toJson(product._16),
            lbl17 -> toJson(product._17),
            lbl18 -> toJson(product._18),
            lbl19 -> toJson(product._19),
            lbl20 -> toJson(product._20),
            lbl21 -> toJson(product._21),
            lbl22 -> toJson(product._22)
          ))
        case _ => throw new RuntimeException("product expected")
      }
    }
    def reads(js: JsValue) = js match {
      case o: JsObject =>
        product(
          fromJson[T1](o \ lbl1),
          fromJson[T2](o \ lbl2),
          fromJson[T3](o \ lbl3),
          fromJson[T4](o \ lbl4),
          fromJson[T5](o \ lbl5),
          fromJson[T6](o \ lbl6),
          fromJson[T7](o \ lbl7),
          fromJson[T8](o \ lbl8),
          fromJson[T9](o \ lbl9),
          fromJson[T10](o \ lbl10),
          fromJson[T11](o \ lbl11),
          fromJson[T12](o \ lbl12),
          fromJson[T13](o \ lbl13),
          fromJson[T14](o \ lbl14),
          fromJson[T15](o \ lbl15),
          fromJson[T16](o \ lbl16),
          fromJson[T17](o \ lbl17),
          fromJson[T18](o \ lbl18),
          fromJson[T19](o \ lbl19),
          fromJson[T20](o \ lbl20),
          fromJson[T21](o \ lbl21),
          fromJson[T22](o \ lbl22)).map( apply.tupled(_) )
      case _ => throw new RuntimeException("object expected")
    }
  }  

}

object JsResultHelpers extends JsResultHelpers

trait JsResultHelpers {
  def product[T1, T2](
    t1: JsResult[T1], 
    t2: JsResult[T2]
  ): JsResult[(T1, T2)] = t1 prod t2

  def product[T1, T2, T3](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3]
  ): JsResult[(T1, T2, T3)] = {
    (t1 prod t2 prod t3) match {
      case JsSuccess(((t1, t2), t3)) => JsSuccess(t1, t2, t3)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4]
  ): JsResult[(T1, T2, T3, T4)] = {
    (t1 prod t2 prod t3 prod t4) match {
      case JsSuccess((((t1, t2), t3), t4)) => JsSuccess(t1, t2, t3, t4)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5]
  ): JsResult[(T1, T2, T3, T4, T5)] = {
    (t1 prod t2 prod t3 prod t4 prod t5) match {
      case JsSuccess(((((t1, t2), t3), t4), t5)) => JsSuccess(t1, t2, t3, t4, t5)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6]
  ): JsResult[(T1, T2, T3, T4, T5, T6)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6) match {
      case JsSuccess((((((t1, t2), t3), t4), t5), t6)) => JsSuccess(t1, t2, t3, t4, t5, t6)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7) match {
      case JsSuccess(((((((t1, t2), t3), t4), t5), t6), t7)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8) match {
      case JsSuccess((((((((t1, t2), t3), t4), t5), t6), t7), t8)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9) match {
      case JsSuccess(((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10) match {
      case JsSuccess((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11) match {
      case JsSuccess(((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12) match {
      case JsSuccess((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13) match {
      case JsSuccess(((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14) match {
      case JsSuccess((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15) match {
      case JsSuccess(((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16) match {
      case JsSuccess((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16)
      case JsError(e) => JsError(e)
    }
  }

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17) match {
      case JsSuccess(((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18) match {
      case JsSuccess((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19) match {
      case JsSuccess(((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19], 
    t20: JsResult[T20]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19 prod t20) match {
      case JsSuccess((((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19), t20)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19], 
    t20: JsResult[T20], 
    t21: JsResult[T21]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19 prod t20 prod t21) match {
      case JsSuccess(((((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19), t20), t21)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21)
      case JsError(e) => JsError(e)
    }
  } 

  def product[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](
    t1: JsResult[T1], 
    t2: JsResult[T2], 
    t3: JsResult[T3], 
    t4: JsResult[T4], 
    t5: JsResult[T5], 
    t6: JsResult[T6], 
    t7: JsResult[T7], 
    t8: JsResult[T8], 
    t9: JsResult[T9], 
    t10: JsResult[T10], 
    t11: JsResult[T11], 
    t12: JsResult[T12], 
    t13: JsResult[T13], 
    t14: JsResult[T14], 
    t15: JsResult[T15], 
    t16: JsResult[T16], 
    t17: JsResult[T17], 
    t18: JsResult[T18], 
    t19: JsResult[T19], 
    t20: JsResult[T20], 
    t21: JsResult[T21], 
    t22: JsResult[T22]
  ): JsResult[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] = {
    (t1 prod t2 prod t3 prod t4 prod t5 prod t6 prod t7 prod t8 prod t9 prod t10 prod t11 prod t12 prod t13 prod t14 prod t15 prod t16 prod t17 prod t18 prod t19 prod t20 prod t21 prod t22) match {
      case JsSuccess((((((((((((((((((((((t1, t2), t3), t4), t5), t6), t7), t8), t9), t10), t11), t12), t13), t14), t15), t16), t17), t18), t19), t20), t21), t22)) => JsSuccess(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22)
      case JsError(e) => JsError(e)
    }
  } 

}
