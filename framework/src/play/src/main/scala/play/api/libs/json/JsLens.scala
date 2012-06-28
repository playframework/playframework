package play.api.libs.json

/**
 * Lenses
 */

trait Lens[A,B]{
  self =>

  def get: A => B
  def set: (A,B) => A

  def mod(a:A, f: B => B) : A = set(a, f(get(a)))

  def compose[C,L <: Lens[C,B]](that: Lens[C,A])(implicit cons:LensConstructor[C,B,L]):L = cons(
    c => get(that.get(c)),
    (c, b) => that.mod(c, x => set(x, b))
  )

  def andThen[C,L <: Lens[A,C]](o: Lens[B,C])(implicit cons:LensConstructor[A,C,L]) = o.compose(self)(cons)
}

trait LensConstructor[A,B,L <: Lens[A,B]] extends (( A => B, (A,B) => A) => L)

trait PriorityOne {
  implicit def lens[A,B,L <: Lens[A,B]] = new LensConstructor[A,B,Lens[A,B]] {

    def apply(get: A => B,
              set: (A, B) => A) = Lens(get,set)

  }
}

object LensConstructor extends PriorityOne {

  implicit val jsValueLens = new LensConstructor[JsValue,JsValue,JsLens] {

    def apply(get: JsValue => JsValue,
              set: (JsValue, JsValue) => JsValue) = JsLens(get,set, None)

  }
}

object Lens {
  def apply[A,B](getter: A => B,setter: (A, B) => A) = new Lens[A,B] {
    def get = getter
    def set = setter
  }
}

case class SeqJsLens(
  getter: JsValue => Seq[JsLens],
  setter: (JsValue, Seq[JsLens]) => JsValue)
  extends Lens[JsValue, Seq[JsLens]] {

  def get = getter
  def set = setter

  /**
   * Apply Lens and reapply item to every generated lenses
   */
  def apply(whole: JsValue): Seq[JsValue] = this.map(whole)(_ get whole)

  /**
   * Apply Lens and prune to all generated JsLens
   */
  def prune(whole: JsValue): JsValue = this.foldLeft(whole){
    (acc, el) => el prune acc
  }

  /**
   * Apply Lens and set element to all generated JsLens
   */
  def set(whole: JsValue, replace: JsValue) = this.foldLeft(whole) {
    (acc, el) => el set (acc, replace)
  }

  /**
   * Apply and map the callback
   */
  def map(whole: JsValue)(callback: JsLens => JsValue): Seq[JsValue] = 
    get(whole).map(callback)

  /**
   * Apply and fold the callback
   */
  def foldLeft(whole: JsValue)(callback: (JsValue, JsLens) => JsValue) =
    get(whole).foldLeft(whole){
      (acc, el) => callback(acc, el)
    }
}

