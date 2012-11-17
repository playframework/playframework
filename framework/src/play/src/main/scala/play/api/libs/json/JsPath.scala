package play.api.libs.json

import play.api.data.validation.ValidationError

sealed trait PathNode {
  def apply(json: JsValue): List[JsValue]
  def toJsonString: String

  private[json] def splitChildren(json: JsValue): List[Either[(PathNode, JsValue), (PathNode, JsValue)]]
  def set(json: JsValue, transform: JsValue => JsValue): JsValue

  private[json] def toJsonField(value: JsValue): JsValue = value

}

case class RecursiveSearch(key: String) extends PathNode {
  def apply(json: JsValue): List[JsValue] = json match {
    case obj: JsObject => (json \\ key).toList.filterNot{ 
      case JsUndefined(_) => true 
      case _ => false
    }
    case _ => List()
  }
  override def toString = "//" + key
  def toJsonString = "*" + key

  /**
   * First found, first set and never goes down after setting
   */
  def set(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case obj: JsObject => 
      var found = false 
      val o = JsObject(obj.fields.map{ case (k,v) => 
        if(k == this.key) { 
          found = true
          k -> transform(v) 
        }
        else k -> set(v, transform) 
      })

      o
    case _ => json
  }  

  private[json] def splitChildren(json: JsValue) = json match {
    case obj: JsObject => obj.fields.toList.map{ case (k,v) => 
      if(k == this.key) Right(this -> v)
      else Left(KeyPathNode(k) -> v)
    }
    case arr: JsArray => 
      arr.value.toList.zipWithIndex.map{ case (js, j) => Left(IdxPathNode(j) -> js) }
    
    case _ => List()
  } 
}
 
case class KeyPathNode(key: String) extends PathNode{

  def apply(json: JsValue): List[JsValue] = json match {
    case obj: JsObject => List(json \ key).filterNot{ 
      case JsUndefined(_) => true 
      case _ => false
    }
    case _ => List()
  }

  override def toString = "/" + key
  def toJsonString = "." + key

  def set(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case obj: JsObject => 
      var found = false 
      val o = JsObject(obj.fields.map{ case (k,v) => 
        if(k == this.key) { 
          found = true
          k -> transform(v) 
        }
        else k -> v 
      })
      if(!found) o ++ Json.obj(this.key -> transform(Json.obj()))
      else o
    case _ => transform(json)
  }

  private[json] def splitChildren(json: JsValue) = json match {
    case obj: JsObject => obj.fields.toList.map{ case (k,v) => 
      if(k == this.key) Right(this -> v)
      else Left(KeyPathNode(k) -> v)
    }
    case _ => List()
  } 

  private[json] override def toJsonField(value: JsValue) = Json.obj(key -> value)

}

case class IdxPathNode(idx: Int) extends PathNode {
  def apply(json: JsValue): List[JsValue] = json match {
    case arr: JsArray => List(arr(idx))
    case _ => List()
  }

  override def toString = "(%d)".format(idx)
  def toJsonString = "[%d]".format(idx)

  def set(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case arr: JsArray => JsArray(arr.value.zipWithIndex.map{ case (js, j) => if(j == idx) transform(js) else js})
    case _ => transform(json)
  }

  private[json] def splitChildren(json: JsValue) = json match {
    case arr: JsArray => arr.value.toList.zipWithIndex.map{ case (js, j) => 
      if(j == idx) Right(this -> js) 
      else Left(IdxPathNode(j) -> js)
    }
    case _ => List()
  } 

  private[json] override def toJsonField(value: JsValue) = value

}

object __ extends JsPath

object JsPath {
  def \(child: String) = JsPath() \ child
  def \(child: Symbol) = JsPath() \ child

  def \\(child: String) = JsPath() \\ child
  def \\(child: Symbol) = JsPath() \\ child

  def apply(idx: Int): JsPath = JsPath()(idx)

  // TODO implement it correctly (doesn't merge )
  def createObj(pathValues: (JsPath, JsValue)*) = {

    def buildSubPath(path: JsPath, value: JsValue) = {
      def step(path: List[PathNode], value: JsValue): JsObject = {
        path match {
          case List() => value match {
            case obj: JsObject => obj
            case _ => throw new RuntimeException("when empty JsPath, expecting JsObject")
          }
          case List(p) => p match {
            case KeyPathNode(key) => Json.obj(key -> value)
            case _ => throw new RuntimeException("expected KeyPathNode")
          }
          case head :: tail => head match {
            case KeyPathNode(key) => Json.obj(key -> step(tail, value))
            case _ => throw new RuntimeException("expected KeyPathNode")
          }
        }
      }

      step(path.path, value)
    }

    pathValues.foldLeft(Json.obj()){ (obj, pv) => 
      val (path, value) = (pv._1, pv._2)
      val subobj = buildSubPath(path, value)
      obj.deepMerge(subobj)
    }
  }
}

case class JsPath(path: List[PathNode] = List()) {
  def \(child: String) = JsPath(path :+ KeyPathNode(child))
  def \(child: Symbol) = JsPath(path :+ KeyPathNode(child.name))

  def \\(child: String) = JsPath(path :+ RecursiveSearch(child))
  def \\(child: Symbol) = JsPath(path :+ RecursiveSearch(child.name))

  def apply(idx: Int): JsPath = JsPath(path :+ IdxPathNode(idx))

  def apply(json: JsValue): List[JsValue] =  path.foldLeft(List(json))((s,p) => s.flatMap(p.apply))

  def asSingleJsResult(json: JsValue): JsResult[JsValue] = this(json) match {
    case Nil => JsError(Seq(this -> Seq(ValidationError("validate.error.missing-path"))))
    case List(js) => JsSuccess(js)
    case head :: tail => JsError(Seq(this -> Seq(ValidationError("validate.error.multiple-result-path"))))
  }

  def asSingleJson(json: JsValue): JsValue = this(json) match {
    case Nil => JsUndefined("not.found")
    case List(js) => js
    case head::tail => JsUndefined("multiple.result")
  }

  override def toString = path.mkString
  def toJsonString = path.foldLeft("obj")((acc, p) => acc + p.toJsonString)

  def compose(other: JsPath) = JsPath(path ++ other.path)
  def ++(other: JsPath) = this compose other

  /**
   * Simple Prune for simple path and only JsObject
   */
  def prune(js: JsValue) = {
    def stepNode(json: JsObject, node: PathNode): JsResult[JsObject] = {
      node match {
        case KeyPathNode(key) => JsSuccess(json - key)
        case _ => JsError(JsPath(), ValidationError("validate.error.expected.keypathnode"))
      }
    }

    def filterPathNode(json: JsObject, node: PathNode, value: JsValue): JsResult[JsObject] = {
      node match {
        case KeyPathNode(key) => JsSuccess(JsObject(json.fields.filterNot( _._1 == key )) ++ Json.obj( key -> value ))
        case _ => JsError(JsPath(), ValidationError("validate.error.expected.keypathnode"))
      }
    }

    def step(json: JsObject, lpath: JsPath): JsResult[JsObject] = {
      lpath.path match {
        case Nil => JsSuccess(json)
        case List(p) => stepNode(json, p).repath(lpath)
        case head :: tail => head(json) match {
          case Nil => JsError(lpath, ValidationError("validate.error.missing-path"))
          case List(js) => 
            js match {
              case o: JsObject => 
                step(o, JsPath(tail)).repath(lpath).flatMap( value => 
                   filterPathNode(json, head, value)
                )
              case _ => JsError(lpath, ValidationError("validate.error.expected.jsobject"))
            }
          case h :: t => JsError(lpath, ValidationError("validate.error.multiple-result-path"))
        }
      }
    }

    js match {
      case o: JsObject => step(o, this)
      case _ => 
        JsError(this, ValidationError("validate.error.expected.jsobject"))
    }    
  }

  /**
   * Reads/Writes/Format builders
   */
  def read[T](implicit r: Reads[T]) = Reads.at[T](this)(r)
  def readOpt[T](implicit r: Reads[T]) = Reads.optional[T](this)(r)
  def read[T](t: T) = Reads.pure(t)
  
  def lazyRead[T](r: => Reads[T]) = Reads( js => Reads.at[T](this)(r).reads(js) )
  
  def write[T](implicit w: Writes[T]) = Writes.at[T](this)(w)
  def writeOpt[T](implicit w: Writes[T]) = Writes.optional[T](this)(w)
  def lazyWrite[T](w: => Writes[T]) = OWrites( (t:T) => Writes.at[T](this)(w).writes(t) )
  def write[T](t: T)(implicit w: Writes[T]) = Writes.pure(this, t)

  def rw[T](implicit r:Reads[T], w:Writes[T]) = Format.at[T](this)(Format(r, w))

  def format[T](implicit f: Format[T]) = Format.at[T](this)(f)
  def formatOpt[T](implicit f: Format[T]): OFormat[Option[T]] = Format.optional[T](this)(f)
  def lazyFormat[T](f: => Format[T]) = OFormat[T]( lazyRead(f), lazyWrite(f) )
  def lazyFormat[T](r: => Reads[T], w: => Writes[T]) = OFormat[T]( lazyRead(r), lazyWrite(w) )
  def format[T](r: Reads[T])(implicit w: Writes[T]) = Format.at[T](this)(Format(r, w))
  def format[T](w: Writes[T])(implicit r: Reads[T]) = Format.at[T](this)(Format(r, w))

  

  private val self = this

  object json {
    /**
     * (__ \ 'key).json.pick[A <: JsValue] is a Reads[A] that:
     * - picks the given value at the given JsPath (WITHOUT THE PATH) from the input JS
     * - validates this element as an object of type A (inheriting JsValue)
     * - returns a JsResult[A]
     * Useful to pick a typed JsValue at a given JsPath
     *
     * Example :
    {{{
    val js = Json.obj("key1" -> "value1", "key2" -> 123) 
    js.validate( (__ \ 'key2).json.pick[JsNumber] ) 
    => JsSuccess(JsNumber(123))
    }}}
     */
    def pick[A <: JsValue](implicit r: Reads[A]): Reads[A] = Reads.jsPick(self)

    /**
     * (__ \ 'key).json.pick is a Reads[JsValue] that:
     * - picks the given value at the given JsPath (WITHOUT THE PATH) from the input JS
     * - validates this element as an object of type JsValue
     * - returns a JsResult[JsValue]
     * Useful to pick a JsValue at a given JsPath
     *
     * Example :
    {{{
    val js = Json.obj("key1" -> "value1", "key2" -> "value2") 
    js.validate( (__ \ 'key2).json.pick ) 
    => JsSuccess(JsString("value2"))
    }}}
     */
    def pick: Reads[JsValue] = pick[JsValue]

    /**
     * (__ \ 'key).json.pickBranch[A <: JsValue](readsOfA) is a Reads[JsObject] that:
     * - copies the given branch (JsPath + relative JsValue) from the input JS at this given JsPath
     * - validates this relative JsValue as an object of type A (inheriting JsValue) potentially modifying it
     * - creates a JsObject from JsPath and validated JsValue
     * - returns a JsResult[JsObject]
     * Useful to create/validate an JsObject from a single JsPath (potentially modifying it)
     *
     * Example :
    {{{
    val js = Json.obj("key1" -> "value1", "key2" -> Json.obj( "key21" -> "value2") )
    js.validate( (__ \ 'key2).json.pickBranch[JsString]( (__ \ 'key21').json.pick[JsString].map( (js: JsString) => JsString(js.value ++ "3456") ) ) )
    => JsSuccess(JsObject(Seq( ("key2", JsString(value23456")) )))
    }}}
     */
    def pickBranch[A <: JsValue](reads: Reads[A]): Reads[JsObject] = Reads.jsPickBranch[A](self)(reads)
    
    /**
     * (__ \ 'key).json.pickBranch is a Reads[JsObject] that:
     * - copies the given branch (JsPath + relative JsValue) from the input JS at this given JsPath
     * - creates a JsObject from JsPath and JsValue
     * - returns a JsResult[JsObject]
     * Useful to create/validate an JsObject from a single JsPath (potentially modifying it)
     *
     * Example :
    {{{
    val js = Json.obj("key1" -> "value1", "key2" -> Json.obj( "key21" -> "value2") )
    js.validate( (__ \ 'key2).json.pickBranch )
    => JsSuccess(JsObject(Seq( ("key2", Json.obj("key21" -> "value2")) )))
    }}}
     */
    def pickBranch: Reads[JsObject] = Reads.jsPickBranch[JsValue](self)


    /**
     * (__ \ 'key).put(fixedValue) is a Reads[JsObject] that:
     * - creates a JsObject setting A (inheriting JsValue) at given JsPath
     * - returns a JsResult[JsObject]
     * This Reads doesn't care about the input JS and is mainly used to set a fixed at a given JsPath
     * Please that A is passed by name allowing to use an expression reevaluated at each time.
     *
     * Example :
    {{{
    val js = Json.obj("key1" -> "value1", "key2" -> "value2") 
    js.validate( (__ \ 'key3).put( { JsNumber((new java.util.Date).getTime()) } ) ) 
    => JsSuccess(JsObject(Seq( ("key3", JsNumber(123364687568756)) )))
    }}}
     */
    def put(a: => JsValue): Reads[JsObject] = Reads.jsPut(self, a)

    def copyFrom[A <: JsValue](reads: Reads[A]): Reads[JsObject] = Reads.jsCopyTo(self)(reads)

    def update[A <: JsValue](reads: Reads[A]): Reads[JsObject] = Reads.jsUpdate(self)(reads)

    def prune: Reads[JsObject] = Reads.jsPrune(self)
  }
  
}
