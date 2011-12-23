package play.test;

import play.libs.*;

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
    
    public play.api.mvc.Request getWrappedRequest() {
        return fake;
    }
    
}