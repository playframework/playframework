package play.api.libs.json

import play.api.data.validation.ValidationError

sealed trait PathNode {
  def apply(json: JsValue): List[JsValue]
  def toJsonString
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

}

case class IdxPathNode(idx: Int) extends PathNode {
  def apply(json: JsValue): List[JsValue] = json match {
    case arr: JsArray => List(arr(idx))
    case _ => List()
  }

  override def toString = "(%d)".format(idx)
  def toJsonString = "[%d]".format(idx)
}

object JsPath {
  def \(child: String) = JsPath() \ child
  def \(child: Symbol) = JsPath() \ child

  def \\(child: String) = JsPath() \\ child
  def \\(child: Symbol) = JsPath() \\ child

  def apply(idx: Int): JsPath = JsPath()(idx)
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
}
