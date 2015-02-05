/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
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
    case obj: JsObject => (json \\ key).toList
    case arr: JsArray => (json \\ key).toList
    case _ => Nil
  }
  override def toString = "//" + key
  def toJsonString = "*" + key

  /**
   * First found, first set and never goes down after setting
   */
  def set(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case obj: JsObject =>
      var found = false
      val o = JsObject(obj.fields.map {
        case (k, v) =>
          if (k == this.key) {
            found = true
            k -> transform(v)
          } else k -> set(v, transform)
      })

      o
    case _ => json
  }

  private[json] def splitChildren(json: JsValue) = json match {
    case obj: JsObject => obj.fields.toList.map {
      case (k, v) =>
        if (k == this.key) Right(this -> v)
        else Left(KeyPathNode(k) -> v)
    }
    case arr: JsArray =>
      arr.value.toList.zipWithIndex.map { case (js, j) => Left(IdxPathNode(j) -> js) }

    case _ => List()
  }
}

case class KeyPathNode(key: String) extends PathNode {

  def apply(json: JsValue): List[JsValue] = json match {
    case obj: JsObject => List(json \ key).flatMap(_.toOption)
    case _ => List()
  }

  override def toString = "/" + key
  def toJsonString = "." + key

  def set(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case obj: JsObject =>
      var found = false
      val o = JsObject(obj.fields.map {
        case (k, v) =>
          if (k == this.key) {
            found = true
            k -> transform(v)
          } else k -> v
      })
      if (!found) o ++ Json.obj(this.key -> transform(Json.obj()))
      else o
    case _ => transform(json)
  }

  private[json] def splitChildren(json: JsValue) = json match {
    case obj: JsObject => obj.fields.toList.map {
      case (k, v) =>
        if (k == this.key) Right(this -> v)
        else Left(KeyPathNode(k) -> v)
    }
    case _ => List()
  }

  private[json] override def toJsonField(value: JsValue) = Json.obj(key -> value)

}

case class IdxPathNode(idx: Int) extends PathNode {
  def apply(json: JsValue): List[JsValue] = json match {
    case arr: JsArray => List(arr(idx)).flatMap(_.toOption)
    case _ => List()
  }

  override def toString = "(%d)".format(idx)
  def toJsonString = "[%d]".format(idx)

  def set(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case arr: JsArray => JsArray(arr.value.zipWithIndex.map { case (js, j) => if (j == idx) transform(js) else js })
    case _ => transform(json)
  }

  private[json] def splitChildren(json: JsValue) = json match {
    case arr: JsArray => arr.value.toList.zipWithIndex.map {
      case (js, j) =>
        if (j == idx) Right(this -> js)
        else Left(IdxPathNode(j) -> js)
    }
    case _ => List()
  }

  private[json] override def toJsonField(value: JsValue) = value

}

object JsPath extends JsPath(List.empty) {

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

    pathValues.foldLeft(Json.obj()) { (obj, pv) =>
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

  def apply(json: JsValue): List[JsValue] = path.foldLeft(List(json))((s, p) => s.flatMap(p.apply))

  def asSingleJsResult(json: JsValue): JsResult[JsValue] = this(json) match {
    case Nil => JsError(Seq(this -> Seq(ValidationError("error.path.missing"))))
    case List(js) => JsSuccess(js)
    case _ :: _ => JsError(Seq(this -> Seq(ValidationError("error.path.result.multiple"))))
  }

  def asSingleJson(json: JsValue): JsLookupResult = this(json) match {
    case Nil => JsUndefined("error.path.missing")
    case List(js) => JsDefined(js)
    case _ :: _ => JsUndefined("error.path.result.multiple")
  }

  def applyTillLast(json: JsValue): Either[JsError, JsResult[JsValue]] = {
    def step(path: List[PathNode], json: JsValue): Either[JsError, JsResult[JsValue]] = path match {
      case Nil => Left(JsError(Seq(this -> Seq(ValidationError("error.path.empty")))))
      case List(node) => node(json) match {
        case Nil => Right(JsError(Seq(this -> Seq(ValidationError("error.path.missing")))))
        case List(js) => Right(JsSuccess(js))
        case _ :: _ => Right(JsError(Seq(this -> Seq(ValidationError("error.path.result.multiple")))))
      }
      case head :: tail => head(json) match {
        case Nil => Left(JsError(Seq(this -> Seq(ValidationError("error.path.missing")))))
        case List(js) => step(tail, js)
        case _ :: _ => Left(JsError(Seq(this -> Seq(ValidationError("error.path.result.multiple")))))
      }
    }

    step(path, json)
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
        case _ => JsError(JsPath(), ValidationError("error.expected.keypathnode"))
      }
    }

