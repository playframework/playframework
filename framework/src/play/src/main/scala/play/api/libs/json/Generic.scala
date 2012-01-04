package play.api.libs.json

object Generic {

  def productFormat2[S, T1, T2](lbl1: String, lbl2: String)(apply: (T1, T2) => S)(unapply: S => Product2[T1, T2])(implicit f1: Format[T1], f2: Format[T2]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat3[S, T1, T2, T3](lbl1: String, lbl2: String, lbl3: String)(apply: (T1, T2, T3) => S)(unapply: S => Product3[T1, T2, T3])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2),
          lbl3 -> toJson(product._3)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat4[S, T1, T2, T3, T4](lbl1: String, lbl2: String, lbl3: String, lbl4: String)(apply: (T1, T2, T3, T4) => S)(unapply: S => Product4[T1, T2, T3, T4])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2),
          lbl3 -> toJson(product._3),
          lbl4 -> toJson(product._4)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat5[S, T1, T2, T3, T4, T5](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String)(apply: (T1, T2, T3, T4, T5) => S)(unapply: S => Product5[T1, T2, T3, T4, T5])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2),
          lbl3 -> toJson(product._3),
          lbl4 -> toJson(product._4),
          lbl5 -> toJson(product._5)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat6[S, T1, T2, T3, T4, T5, T6](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String)(apply: (T1, T2, T3, T4, T5, T6) => S)(unapply: S => Product6[T1, T2, T3, T4, T5, T6])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2),
          lbl3 -> toJson(product._3),
          lbl4 -> toJson(product._4),
          lbl5 -> toJson(product._5),
          lbl6 -> toJson(product._6)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat7[S, T1, T2, T3, T4, T5, T6, T7](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String)(apply: (T1, T2, T3, T4, T5, T6, T7) => S)(unapply: S => Product7[T1, T2, T3, T4, T5, T6, T7])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2),
          lbl3 -> toJson(product._3),
          lbl4 -> toJson(product._4),
          lbl5 -> toJson(product._5),
          lbl6 -> toJson(product._6),
          lbl7 -> toJson(product._7)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat8[S, T1, T2, T3, T4, T5, T6, T7, T8](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8) => S)(unapply: S => Product8[T1, T2, T3, T4, T5, T6, T7, T8])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2),
          lbl3 -> toJson(product._3),
          lbl4 -> toJson(product._4),
          lbl5 -> toJson(product._5),
          lbl6 -> toJson(product._6),
          lbl7 -> toJson(product._7),
          lbl8 -> toJson(product._8)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat9[S, T1, T2, T3, T4, T5, T6, T7, T8, T9](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9) => S)(unapply: S => Product9[T1, T2, T3, T4, T5, T6, T7, T8, T9])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
          lbl1 -> toJson(product._1),
          lbl2 -> toJson(product._2),
          lbl3 -> toJson(product._3),
          lbl4 -> toJson(product._4),
          lbl5 -> toJson(product._5),
          lbl6 -> toJson(product._6),
          lbl7 -> toJson(product._7),
          lbl8 -> toJson(product._8),
          lbl9 -> toJson(product._9)
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat10[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => S)(unapply: S => Product10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat11[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => S)(unapply: S => Product11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat12[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => S)(unapply: S => Product12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat13[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => S)(unapply: S => Product13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat14[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => S)(unapply: S => Product14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat15[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => S)(unapply: S => Product15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat16[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => S)(unapply: S => Product16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15)),
          fromJson[T16](m(lbl16))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat17[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => S)(unapply: S => Product17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15)),
          fromJson[T16](m(lbl16)),
          fromJson[T17](m(lbl17))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat18[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => S)(unapply: S => Product18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15)),
          fromJson[T16](m(lbl16)),
          fromJson[T17](m(lbl17)),
          fromJson[T18](m(lbl18))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat19[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => S)(unapply: S => Product19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15)),
          fromJson[T16](m(lbl16)),
          fromJson[T17](m(lbl17)),
          fromJson[T18](m(lbl18)),
          fromJson[T19](m(lbl19))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat20[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String, lbl20: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => S)(unapply: S => Product20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19], f20: Format[T20]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15)),
          fromJson[T16](m(lbl16)),
          fromJson[T17](m(lbl17)),
          fromJson[T18](m(lbl18)),
          fromJson[T19](m(lbl19)),
          fromJson[T20](m(lbl20))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat21[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String, lbl20: String, lbl21: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21) => S)(unapply: S => Product21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19], f20: Format[T20], f21: Format[T21]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15)),
          fromJson[T16](m(lbl16)),
          fromJson[T17](m(lbl17)),
          fromJson[T18](m(lbl18)),
          fromJson[T19](m(lbl19)),
          fromJson[T20](m(lbl20)),
          fromJson[T21](m(lbl21))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

  def productFormat22[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](lbl1: String, lbl2: String, lbl3: String, lbl4: String, lbl5: String, lbl6: String, lbl7: String, lbl8: String, lbl9: String, lbl10: String, lbl11: String, lbl12: String, lbl13: String, lbl14: String, lbl15: String, lbl16: String, lbl17: String, lbl18: String, lbl19: String, lbl20: String, lbl21: String, lbl22: String)(apply: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22) => S)(unapply: S => Product22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22])(implicit f1: Format[T1], f2: Format[T2], f3: Format[T3], f4: Format[T4], f5: Format[T5], f6: Format[T6], f7: Format[T7], f8: Format[T8], f9: Format[T9], f10: Format[T10], f11: Format[T11], f12: Format[T12], f13: Format[T13], f14: Format[T14], f15: Format[T15], f16: Format[T16], f17: Format[T17], f18: Format[T18], f19: Format[T19], f20: Format[T20], f21: Format[T21], f22: Format[T22]) = new Format[S] {
    def writes(s: S) = {
      val product = unapply(s)
      JsObject(
        Seq(
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
        )
      )
    }
    def reads(js: JsValue) = js match {
      case JsObject(seq) =>
        val m = seq.toMap
        apply(
          fromJson[T1](m(lbl1)),
          fromJson[T2](m(lbl2)),
          fromJson[T3](m(lbl3)),
          fromJson[T4](m(lbl4)),
          fromJson[T5](m(lbl5)),
          fromJson[T6](m(lbl6)),
          fromJson[T7](m(lbl7)),
          fromJson[T8](m(lbl8)),
          fromJson[T9](m(lbl9)),
          fromJson[T10](m(lbl10)),
          fromJson[T11](m(lbl11)),
          fromJson[T12](m(lbl12)),
          fromJson[T13](m(lbl13)),
          fromJson[T14](m(lbl14)),
          fromJson[T15](m(lbl15)),
          fromJson[T16](m(lbl16)),
          fromJson[T17](m(lbl17)),
          fromJson[T18](m(lbl18)),
          fromJson[T19](m(lbl19)),
          fromJson[T20](m(lbl20)),
          fromJson[T21](m(lbl21)),
          fromJson[T22](m(lbl22))
        )
      case _ => throw new RuntimeException("object expected")
    }
  }

}

