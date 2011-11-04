package play.test;

import play.api.test.Mock;
import play.api.Plugin;
import play.api.Application;
import java.util.List;
import java.util.Map;

abstract class MockApplication extends Mock{

  public Map<String,String> withMockDataSources() { 
    return mockDataAsJava();
  }

  public Map<String,String> mockConfig;

  public List<Plugin> mockPlugins;
  
  public MockApplication() {
    setCurrentApp(makeApp(mockPlugins, mockConfig));
  }

}
