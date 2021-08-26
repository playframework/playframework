/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

object JFile {
  class FileOption(val any: Any) extends AnyVal {
    def isEmpty: Boolean  = !any.isInstanceOf[java.io.File]
    def get: java.io.File = any.asInstanceOf[java.io.File]
  }
  def unapply(any: Any): FileOption = new FileOption(any)
}

object VirtualFile {
  def unapply(value: Any): Option[Any] = {
    Option(value).filter { vf =>
      val name = vf.getClass.getSimpleName
      (name == "BasicVirtualFileRef" || name == "MappedVirtualFile")
    }
  }
}
