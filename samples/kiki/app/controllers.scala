package controllers

import play.api.mvc._
import play.api.mvc.Results._

object Actions {
    
    def Secured(predicate:Context=>Boolean)(action:Action):Action = new Action {
        
        def apply(ctx:Context) = {
            if(predicate(ctx)) {
                action(ctx)
            } else {
                Forbidden
            }
        }
        
    }
    
    def Secured(predicate: =>Boolean)(action:Action):Action = Secured(_ => predicate)(action)
    
    val cache = scala.collection.mutable.HashMap.empty[String,Result] 
    
    def Cached(args: Any*)(action:Action) = new Action {
        
        val key = args.mkString
        
        def apply(ctx:Context) = {
            cache.get(key).getOrElse {
                val r = action(ctx)
                cache.put(key, r)
                r
            }
        }
        
    }
    
}

import Actions._

object Blocking extends Controller {

    val waited = play.core.Iteratee.Promise[Int]()

    def unblockEveryone(status:Int) = Action {
        waited.redeem(status) 
        Ok
    }

    def waitForUnblock = Action {
        AsyncResult(waited.map{ status => println("status"); EmptyStatus(status)})

    }

}

object TOTO {
    
    lazy val a = System.currentTimeMillis
    
}

object Application extends Controller {
    
    override def Action(block:Context => Result) = new Action {
        def apply(ctx:Context) = {
            println("Request for Application controller")
            block(ctx)
        }
    }
    
    def coco = Action {
        NotFound("oops")
    }
    
    import play.core.Iteratee._
    
    def index = Action {
        Ok(views.html.index("World " + TOTO.a))
    }
    
    def websocketTest = Action {
        Ok(views.html.sockets())
    }
    
    def moreSockets = Action {
        Ok(views.html.moreSockets())
    }
    
    def socketEchoReversed = Action {
        SocketResult[String]{ (in,out) => 
            out <<: in.map {
                case El("") => EOF
                case o => o.map(_.reverse)
            }
        }
    }
    
    val specialTemplates = Map(
        "home"  -> views.pages.html.home.f,
        "about" -> views.pages.html.about.f
    )
    
    def page(name:String) = Action {
        Ok(specialTemplates.get(name).getOrElse(views.pages.html.page.f)(name, "Dummy content"))
    }
    
    def list(page:Int, sort:String) = {
        
        val p = if(page > 0) page else 1
        
        Secured(p != 42) { 
            Cached(p, sort) { 
                Action {
                    println("Listing page " + p + " using " + sort)
                    Ok(views.html.list(p, sort).toString)
                }
            }
        } 
    }
    
    def goToJava = Action {
        Redirect(routes.JavaApplication.index)
    }

}
