package play.core.j

import play.api.mvc._

import play.mvc.{Action => JAction, Result => JResult}
import play.mvc.Http.{Context => JContext, Request => JRequest}

trait JavaAction extends Action {
    
    def invocation:JResult
    def controller:Class[_]
    def method:java.lang.reflect.Method
    
    def apply(ctx:Context) = {
        
        val javaContext = new JContext {
            
            def request = new JRequest {
                
                def uri = ctx.request.uri
                def method = ctx.request.method
                def path = ctx.request.method
                
                override def toString = ctx.request.toString
                
            }
            
            override def toString = ctx.toString
            
        }
        
        val rootAction = new JAction[Any] {
            
            def call(ctx:JContext):JResult = {
                try {
                    JContext.current.set(ctx)
                    invocation
                } finally {
                    JContext.current.remove()
                }
            }
        } 
        
        val actionMixins = {
            method.getDeclaredAnnotations
                .filter(_.annotationType.isAnnotationPresent(classOf[play.mvc.With]))
                .map(a => a -> a.annotationType.getAnnotation(classOf[play.mvc.With]).value())
                .reverse
        }

        val finalAction = actionMixins.foldLeft(rootAction) { 
            case (deleguate, (annotation, actionClass)) => {
                val action = actionClass.newInstance().asInstanceOf[JAction[Any]]
                action.configuration = annotation
                action.deleguate = deleguate
                action
            }
        }
        
        finalAction.call(javaContext).getInternalResult
    }
    
}
