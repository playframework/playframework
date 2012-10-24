# Application global settings

## The Global object

Defining a `Global` object in your project allows you to handle global settings for your application. This object must be defined in the root package.

```java
import play.*;

public class Global extends GlobalSettings {

}
```

## Intercepting application start-up and shutdown

You can override the `onStart` and `onStop` operation to be notified of the corresponding application lifecycle events:

```java
import play.*;

public class Global extends GlobalSettings {

  @Override
  public void onStart(Application app) {
    Logger.info("Application has started");
  }  
  
  @Override
  public void onStop(Application app) {
    Logger.info("Application shutdown...");
  }  
    
}
```

## Providing an application error page

When an exception occurs in your application, the `onError` operation will be called. The default is to use the internal framework error page. You can override this:

```java
import play.*;
import play.mvc.*;

import static play.mvc.Results.*;

public class Global extends GlobalSettings {

  @Override
  public Result onError(Throwable t) {
    return internalServerError(
      views.html.errorPage(t)
    );
  }  
    
}
```

## Handling action not found

If the framework doesn’t find an action method for a request, the `onHandlerNotFound` operation will be called:

```java
import play.*;
import play.mvc.*;

import static play.mvc.Results.*;

public class Global extends GlobalSettings {

  @Override
  public Result onHandlerNotFound(String uri) {
    return notFound(
      views.html.pageNotFound(uri)
    );
  }  
    
}
```

The `onBadRequest` operation will be called if a route was found, but it was not possible to bind the request parameters:

```scala
import play.*;
import play.mvc.*;

import static play.mvc.Results.*;

public class Global extends GlobalSettings {

  @Override
  public Result onBadRequest(String uri, String error) {
    return badRequest("Don't try to hack the URI!");
  }  
    
}
```

> **Next:** [[Intercepting requests | JavaInterceptors]]