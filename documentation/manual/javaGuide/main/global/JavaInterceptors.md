# Intercepting requests

## Overriding onRequest

One important aspect of  the ```GlobalSettings``` class is that it provides a way to intercept requests and execute business logic before a request is dispatched to an action.

For example:

```java
import play.*;
import play.mvc.Action;
import play.mvc.Http.Request;
import java.lang.reflect.Method;

public class Global extends GlobalSettings {

@Override
public Action onRequest(Request request, Method actionMethod) {
   System.out.println("before each request..." + request.toString());
   return super.onRequest(request, actionMethod);
}

}
```

It’s also possible to intercept a specific action method. This can be achieved via [[Action composition| JavaActionsComposition]].

> **Next:** [[Testing your application | JavaTest]]