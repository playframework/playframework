package play.test;

import play.api.test.Mock;
import play.api.Plugin;
import java.util.List;
import java.util.Map;

/*
 * provides a mock application context for JUnit tests
 * example:
 * {{{
 * public class MyTest extends MockApplication{
 *  
 *  @Before protected void initialize() {
 *    injectGlobalMock(new ArrayList<String>(), MockData.dataSource());
 *  }
 *
 *  @Test public void myTest {...}
 *
 *  @After protected void tearDown() {
 *    clearMock();
 *  }
 * }
 * }}}
 */
public abstract class MockApplication extends Mock {

  /*
   *
   * creates an play application and sets it into global context, this method is usually used together with clearMock()
   */
  public play.api.Application injectGlobalMock(List<String> mockPlugins, Map<String,String> mockConfig) {
     play.api.Application app = createMock(mockPlugins, mockConfig);
     play.api.Play.start(app);
     return app;
  }
}
