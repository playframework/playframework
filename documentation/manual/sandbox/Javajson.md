# JSON support

Play includes the [Jackson](http://jackson.codehaus.org/) library as a dependency _(A good tutorial of Jackson can be found [here](http://wiki.fasterxml.com/JacksonInFiveMinutes))_. What this means in practice is that one can serialize from and to JSON format. 

# Render JSON
As for rendering JSON, there is a helper method ```play.json.Render.toJson``` that can be used to send JSON as a response. For example:

```
import play.*;
import play.mvc.*;
import static play.libs.Json.toJson;

import java.util.Map;
import java.util.HashMap;

public class MyController extends Controller {
  
  public static Result index() {
    Map<String,String> d = new HashMap<String,String>();
    d.put("peter","foo");
    d.put("yay","value");
    return ok(toJson(d));
   }   
}
```

# Google Guava
The code above can be rewritten for brevity using the Google Guava collections library:

```
import com.google.common.collect.ImmutableMap;
import play.*;
import play.mvc.*;
import static play.libs.Json.toJson;

public class MyController extends Controller {
  
  public static Result index() {
    return ok(toJson(ImmutableMap.of(
        "peter", "foo",
        "yay", "value")));
   }
}
```