case class JsLens(getter: JsValue => JsValue,
                  setter: (JsValue, JsValue) => JsValue,
                  maybeLens: Option[JsLens]) extends Lens[JsValue,JsValue]{
  /**
   * Aliases
   */
  def get = getter
  def set = setter

  /**
   * Helper methods
   */
  def apply(whole: JsValue) = get(whole)
  def apply(whole: JsValue, repl: JsValue) = set(whole, repl)

  /**
   * Prune items from object
   */
  def prune(whole: JsValue): JsValue = {
    maybeLens.map{
      case parentLens => 
        // We will be looking for the item to delete
        // We'll then search in parent's content for the item to delete, 
        // if we found a match (reference match) then we'll filter it

        // Get back the item to delete
        val todelete = get(whole)

        // We'll rewrite parent's content with original content filtered from
        // item to delete
        parentLens.set(whole, parentLens.get(whole) match {
          // Quick tip: == is not eq !
          //   * == makes a deep match
          //   * eq compares object references
          case JsObject(elements) => JsObject(elements.filterNot(_._2 eq todelete))
          case JsArray(elements)  => JsArray(elements.filterNot(_ eq todelete))
          case e => e
        })
      }.getOrElse(whole)
  }

  def \\(f: String): SeqJsLens = {
    // Helper methods for getting multiple lenses describing requirements
    def recursiveFind(whole: JsValue, key: String): List[JsLens] = {
      whole match {
        case JsArray(elements) => {
          elements.foldLeft(List[JsLens]()){
            (acc, e) => acc ++ recursiveFind(e, key)
          }.zipWithIndex.map{
            // Rebase all children lenses upon this one
            case (sublens, i) => JsLens at i andThen sublens
          }
        }
        case JsObject(elements) => {
          elements.foldLeft(List[JsLens]()){
            (acc, e) => {
              val (k, v) = e
              val lensprefix = JsLens \ key
              acc ++ recursiveFind(v, key).map(lensprefix andThen _) ++ (
                if (k == key) Some(lensprefix) else None
              ).toList
            }
          }
        }
        case _ => Nil
      }
    }

    SeqJsLens(
      (a => recursiveFind(get(a), f).toSeq.map{
          // Once we have lens we will need to rebase those lenses on the
          // current one
          case e => this andThen e
        }),
      // This is an identity setter
      (a,_) => a
    )
  }

  def \(f: String) = JsLens(
    JsLens.objectGetter(get)(f),
    JsLens.objectSetter(get)(set)(f),
    Some(this))

  def at(i: Int) = JsLens(
    JsLens.arrayGetter(get)(i),
    JsLens.arraySetter(get)(set)(i),
    Some(this))

  // TODO: Rewrite this method with SeqJsLens
  def \\(whole: JsValue,
         selector: JsValue => Boolean,
         cb: JsValue => JsValue): JsValue = {
    // Get back elements
    val elements = JsLens.selectAll(get(whole), selector).map{
      t => ((this andThen t._1) -> t._2)
    }

    elements.foldLeft(whole)((acc, t) => {
      val lens = t._1
      val previous = t._2

      lens.set(acc, cb(previous))
      })
  }

  /**
   * Composition methods
   */

  /**
   * Compose a lens with an other one
   * You will change the parent lens with the one specified in that
   */
  def compose(that: JsLens): JsLens = JsLens(
    c => get(that.get(c)),
    (c, b) => that.set(c, set(that.get(c), b)),
    // Let's rebuild the parent lens
    Some(this.maybeLens match {
      // If parent lens is self, then put that as parent lens
      case Some(lens) if lens eq JsLens.self => that
      // If parent lens is identity, then put that as parent lens
      case Some(lens) if lens eq JsLens.identity => that
      // If not self nor identity then put the current parent
      case Some(lens) => lens compose that
      // If None then put that too
      case None => that
    })
  )

  /**
   * Add a lens following this one
   */
  def andThen(o: JsLens) = o.compose(this)


  /**
   * Transformation methods
   */

  /**
   * Get a custom lens from JsValue to a template
   */
  def as[A](implicit format:Format[A]):Lens[JsValue,A] = Lens[JsValue,A](
    getter = jsValue => get(jsValue).as[A], 
    setter = (me, value) => set(me, format.writes(value)) )

  /**
   * Get a lens from JsValue to an either
   */
  def asEither[A](implicit format:Format[A]): Lens[JsValue,Either[String, A]] = Lens[JsValue,Either[String, A]](
    getter = jsValue => get(jsValue) match {
      // TODO: JsUndefined is not a left
      case JsUndefined(e) => Left(e)
      case e => Right(e.as[A])
    },
    setter = (me, value) => value match {
      case Left(_) => me
      case Right(v) => set(me, format.writes(v))
    })

  //TODO: rewrite without asEither
  def asOpt[A](implicit format:Format[A]): Lens[JsValue,Option[A]] =
    this.asEither andThen Lens[Either[String,A],Option[A]](
      getter = jsValue => jsValue match {
        case Left(_) => None
        case Right(v) => Some(v)
      },
      setter = (me, value) => value match {
        case None => me
        case Some(v) => me match {
          case l if l.isLeft => l
          case Right(_) => Right[String,A](v)
        }
      })

}

