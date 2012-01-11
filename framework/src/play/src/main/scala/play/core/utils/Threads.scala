package play.utils

object Threads {

  def withContextClassLoader[T](classloader: ClassLoader)(b: => T): T = {
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader
    thread.setContextClassLoader(classloader)
    val result = b
    thread.setContextClassLoader(oldLoader)
    result
  }

}