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

}