object JsLens {
  val identity = JsLens(a => a, (a, _) => a, None)
  val self = JsLens(a => a, (_, a) => a, Some(JsLens.identity))

  def \(f: String): JsLens = JsLens.self \ f

  def at(i: Int): JsLens = JsLens.self at i

  private[JsLens] def objectGetter
    (get: JsValue => JsValue)
    (f: String): (JsValue => JsValue) = (a => get(a) match {
        case JsObject(fields) => 
          fields
            .find(t => t._1 == f)
            .getOrElse(("undefined" -> JsUndefined("Field " + f + " not found")))
            ._2
        case _ => JsUndefined("Element is not an object, couldn't find field "+f)
      })

  private[JsLens] def arrayGetter
    (get: JsValue => JsValue)
    (i: Int): (JsValue => JsValue) = (a => {
      get(a) match {
        case JsArray(fields) => fields
          .lift(i)
          .getOrElse(JsUndefined("Index " + i + " not found"))
        case _ => JsUndefined("Element is not an array, couldn't find index " + i)
      }
    })

  private[JsLens] def objectSetter
    (get: JsValue => JsValue)
    (set: (JsValue, JsValue) => JsValue)
    (f: String): ((JsValue, JsValue) => JsValue) = (whole, repl) => {
      get(whole) match {
        case JsObject(fields) => {
          val found = fields.find(t => t._1 == f) match {
            case None => false
            case _ => true
          }
          set(whole, JsObject(found match {
            case false => fields :+ (f -> repl)
            case true => fields.map(
              _ match {
                case (k: String, v: JsValue) => if(k == f){
                  k -> repl
                }else{
                  k -> v
                }
              }
            )
          }))
        }
        case _ => {
          set(whole, JsObject(Seq(f -> repl)))
        }
      }
    }

  private[JsLens] def arraySetter
    (get: JsValue => JsValue)
    (set: (JsValue, JsValue) => JsValue)
    (i: Int): ((JsValue, JsValue) => JsValue) = (whole, repl) => {
      get(whole) match {
        case JsArray(fields) => {
          val found = fields.lift(i) match {
            case None => false
            case _ => true
          }
          set(whole, JsArray(found match {
            case false => fields.padTo(i, JsNull).patch(i, Seq(repl), 1)
            //case false => fields :+ repl
            case true => fields.patch(i, Seq(repl), 1)
          }))
        }
        case _ => {
          set(whole, JsArray(Seq().padTo(i,JsNull).patch(i,Seq(repl),1)))
          //set(whole, JsArray(Seq(repl)))
        }
      }
    }

  def selectAll(whole: JsValue, selector: JsValue => Boolean, depth: Int = Int.MaxValue): Seq[(JsLens,
  JsValue)] = {
    val baseLens = JsLens.self

    val elements = if(selector(whole)){
      Seq(baseLens -> whole)
    } else {
      Nil
    }

    depth match {
      case v if v <= 0 => elements
      case _ => elements ++ (whole match {
        case JsObject(fields) => {
          fields.flatMap{ tuple => {
              val lens = baseLens \ tuple._1

              JsLens.selectAll(tuple._2, selector, depth - 1).map{
                t => ((lens andThen t._1) -> t._2)
              }
            }
          }
        }
        case JsArray(fields) => {
          val baseLens = JsLens.self
          fields.zipWithIndex.flatMap{ 
            tuple => {
              val lens = baseLens at tuple._2

              JsLens.selectAll(tuple._1, selector, depth - 1).map{
                t => ((lens andThen t._1) -> t._2)
              }
            }
          }
        }
        case _ => Nil
      })
    }
  }

}

