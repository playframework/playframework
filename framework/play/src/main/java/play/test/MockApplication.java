package play.test;

import play.api.test.Mock;
import play.api.Plugin;
import play.api.Application;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Provides mocking ability for java apps. A typical use case:
 * {{{
 * MyGlobalApplicationMock extends MockApplication {
 *
 *  public Map<String,String> mockConfig {
 *     Map<String,String>  configs = withMockDataSources();
 *     configs.put("mykey","myvalue");
 *     return configs;
 *  }
 *
 *  @Override
 *  public List<Plugin> mockPlugins {
 *     //if you have any plugins you want to mock
 *     List<Plugin> plugins = new ArrayList<Plugin>();
 *     plugins.add(mock(CachePlugin.class));
 *     return plugins;
 *  }
 * }
 * }}}
 * after which add this to your @Before method in your junit test: `new MyGlobalApplicationMock()` this will utilize the mock values.
 * Please note this is a global mock
 */
public abstract class MockApplication extends Mock{

  /*
   * sets the mock with in memory cache and db connection
   */
  public Map<String,String> withMockDataSources() { 
    return mockDataAsJava();
  }

  /*
   * lets you to provide your mock configs
   */
  abstract public Map<String,String> mockConfig();

  /*
   * provides the mocked plugins, 
   * @return  an empty list if not specified
   */
  public List<Plugin> mockPlugins() {
    return new ArrayList<Plugin>();
  }
  
  public MockApplication() {
    setCurrentApp(makeApp(mockPlugins(), mockConfig()));
  }

}
