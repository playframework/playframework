package play.api.libs.json

case class AccessorStack(stack: List[Accessor]){
  // List(ObjectAccessor("foo"), ObjectAccessor("bar")) should output:
  // \ "foo" \ "bar"
  override def toString = stack.foldLeft("")((i,e) => i + e.toString + " ")

  /**
   * Look into an object
   */
  def \(name: String): AccessorStack = AccessorStack(stack :+ ObjectAccessor(name))

  /**
   * Look into an array
   */
  def *(index: Int):   AccessorStack = AccessorStack(stack :+ ArrayAccessor(index))
}

/**
 * Generic accessor
 */
trait Accessor {
  def \(name: String): AccessorStack = AccessorStack(List(this, ObjectAccessor(name)))
  def *(index: Int):   AccessorStack = AccessorStack(List(this, ArrayAccessor(index)))

  override def toString = "Undefined, please override"
}


/**
 * Accessor for looking inside objects
 */
case class ObjectAccessor(name: String) extends Accessor {
  override def toString = "\\ \"" + name + "\""
}


/**
 * Accessor for looking inside arrays
 */
case class ArrayAccessor(index: Int) extends Accessor {
  override def toString = "* "+ index.toString
}


// vim: set ts=2 sw=2 ft=scala et:
