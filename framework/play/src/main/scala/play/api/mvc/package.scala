package play.api

package object mvc {
    
    implicit def request[A](implicit ctx:Context[A]):Request[A] = ctx.request

}
