package play.api.libs.json

import play.api.data.validation.ValidationError

sealed trait PathNode {
  def apply(json: JsValue): List[JsValue]

  def recursive: Boolean = false
  def splitChildren(json: JsValue): List[Either[(PathNode, JsValue), (PathNode, JsValue)]]
  def set(json: JsValue, transform: JsValue => JsValue): JsValue
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
    case Nil => JsError(json, this -> Seq(ValidationError("validation.error.missing-path")))
    case List(js) => JsSuccess(js)
    case head :: tail => JsError(json, this -> Seq(ValidationError("validation.error.multiple-result-path")))
  }

  override def toString = path.foldLeft("")((acc, p) => acc + p.toString)
  def toJsonString = path.foldLeft("obj")((acc, p) => acc + p.toJsonString)

  def compose(other: JsPath) = JsPath(path ++ other.path)
  def ++(other: JsPath) = this compose other

  def set(origin: JsValue, newElt: JsValue): JsValue = set(origin, js => newElt)

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
}

/*sealed trait JsPath {
  def parent: Option[JsPath] = Some(ROOT)
  def key: String
  def recursive: Boolean = false
  def idx: Option[Int] = None

  def \(child: String) = new \(Some(this), child)
  def \(child: Symbol) = new \(Some(this), child.name)

  def \\(child: String) = new \\(Some(this), child)
  def \\(child: Symbol) = new \\(Some(this), child.name)

  private def applyIdx(seq: Seq[JsValue]) = {
    idx.map{ i => 
      seq.collect{ case arr: JsArray => arr(i) }
        .filterNot( _ match { 
          case JsUndefined(_) => true
          case _ => false 
        })
    }.getOrElse(seq)
  }

  def apply(json: JsValue): Seq[JsValue] = applyIdx(
    (parent match {
      case Some(ROOT) => 
        if(recursive) {
          json \\ key
        } else {
          Seq(json \ key)
        }
          
      case Some(p) => 
        p.apply(json).map{ js =>
          if(recursive) js \\ key
          else Seq(js \ key)
        }.flatten

      case None => Seq(json)
    }).filterNot(_ match { 
      case JsUndefined(_) => true
      case _ => false 
    })
  )

  def apply(idx: Int): JsPath = IDX(parent, key, recursive, Some(idx))

  def set(origin: JsValue, newelt: JsValue): JsValue = this.set(origin, js => newelt)

  def set(origin: JsValue, transform: JsValue => JsValue): JsValue = {
    (parent match {
      case Some(ROOT) => 
        modify(origin, transform)
          
      case Some(p) => 
        if(recursive) {
          Json.obj()
        }else {
          val parent = p.set(origin, js => js)

          modify(parent, transform)
        }

      case None => transform(origin)
    })
  }

  def modify(json: JsValue, transform: JsValue => JsValue) = {
    if(recursive) {
      json match {
        case obj: JsObject =>
          var found = false;
          val o = JsObject(obj.fields.map{ case (k,v) => if(k==key) {found=true; k -> transform(v)} else k -> this.set(v, transform(v)) })
          if(!found) o ++ Json.obj(key -> transform(Json.obj()))
          else o
        case arr: JsArray =>
          JsArray(arr.value.map{ this.set(_, transform(_)) })
        case _ => JsUndefined("Element is not an object, couldn't find field "+key)
      }
    } else {
      json match {
        case obj: JsObject =>
          var found = false
          val o = JsObject(obj.fields.map{ case (k,v) => if(k==key) { found = true; k -> transform(v) } else k -> transform(v) })
          if(!found) o ++ Json.obj(key -> transform(Json.obj()))
          else o
        case _ => JsUndefined("Element is not an object, couldn't find field "+key)
      }
    }
  }


  /*lazy val toLens: JsLens = {
    val pLens = parent match {
      case Some(ROOT) => 
        if(recursive) {
          JsLens \\ key
        } else {
          JsLens \ key
        }
          
      case Some(p) => 
        p.toLens andThen (
          if(recursive) JsLens \\ key
          else JsLens \ key
        )

      case None => JsLens.self
    }

    idx.map( pLens.at(_) )

    pLens
  }*/
}

case class \(override val parent: Option[JsPath] = Some(ROOT), override val key: String) extends JsPath {
  override def toString = parent.map(_.toString).get + "/" + key
}

case class \\(override val parent: Option[JsPath] = Some(ROOT), override val key: String) extends JsPath {
  override val recursive = true

  override def toString = parent.map(_.toString).get + "//" + key
}

case class IDX(override val parent: Option[JsPath] = Some(ROOT), override val key: String, 
               override val recursive: Boolean, override val idx: Option[Int] = None) extends JsPath {

  override def toString = idx.map("%s(%d)".format(super.toString, _)).getOrElse("%s(%d)".format(super.toString))
}

case object ROOT extends JsPath {
  override val parent = None
  override val key = "__ROOT__"

  override def toString = "ROOT"

}*/