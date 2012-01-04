package play.api.libs.json

object `package` {

  /**
   * Provided a Reads implicit for its type is available, convert any object into a JsValue
   */
  def toJson[T](o: T)(implicit tjs: Writes[T]): JsValue = tjs.writes(o)

  /**
   * Provided a Writes implicit for that type is available, convert a JsValue to any type
   */
  def fromJson[T](json: JsValue)(implicit fjs: Reads[T]): T = fjs.reads(json)


  /**
   * Facilities to build Implicit Class Formatters (''basically case class'') from a class having 2 functions :
   *  - apply(T1, T2, T3, ..., TN) => S
   *  - extractor-like unapply S => Option[ProductN[T1, T2, T3, ..., TN]]
   *
   * To be added after class definition for ex:
   * {{{
   * case class Foo(bar1:String, bar2:Int)
   * implicit val FooFormat = buildFormat2("bar1", "bar2")(Foo.apply)(Foo.unapply)
   * //or shorter
   * implicit val FooFormat = buildFormat2("bar1", "bar2")(Foo)(Foo.unapply)
   * }}}
   */
  def buildFormat1[S, T1](f1: String)(apply : (T1) => S)(unapply : S => Option[T1])(implicit bin1: Format[T1]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat2[S, T1, T2](f1: String, f2: String)(apply : (T1, T2) => S)(unapply : S => Option[Product2[T1, T2]])(implicit bin1: Format[T1], bin2: Format[T2]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat3[S, T1, T2, T3](f1: String, f2: String, f3:String)(apply : (T1, T2, T3) => S)(unapply : S => Option[Product3[T1, T2, T3]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat4[S, T1, T2, T3, T4](f1: String, f2: String, f3:String, f4:String)(apply : (T1, T2, T3, T4) => S)(unapply : S => Option[Product4[T1, T2, T3, T4]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat5[S, T1, T2, T3, T4, T5](f1: String, f2: String, f3:String, f4:String, f5:String)(apply : (T1, T2, T3, T4, T5) => S)(unapply : S => Option[Product5[T1, T2, T3, T4, T5]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat6[S, T1, T2, T3, T4, T5, T6](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String)(apply : (T1, T2, T3, T4, T5, T6) => S)(unapply : S => Option[Product6[T1, T2, T3, T4, T5, T6]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat7[S, T1, T2, T3, T4, T5, T6, T7](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String)(apply : (T1, T2, T3, T4, T5, T6, T7) => S)(unapply : S => Option[Product7[T1, T2, T3, T4, T5, T6, T7]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat8[S, T1, T2, T3, T4, T5, T6, T7, T8](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8) => S)(unapply : S => Option[Product8[T1, T2, T3, T4, T5, T6, T7, T8]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat9[S, T1, T2, T3, T4, T5, T6, T7, T8, T9](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9) => S)(unapply : S => Option[Product9[T1, T2, T3, T4, T5, T6, T7, T8, T9]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat10[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) => S)(unapply : S => Option[Product10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat11[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) => S)(unapply : S => Option[Product11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat12[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) => S)(unapply : S => Option[Product12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat13[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) => S)(unapply : S => Option[Product13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat14[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) => S)(unapply : S => Option[Product14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat15[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) => S)(unapply : S => Option[Product15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat16[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String, f16:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) => S)(unapply : S => Option[Product16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15], bin16: Format[T16]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15)),
					(f16, toJson(prod._16))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15),
	        	fromJson[T16](o\f16)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat17[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String, f16:String, f17:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17) => S)(unapply : S => Option[Product17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15], bin16: Format[T16], bin17: Format[T17]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15)),
					(f16, toJson(prod._16)),
					(f17, toJson(prod._17))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15),
	        	fromJson[T16](o\f16),
	        	fromJson[T17](o\f17)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat18[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String, f16:String, f17:String, f18:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18) => S)(unapply : S => Option[Product18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15], bin16: Format[T16], bin17: Format[T17], bin18: Format[T18]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15)),
					(f16, toJson(prod._16)),
					(f17, toJson(prod._17)),
					(f18, toJson(prod._18))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15),
	        	fromJson[T16](o\f16),
	        	fromJson[T17](o\f17),
	        	fromJson[T18](o\f18)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat19[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String, f16:String, f17:String, f18:String, f19:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19) => S)(unapply : S => Option[Product19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15], bin16: Format[T16], bin17: Format[T17], bin18: Format[T18], bin19: Format[T19]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15)),
					(f16, toJson(prod._16)),
					(f17, toJson(prod._17)),
					(f18, toJson(prod._18)),
					(f19, toJson(prod._19))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15),
	        	fromJson[T16](o\f16),
	        	fromJson[T17](o\f17),
	        	fromJson[T18](o\f18),
	        	fromJson[T19](o\f19)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat20[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String, f16:String, f17:String, f18:String, f19:String, f20:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20) => S)(unapply : S => Option[Product20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15], bin16: Format[T16], bin17: Format[T17], bin18: Format[T18], bin19: Format[T19], bin20: Format[T20]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15)),
					(f16, toJson(prod._16)),
					(f17, toJson(prod._17)),
					(f18, toJson(prod._18)),
					(f19, toJson(prod._19)),
					(f20, toJson(prod._20))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15),
	        	fromJson[T16](o\f16),
	        	fromJson[T17](o\f17),
	        	fromJson[T18](o\f18),
	        	fromJson[T19](o\f19),
	        	fromJson[T20](o\f20)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat21[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String, f16:String, f17:String, f18:String, f19:String, f20:String, f21:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21) => S)(unapply : S => Option[Product21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15], bin16: Format[T16], bin17: Format[T17], bin18: Format[T18], bin19: Format[T19], bin20: Format[T20], bin21: Format[T21]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15)),
					(f16, toJson(prod._16)),
					(f17, toJson(prod._17)),
					(f18, toJson(prod._18)),
					(f19, toJson(prod._19)),
					(f20, toJson(prod._20)),
					(f21, toJson(prod._21))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15),
	        	fromJson[T16](o\f16),
	        	fromJson[T17](o\f17),
	        	fromJson[T18](o\f18),
	        	fromJson[T19](o\f19),
	        	fromJson[T20](o\f20),
	        	fromJson[T21](o\f21)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }

  def buildFormat22[S, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](f1: String, f2: String, f3:String, f4:String, f5:String, f6:String, f7:String, f8:String, f9:String, f10:String, f11:String, f12:String, f13:String, f14:String, f15:String, f16:String, f17:String, f18:String, f19:String, f20:String, f21:String, f22:String)(apply : (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22) => S)(unapply : S => Option[Product22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]])(implicit bin1: Format[T1], bin2: Format[T2], bin3: Format[T3], bin4: Format[T4], bin5: Format[T5], bin6: Format[T6], bin7: Format[T7], bin8: Format[T8], bin9: Format[T9], bin10: Format[T10], bin11: Format[T11], bin12: Format[T12], bin13: Format[T13], bin14: Format[T14], bin15: Format[T15], bin16: Format[T16], bin17: Format[T17], bin18: Format[T18], bin19: Format[T19], bin20: Format[T20], bin21: Format[T21], bin22: Format[T22]) = new Format[S]{
	  def writes(s: S) = {
	    unapply(s) match {
	    	case Some(prod) => 
	    		JsObject(List(
					(f1, toJson(prod._1)),
					(f2, toJson(prod._2)),
					(f3, toJson(prod._3)),
					(f4, toJson(prod._4)),
					(f5, toJson(prod._5)),
					(f6, toJson(prod._6)),
					(f7, toJson(prod._7)),
					(f8, toJson(prod._8)),
					(f9, toJson(prod._9)),
					(f10, toJson(prod._10)),
					(f11, toJson(prod._11)),
					(f12, toJson(prod._12)),
					(f13, toJson(prod._13)),
					(f14, toJson(prod._14)),
					(f15, toJson(prod._15)),
					(f16, toJson(prod._16)),
					(f17, toJson(prod._17)),
					(f18, toJson(prod._18)),
					(f19, toJson(prod._19)),
					(f20, toJson(prod._20)),
					(f21, toJson(prod._21)),
					(f22, toJson(prod._22))
				))
			case _ => throw new RuntimeException("product expected")
	    }
	  }
	  def reads(js: JsValue) = js match {
	    case o:JsObject =>
	     	apply(
	        	fromJson[T1](o\f1),
	        	fromJson[T2](o\f2),
	        	fromJson[T3](o\f3),
	        	fromJson[T4](o\f4),
	        	fromJson[T5](o\f5),
	        	fromJson[T6](o\f6),
	        	fromJson[T7](o\f7),
	        	fromJson[T8](o\f8),
	        	fromJson[T9](o\f9),
	        	fromJson[T10](o\f10),
	        	fromJson[T11](o\f11),
	        	fromJson[T12](o\f12),
	        	fromJson[T13](o\f13),
	        	fromJson[T14](o\f14),
	        	fromJson[T15](o\f15),
	        	fromJson[T16](o\f16),
	        	fromJson[T17](o\f17),
	        	fromJson[T18](o\f18),
	        	fromJson[T19](o\f19),
	        	fromJson[T20](o\f20),
	        	fromJson[T21](o\f21),
	        	fromJson[T22](o\f22)
	    	)
	    case _ => throw new RuntimeException("JsObject expected")
	  }
  }
}
