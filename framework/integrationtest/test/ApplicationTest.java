package test;

import org.junit.*;
import static org.junit.Assert.assertEquals;
import play.test.*;
import java.util.*;
import play.api.mvc.*;
import play.Logger;

public class ApplicationTest extends MockApplication{

  public ApplicationTest() {
    Logger.warn("starting ApplicationTest...");
  }
  @Before public void init() {
    injectGlobalMock(new ArrayList<String>(), MockData.dataSource());  
  }

  @Test public void Test() {
    Action action = controllers.Application.index_java_cache();
    Result result = action.apply(new FakeRequest());
    assertEquals(result.toString().contains("200,Map(Content-Type -> text/html"),true);
  }
  @After public void after() {
   clearMock();
  }
}