    def filterPathNode(json: JsObject, node: PathNode, value: JsValue): JsResult[JsObject] = {
      node match {
        case KeyPathNode(key) => JsSuccess(JsObject(json.fields.filterNot(_._1 == key)) ++ Json.obj(key -> value))
        case _ => JsError(JsPath(), ValidationError("error.expected.keypathnode"))
      }
    }

    def step(json: JsObject, lpath: JsPath): JsResult[JsObject] = {
      lpath.path match {
        case Nil => JsSuccess(json)
        case List(p) => stepNode(json, p).repath(lpath)
        case head :: tail => head(json) match {
          case Nil => JsError(lpath, ValidationError("error.path.missing"))
          case List(js) =>
            js match {
              case o: JsObject =>
                step(o, JsPath(tail)).repath(lpath).flatMap(value =>
                  filterPathNode(json, head, value)
                )
              case _ => JsError(lpath, ValidationError("error.expected.jsobject"))
            }
          case h :: t => JsError(lpath, ValidationError("error.path.result.multiple"))
        }
      }
    }

    js match {
      case o: JsObject => step(o, this) match {
        case s: JsSuccess[JsObject] => s.copy(path = this)
        case e => e
      }
      case _ =>
        JsError(this, ValidationError("error.expected.jsobject"))
    }
  }

  /** Reads a T at JsPath */
  def read[T](implicit r: Reads[T]): Reads[T] = Reads.at[T](this)(r)

  /**
   * Reads a Option[T] search optional or nullable field at JsPath (field not found or null is None
   * and other cases are Error).
   *
   * It runs through JsValue following all JsPath nodes on JsValue except last node:
   * - If one node in JsPath is not found before last node => returns JsError( "missing-path" )
   * - If all nodes are found till last node, it runs through JsValue with last node =>
   *   - If last node is not found => returns None
   *   - If last node is found with value "null" => returns None
   *   - If last node is found => applies implicit Reads[T]
   */
  def readNullable[T](implicit r: Reads[T]): Reads[Option[T]] = Reads.nullable[T](this)(r)

  /**
   * Reads a T at JsPath using the explicit Reads[T] passed by name which is useful in case of
   * recursive case classes for ex.
   *
   * {{{
   * case class User(id: Long, name: String, friend: User)
   *
   * implicit lazy val UserReads: Reads[User] = (
   *   (__ \ 'id).read[Long] and
   *   (__ \ 'name).read[String] and
   *   (__ \ 'friend).lazyRead(UserReads)
   * )(User.apply _)
   * }}}
   */
  def lazyRead[T](r: => Reads[T]): Reads[T] = Reads(js => Reads.at[T](this)(r).reads(js))

  /**
   * Reads lazily a Option[T] search optional or nullable field at JsPath using the explicit Reads[T]
   * passed by name which is useful in case of recursive case classes for ex.
   *
   * {{{
   * case class User(id: Long, name: String, friend: Option[User])
   *
   * implicit lazy val UserReads: Reads[User] = (
   *   (__ \ 'id).read[Long] and
   *   (__ \ 'name).read[String] and
   *   (__ \ 'friend).lazyReadNullable(UserReads)
   * )(User.apply _)
   * }}}
   */
  def lazyReadNullable[T](r: => Reads[T]): Reads[Option[T]] = Reads(js => Reads.nullable[T](this)(r).reads(js))

  /** Pure Reads doesn't read anything but creates a JsObject based on JsPath with the given T value */
  def read[T](t: T) = Reads.pure(t)

  /** Writes a T at given JsPath */
  def write[T](implicit w: Writes[T]): OWrites[T] = Writes.at[T](this)(w)

  /**
   * Writes a Option[T] at given JsPath
   * If None => doesn't write the field (never writes null actually)
   * else => writes the field using implicit Writes[T]
   */
  def writeNullable[T](implicit w: Writes[T]): OWrites[Option[T]] = Writes.nullable[T](this)(w)

  /**
   * Writes a T at JsPath using the explicit Writes[T] passed by name which is useful in case of
   * recursive case classes for ex
   *
   * {{{
   * case class User(id: Long, name: String, friend: User)
   *
   * implicit lazy val UserReads: Reads[User] = (
   *   (__ \ 'id).write[Long] and
   *   (__ \ 'name).write[String] and
   *   (__ \ 'friend).lazyWrite(UserReads)
   * )(User.apply _)
   * }}}
   */
  def lazyWrite[T](w: => Writes[T]): OWrites[T] = OWrites((t: T) => Writes.at[T](this)(w).writes(t))

  /**
   * Writes a Option[T] at JsPath using the explicit Writes[T] passed by name which is useful in case of
   * recursive case classes for ex
   *
   * Please note that it's not writeOpt to be coherent with readNullable
   *
   * {{{
   * case class User(id: Long, name: String, friend: Option[User])
   *
   * implicit lazy val UserReads: Reads[User] = (
   *   (__ \ 'id).write[Long] and
   *   (__ \ 'name).write[String] and
   *   (__ \ 'friend).lazyWriteNullable(UserReads)
   * )(User.apply _)
   * }}}
   */
  def lazyWriteNullable[T](w: => Writes[T]): OWrites[Option[T]] = OWrites((t: Option[T]) => Writes.nullable[T](this)(w).writes(t))

  /** Writes a pure value at given JsPath */
  def write[T](t: T)(implicit w: Writes[T]): OWrites[JsValue] = Writes.pure(this, t)

  /** Reads/Writes a T at JsPath using provided implicit Format[T] */
  def format[T](implicit f: Format[T]): OFormat[T] = Format.at[T](this)(f)
  /** Reads/Writes a T at JsPath using provided explicit Reads[T] and implicit Writes[T]*/
  def format[T](r: Reads[T])(implicit w: Writes[T]): OFormat[T] = Format.at[T](this)(Format(r, w))
  /** Reads/Writes a T at JsPath using provided explicit Writes[T] and implicit Reads[T]*/
  def format[T](w: Writes[T])(implicit r: Reads[T]): OFormat[T] = Format.at[T](this)(Format(r, w))

  /**
   * Reads/Writes a T at JsPath using provided implicit Reads[T] and Writes[T]
   *
   * Please note we couldn't call it "format" to prevent conflicts
   */
  def rw[T](implicit r: Reads[T], w: Writes[T]): OFormat[T] = Format.at[T](this)(Format(r, w))

  /**
   * Reads/Writes a Option[T] (optional or nullable field) at given JsPath
   *
   * @see JsPath.readNullable to see behavior in reads
   * @see JsPath.writeNullable to see behavior in writes
   */
  def formatNullable[T](implicit f: Format[T]): OFormat[Option[T]] = Format.nullable[T](this)(f)

  /**
   * Lazy Reads/Writes a T at given JsPath using implicit Format[T]
   * (useful in case of recursive case classes).
   *
   * @see JsPath.lazyReadNullable to see behavior in reads
   * @see JsPath.lazyWriteNullable to see behavior in writes
   */
  def lazyFormat[T](f: => Format[T]): OFormat[T] = OFormat[T](lazyRead(f), lazyWrite(f))

  /**
   * Lazy Reads/Writes a Option[T] (optional or nullable field) at given JsPath using implicit Format[T]
   * (useful in case of recursive case classes).
   *
   * @see JsPath.lazyReadNullable to see behavior in reads
   * @see JsPath.lazyWriteNullable to see behavior in writes
   */
  def lazyFormatNullable[T](f: => Format[T]): OFormat[Option[T]] = OFormat[Option[T]](lazyReadNullable(f), lazyWriteNullable(f))

  /**
   * Lazy Reads/Writes a T at given JsPath using explicit Reads[T] and Writes[T]
   * (useful in case of recursive case classes).
   *
   * @see JsPath.lazyReadNullable to see behavior in reads
   * @see JsPath.lazyWriteNullable to see behavior in writes
   */
  def lazyFormat[T](r: => Reads[T], w: => Writes[T]): OFormat[T] = OFormat[T](lazyRead(r), lazyWrite(w))

  /**
   * Lazy Reads/Writes a Option[T] (optional or nullable field) at given JsPath using explicit Reads[T] and Writes[T]
   * (useful in case of recursive case classes).
   *
   * @see JsPath.lazyReadNullable to see behavior in reads
   * @see JsPath.lazyWriteNullable to see behavior in writes
   */
  def lazyFormatNullable[T](r: => Reads[T], w: => Writes[T]): OFormat[Option[T]] = OFormat[Option[T]](lazyReadNullable(r), lazyWriteNullable(w))

  private val self = this

  object json {
    /**
     * (__ \ 'key).json.pick[A <: JsValue] is a Reads[A] that:
     * - picks the given value at the given JsPath (WITHOUT THE PATH) from the input JS
     * - validates this element as an object of type A (inheriting JsValue)
     * - returns a JsResult[A]
     *
     * Useful to pick a typed JsValue at a given JsPath
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> 123)
     * js.validate((__ \ 'key2).json.pick[JsNumber])
     * => JsSuccess(JsNumber(123),/key2)
     * }}}
     */
    def pick[A <: JsValue](implicit r: Reads[A]): Reads[A] = Reads.jsPick(self)

    /**
     * (__ \ 'key).json.pick is a Reads[JsValue] that:
     * - picks the given value at the given JsPath (WITHOUT THE PATH) from the input JS
     * - validates this element as an object of type JsValue
     * - returns a JsResult[JsValue]
     *
     * Useful to pick a JsValue at a given JsPath
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> "value2")
     * js.validate((__ \ 'key2).json.pick)
     * => JsSuccess("value2",/key2)
     * }}}
     */
    def pick: Reads[JsValue] = pick[JsValue]

    /**
     * (__ \ 'key).json.pickBranch[A <: JsValue](readsOfA) is a Reads[JsObject] that:
     * - copies the given branch (JsPath + relative JsValue) from the input JS at this given JsPath
     * - validates this relative JsValue as an object of type A (inheriting JsValue) potentially modifying it
     * - creates a JsObject from JsPath and validated JsValue
     * - returns a JsResult[JsObject]
     *
     * Useful to create/validate an JsObject from a single JsPath (potentially modifying it)
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> Json.obj( "key21" -> "value2") )
     * js.validate( (__ \ 'key2).json.pickBranch[JsString]( (__ \ 'key21).json.pick[JsString].map( (js: JsString) => JsString(js.value ++ "3456") ) ) )
     * => JsSuccess({"key2":"value23456"},/key2/key21)
     * }}}
     */
    def pickBranch[A <: JsValue](reads: Reads[A]): Reads[JsObject] = Reads.jsPickBranch[A](self)(reads)

    /**
     * (__ \ 'key).json.pickBranch is a Reads[JsObject] that:
     * - copies the given branch (JsPath + relative JsValue) from the input JS at this given JsPath
     * - creates a JsObject from JsPath and JsValue
     * - returns a JsResult[JsObject]
     *
     * Useful to create/validate an JsObject from a single JsPath (potentially modifying it)
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> Json.obj( "key21" -> "value2") )
     * js.validate( (__ \ 'key2).json.pickBranch )
     * => JsSuccess({"key2":{"key21":"value2"}},/key2)
     * }}}
     */
    def pickBranch: Reads[JsObject] = Reads.jsPickBranch[JsValue](self)

    /**
     * (__ \ 'key).put(fixedValue) is a Reads[JsObject] that:
     * - creates a JsObject setting A (inheriting JsValue) at given JsPath
     * - returns a JsResult[JsObject]
     *
     * This Reads doesn't care about the input JS and is mainly used to set a fixed at a given JsPath
     * Please that A is passed by name allowing to use an expression reevaluated at each time.
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> "value2")
     * js.validate( (__ \ 'key3).json.put( { JsNumber((new java.util.Date).getTime()) } ) )
     * => JsSuccess({"key3":1376419773171},)
     * }}}
     */
    def put(a: => JsValue): Reads[JsObject] = Reads.jsPut(self, a)

    /**
     * (__ \ 'key).json.copyFrom(reads) is a Reads[JsObject] that:
     * - copies a JsValue using passed Reads[A]
     * - creates a new branch from JsPath and copies previous value into it
     *
     * Useful to copy a value from a Json branch into another branch
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> "value2")
     * js.validate( (__ \ 'key3).json.copyFrom((__ \ 'key2).json.pick))
     * => JsSuccess({"key3":"value2"},/key2)
     * }}}
     */
    def copyFrom[A <: JsValue](reads: Reads[A]): Reads[JsObject] = Reads.jsCopyTo(self)(reads)

    /**
     * (__ \ 'key).json.update(reads) is the most complex Reads[JsObject] but the most powerful:
     * - copies the whole JsValue => A
     * - applies the passed Reads[A] on JsValue => B
     * - deep merges both JsValues (A ++ B) so B overwrites A identical branches
     *
     * Please note that if you have prune a branch in B, it is still in A so you'll see it in the result
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> "value2")
     * js.validate(__.json.update((__ \ 'key3).json.put(JsString("value3"))))
     * => JsSuccess({"key1":"value1","key2":"value2","key3":"value3"},)
     * }}}
     */
    def update[A <: JsValue](reads: Reads[A]): Reads[JsObject] = Reads.jsUpdate(self)(reads)

    /**
     * (__ \ 'key).json.prune is Reads[JsObject] that prunes the branch and returns remaining JsValue
     *
     * Example :
     * {{{
     * val js = Json.obj("key1" -> "value1", "key2" -> "value2")
     * js.validate( (__ \ 'key2).json.prune )
     * => JsSuccess({"key1":"value1"},/key2)
     * }}}
     */
    def prune: Reads[JsObject] = Reads.jsPrune(self)
  }

}
