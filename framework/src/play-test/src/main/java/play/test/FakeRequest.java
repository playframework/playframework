package play.test;

import play.libs.*;

import java.util.*;

public class FakeRequest {
    
    play.api.test.FakeRequest fake;
    
    public FakeRequest() {
        this.fake = play.api.test.FakeRequest.apply(); 
    }
    
    public FakeRequest(String method, String path) {
        this.fake = play.api.test.FakeRequest.apply(method, path); 
    }
    
    public FakeRequest withHeader(String name, String value) {
        fake = fake.withHeaders(Scala.varargs(Scala.Tuple(name, value)));
        return this;
    }
    
    public FakeRequest withUrlFormEncodedBody(java.util.Map<String,String> data) {
        List<scala.Tuple2<String,String>> args = new ArrayList<scala.Tuple2<String,String>>();
        for(String key: data.keySet()) {
            scala.Tuple2<String,String> pair = Scala.Tuple(key, data.get(key));
            args.add(pair);
        }
        fake = fake.withUrlFormEncodedBody(Scala.toSeq(args));
        return this;
    }
    
    public play.api.mvc.Request<play.mvc.Http.RequestBody> getWrappedRequest() {
        return ((play.api.test.FakeRequest<play.api.mvc.AnyContent>)fake).map(new scala.runtime.AbstractFunction1<play.api.mvc.AnyContent, play.mvc.Http.RequestBody>() {
            public play.mvc.Http.RequestBody apply(play.api.mvc.AnyContent anyContent) {
                return new play.core.j.JavaParsers.DefaultRequestBody(
                    anyContent.asUrlFormEncoded(),
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