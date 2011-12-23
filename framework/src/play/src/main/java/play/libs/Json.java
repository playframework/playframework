package play.libs;

import java.io.StringWriter;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * provides helpers to deal with JSON
 */
 
public class Json {

   /**
    * provides a simple way to serialize into JSON.
    *
    * usage (in a controller):
    * public static Result index() {
    *{{{
    *Map<String,String> d = new HashMap<String,String>();
    *d.put("peter","foo");
    *d.put("yay","value");
    *    return ok(toJson(d));
    *}
    *}}}
    *
    * @param data to be serialized 
    */
   public static play.mvc.Content toJson(final Object data) {
     return new play.mvc.Content() {
        public String body() {
            ObjectMapper mapper = new ObjectMapper();
            StringWriter w = new StringWriter();
            try {
                mapper.writeValue(w,data);
            } catch (java.io.IOException ex) {
                play.Logger.warn("could not seralize to JSON:"+data.toString());
            }
            return w.toString();
        }

        public String contentType() {
            return "application/json";
        }
     };

   }
}
