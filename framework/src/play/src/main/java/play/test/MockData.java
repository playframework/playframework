package play.test;

import java.util.Map;

/*
 * provides standard mock data
 */
public abstract class MockData {

  /*
   * @return data source mock
   * provides in memory datasource mock
   */
  public static Map<String,String> dataSource() {
    return play.api.test.MockData.dataSourceAsJava();
  }

}
