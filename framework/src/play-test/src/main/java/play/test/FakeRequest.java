package play.test;

public class FakeRequest {
    
    final play.api.test.FakeRequest fake;
    
    public FakeRequest() {
        this.fake = play.api.test.FakeRequest.apply(); 
    }
    
    public FakeRequest(String method, String path) {
        this.fake = play.api.test.FakeRequest.apply(method, path); 
    }
    
    public play.api.mvc.Request getWrappedRequest() {
        return fake;
    }
    
}