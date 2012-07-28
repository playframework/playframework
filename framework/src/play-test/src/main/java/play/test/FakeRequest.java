package play.test;

import org.codehaus.jackson.JsonNode;
import play.api.mvc.AnyContentAsJson;
import play.libs.*;
import play.mvc.*;

import java.util.*;
import scala.collection.Seq;

/**
 * Fake HTTP request implementation.
 */
public class FakeRequest {
    
    play.api.test.FakeRequest fake;
    
    /**
     * Constructs a new GET / fake request.
     */
    public FakeRequest() {
        this.fake = play.api.test.FakeRequest.apply(); 
    }
    
    /**
     * Constructs a new request.
     */
    public FakeRequest(String method, String path) {
        this.fake = play.api.test.FakeRequest.apply(method, path); 
    }
    
    /**
     * Add addtional headers to this request.
     */
    @SuppressWarnings(value = "unchecked")
    public FakeRequest withHeader(String name, String value) {
        fake = fake.withHeaders(Scala.varargs(Scala.Tuple(name, value)));
        return this;
    }

    /**
     * Set a Json Body to this request.
     * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
     * The method is set to <tt>POST</tt>.
     * @param node the Json Node
     * @return the Fake Request
     */
    public FakeRequest withJsonBody(JsonNode node) {
        Map<String, Seq<String>> map = new HashMap(Scala.asJava(fake.headers().toMap()));
        map.put("Content-Type", Scala.toSeq(new String[] {"application/json"}));
        AnyContentAsJson content = new AnyContentAsJson(play.api.libs.json.Json.parse(node.toString()));
        fake = new play.api.test.FakeRequest(Helpers.POST, fake.path(), new play.api.test.FakeHeaders(Scala.asScala(map)), content, "127.0.0.1");
        return this;
    }

    /**
     * Set a Json Body to this request.
     * The <tt>Content-Type</tt> header of the request is set to <tt>application/json</tt>.
     * @param node the Json Node
     * @param method the HTTP method. <tt>POST</tt> if set to <code>null</code>
     * @return the Fake Request
     */
    public FakeRequest withJsonBody(JsonNode node, String method) {
        if (method == null) {
            method = Helpers.POST;
        }
        Map<String, Seq<String>> map = new HashMap(Scala.asJava(fake.headers().toMap()));
        map.put("Content-Type", Scala.toSeq(new String[] {"application/json"}));
        AnyContentAsJson content = new AnyContentAsJson(play.api.libs.json.Json.parse(node.toString()));
        fake = new play.api.test.FakeRequest(method, fake.path(), new play.api.test.FakeHeaders(Scala.asScala(map)), content, "127.0.0.1");
        return this;
    }

   /**
    * Add addtional session to this request.
    */
    @SuppressWarnings(value = "unchecked")
    public FakeRequest withFlash(String name, String value) {
        fake = fake.withFlash(Scala.varargs(Scala.Tuple(name, value)));
        return this;
    }

      /**
     * Add addtional session to this request.
     */
    @SuppressWarnings(value = "unchecked")  
    public FakeRequest withSession(String name, String value) {
        fake = fake.withSession(Scala.varargs(Scala.Tuple(name, value)));
        return this;
    }

    /**
     * Add cookies to this request
     */
    @SuppressWarnings(value = "unchecked")
    public FakeRequest withCookies(Http.Cookie... cookies) {
        List <play.api.mvc.Cookie> scalacookies = new ArrayList<play.api.mvc.Cookie>();
        for (Http.Cookie c : cookies) {
            scalacookies.add(new play.api.mvc.Cookie(c.name(), c.value(), c.maxAge(), c.path(), Scala.Option(c.domain()), c.secure(), c.httpOnly()) );
        }
        fake = fake.withCookies(Scala.varargs(scalacookies.toArray()));
        return this;
    }

    /**
     * Set a Form url encoded body to this request.
     */
    @SuppressWarnings(value = "unchecked")
    public FakeRequest withFormUrlEncodedBody(java.util.Map<String,String> data) {
        List<scala.Tuple2<String,String>> args = new ArrayList<scala.Tuple2<String,String>>();
        for(String key: data.keySet()) {
            scala.Tuple2<String,String> pair = Scala.Tuple(key, data.get(key));
            args.add(pair);
        }
        fake = fake.withFormUrlEncodedBody(Scala.toSeq(args));
        return this;
    }
    
    @SuppressWarnings(value = "unchecked")
    public play.api.mvc.Request<play.mvc.Http.RequestBody> getWrappedRequest() {
        return ((play.api.test.FakeRequest<play.api.mvc.AnyContent>)fake).map(new scala.runtime.AbstractFunction1<play.api.mvc.AnyContent, play.mvc.Http.RequestBody>() {
            public play.mvc.Http.RequestBody apply(play.api.mvc.AnyContent anyContent) {
                return new play.core.j.JavaParsers.DefaultRequestBody(
                    anyContent.asFormUrlEncoded(),
                    anyContent.asRaw(),
                    anyContent.asText(),
                    anyContent.asJson(),
                    anyContent.asXml(),
                    anyContent.asMultipartFormData(),
                    false
                );
            }
        });
    }
    
}
