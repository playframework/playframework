/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

object JFile {
  def unapply(value: Any): Option[java.io.File] = {
    Option(value).filter(_.isInstanceOf[java.io.File]).map(_.asInstanceOf[java.io.File])
  }
}

object VirtualFile {
  def unapply(value: Any): Option[Any] = {
    Option(value).filter { vf =>
      val name = vf.getClass.getSimpleName
      (name == "BasicVirtualFileRef" || name == "MappedVirtualFile")
    }
  }
}
