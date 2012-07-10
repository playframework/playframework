package play.api.libs.json

import play.api.data.validation.ValidationError

sealed trait PathNode {
  def apply(json: JsValue): List[JsValue]

  def recursive: Boolean = false
  def splitChildren(json: JsValue): List[Either[(PathNode, JsValue), (PathNode, JsValue)]]
  def set(json: JsValue, transform: JsValue => JsValue): JsValue
  def setIfDef(json: JsValue, transform: JsValue => JsValue): JsValue
  def prune(json: JsValue): JsValue

  def toJsonField(value: JsValue): JsValue

  def toJsonString: String
}
 
case class KeyPathNode(key: String, override val recursive: Boolean = false) extends PathNode{

  def apply(json: JsValue): List[JsValue] = json match {
    case obj: JsObject => (if(recursive) (json \\ key).toList else List(json \ key)).filterNot{ 
      case JsUndefined(_) => true 
      case _ => false
    }
    case _ => List()
  }

  override def toString = (if(recursive) "//" else "/") + key
  def toJsonString = (if(recursive) "*." else ".") + key

  def splitChildren(json: JsValue) = json match {
    case obj: JsObject => obj.fields.toList.map{ case (k,v) => 
      if(k == this.key) Right(this -> v)
      else Left(KeyPathNode(k) -> v)
    }
    case arr: JsArray => if(recursive) {
      arr.value.toList.zipWithIndex.map{ case (js, j) => Left(IdxPathNode(j) -> js) }
    } else List()
    case _ => List()
  } 

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

  def setIfDef(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case js @ JsUndefined(_) => json
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

  def prune(json: JsValue): JsValue = json match {
    case obj: JsObject => 
      JsObject(obj.fields.collect{ case (k,v) if(k != this.key) => k -> v })
    case _ => json
  } 

  def toJsonField(value: JsValue) = Json.obj(key -> value)

}

case class IdxPathNode(idx: Int) extends PathNode {
  def apply(json: JsValue): List[JsValue] = json match {
    case arr: JsArray => List(arr(idx))
    case _ => List()
  }

  override def toString = "(%d)".format(idx)
  def toJsonString = "[%d]".format(idx)

  def splitChildren(json: JsValue) = json match {
    case arr: JsArray => arr.value.toList.zipWithIndex.map{ case (js, j) => 
      if(j == idx) Right(this -> js) 
      else Left(IdxPathNode(j) -> js)
    }
    case _ => List()
  } 

  def set(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case arr: JsArray => JsArray(arr.value.zipWithIndex.map{ case (js, j) => if(j == idx) transform(js) else js})
    case _ => transform(json)
  }

  def setIfDef(json: JsValue, transform: JsValue => JsValue): JsValue = json match {
    case js @ JsUndefined(_) => json
    case arr: JsArray => JsArray(arr.value.zipWithIndex.map{ case (js, j) => if(j == idx) transform(js) else js})
    case _ => transform(json)
  }

  def prune(json: JsValue): JsValue = json match {
    case arr: JsArray => JsArray(arr.value.zipWithIndex.collect{ case (js, j) if(j != idx) => js })
    case _ => json
  }

  def toJsonField(value: JsValue) = value

}

object JsPath {
  def \(child: String) = JsPath() \ child
  def \(child: Symbol) = JsPath() \ child

  def \\(child: String) = JsPath() \\ child
  def \\(child: Symbol) = JsPath() \\ child

  def set(origin: JsValue, newelt: JsValue) = JsPath().set(origin, newelt)

  def apply(idx: Int): JsPath = JsPath()(idx)
}

case class JsPath(path: List[PathNode] = List()) {
  def \(child: String) = JsPath(path :+ KeyPathNode(child))
  def \(child: Symbol) = JsPath(path :+ KeyPathNode(child.name))

  def \\(child: String) = JsPath(path :+ KeyPathNode(child, true))
  def \\(child: Symbol) = JsPath(path :+ KeyPathNode(child.name, true))

  def apply(idx: Int): JsPath = JsPath(path :+ IdxPathNode(idx))

  def apply(json: JsValue): List[JsValue] = {
    def step(path: List[PathNode], json: JsValue): List[JsValue] = {
      path match {
        case Nil => List(json)
        case head :: tail => 
          head(json).map { 
            case JsUndefined(_) => List()
            case js => step(tail, js)
          }.flatten
      }
    }
    step(path, json)
  }

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

  override def toString = path.foldLeft("")((acc, p) => acc + p.toString)
  def toJsonString = path.foldLeft("obj")((acc, p) => acc + p.toJsonString)

  def compose(other: JsPath) = JsPath(path ++ other.path)
  def ++(other: JsPath) = this compose other

  def set(origin: JsValue, newElt: JsValue): JsValue = set(origin, js => newElt)
  def setIfDef(origin: JsValue, newElt: JsValue): JsValue = newElt match {
    case js @ JsUndefined(_) => origin
    case _ => set(origin, newElt)
  }

  def set(origin: JsValue, transform: JsValue => JsValue): JsValue = {
    def buildLevel(json: JsValue, currentPath: PathNode)(mapF: Either[(PathNode, JsValue), (PathNode, JsValue)] => (PathNode, JsValue)): JsValue = {
      val nodes = currentPath.splitChildren(json).map( mapF )

      json match {
        case obj: JsObject => nodes.foldLeft(Json.obj()){ (acc, nodejs) => (acc ++ nodejs._1.toJsonField(nodejs._2)).as[JsObject] } 
        case arr: JsArray => nodes.foldLeft(Json.arr()){ (acc, nodejs) => (acc :+ nodejs._1.toJsonField(nodejs._2)).as[JsArray] }
        case _ => json
      }
    }

    def step(path: List[PathNode], json: JsValue): JsValue = path match {
      case Nil => transform(json)
      case List(pathnode) => 
        if(pathnode.recursive) {
          buildLevel(json, pathnode){
            case Left((locpath, js)) => locpath -> step(path, js)
            case Right((locpath, js)) => locpath -> transform(js)
          }
        } else pathnode.set(json, transform)
      case pathnode :: tail => 
        if(pathnode.recursive) {
          buildLevel(json, pathnode){
            case Left((locpath, js)) => locpath -> step(path, js)
            case Right((locpath, js)) => locpath -> step(tail, js)
          }
        } else {
          pathnode(json) match {
            case Nil => json
            case List(js) => pathnode.set(json, js => step(tail, js)) 
            case js :: jstail => pathnode.set(js, js => step(tail, js)) // shall not happen 
          }
        }
    }

    step(path, origin)
  }

  def prune(origin: JsValue): JsValue = {
    def buildLevel(json: JsValue, currentPath: PathNode)(mapF: PartialFunction[Either[(PathNode, JsValue), (PathNode, JsValue)], (PathNode, JsValue)]): JsValue = {
      val nodes = currentPath.splitChildren(json).collect( mapF )

      json match {
        case obj: JsObject => nodes.foldLeft(Json.obj()){ (acc, nodejs) => (acc ++ nodejs._1.toJsonField(nodejs._2)).as[JsObject] } 
        case arr: JsArray => nodes.foldLeft(Json.arr()){ (acc, nodejs) => (acc :+ nodejs._1.toJsonField(nodejs._2)).as[JsArray] }
        case _ => json
      }
    }

    def step(path: List[PathNode], json: JsValue): JsValue = path match {
      case Nil => JsNull
      case List(pathnode) => 
        if(pathnode.recursive) {
          buildLevel(json, pathnode){
            case Left((locpath, js)) => locpath -> step(path, js)
            // do not keep right as they correspond to the nodes I want to prune
          }
        } else {
          pathnode.prune(json)
        }
      case pathnode :: tail => 
        if(pathnode.recursive) {
          buildLevel(json, pathnode){
            case Left((locpath, js)) => locpath -> step(path, js)
            case Right((locpath, js)) => locpath -> step(tail, js)
          }
        } else {
          pathnode(json) match {
            case Nil => json
            case List(js) => pathnode.set(json, js => step(tail, js)) 
            case js :: jstail => pathnode.set(js, js => step(tail, js))
          }
        }
    }
    step(path, origin)
  }

}
