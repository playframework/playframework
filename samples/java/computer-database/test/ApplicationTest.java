import org.junit.*;
import static org.junit.Assert.assertEquals;
import play.test.*;
import play.api.test.ResultData;
import java.util.*;
import play.Logger;
import play.mvc.Result;
import static play.core.j.Wrap.*;
import java.util.HashMap;
import play.mvc.Http.RequestBody;
import play.mvc.Http.Context;

public class ApplicationTest extends MockApplication{

  @Before public void init() {
    //run evolution before anything
    play.api.db.evolutions.OfflineEvolutions.applyScript("default");

    //inject application mock
    injectGlobalMock(new ArrayList<String>(), MockData.dataSource());  
    Logger.warn("starting ApplicationTest...");
    
  }

  @Test public void Test() {

    //create mock request
    play.api.mvc.Request<RequestBody> req = toRequest(
    "/computers",
    "GET",
    new HashMap<String, String[]>(),
    new HashMap<String, String[]>(),
    "peter",
    "/computers",
    new HashMap<String, String[]>(),
    new HashMap<String, String>()
    );

    //set mock request into context
    Context.current.set(createJavaContext(req));

    //create Action
    play.api.mvc.Action action = toAction(
      controllers.Application.list(0, "name", "asc", "")
    );

    //execute action
    ResultData result = Extract.from(action.apply(req));

    assertEquals(result.body().toString().contains("574 computers found"),true);
  }
  
  @After public void after() {
   clearMock();
  }
}
