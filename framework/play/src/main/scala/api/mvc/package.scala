package play.api

package object mvc {
    
    implicit def request(implicit ctx:Context) = ctx.request
    implicit def application(implicit ctx:Context) = ctx.application


}
