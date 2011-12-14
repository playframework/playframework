package test;

import org.junit.*;
import static org.junit.Assert.assertEquals;
import play.test.*;
import play.api.test.ResultData;
import java.util.*;
import play.Logger;
import play.mvc.Result;
import play.core.j.Wrap;

public class ApplicationTest extends MockApplication{

  @Before public void init() {
    injectGlobalMock(new ArrayList<String>(), MockData.dataSource());  
    Logger.warn("starting ApplicationTest...");
  }

  @Test public void Test() {
    play.api.mvc.Action action = Wrap.toAction(controllers.Application.index());
    ResultData result = Extract.from(action.apply(new FakeRequest()));
    assertEquals(result.body().toString().contains("Database 'default' needs evolution!"),true);
  }
  
  @After public void after() {
   clearMock();
  }
}
