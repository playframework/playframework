package play.test;
/**
 * provides a webdriver/selenium based integration test runner
 * by default HtmlUnitDriver and ChromeDriver are in context
 * example: 
 * {{{
 * class MyUnitTest extends IntegrationTest
 *
 * @Test public void myFunctionalTest {
 *  withNettyServer(new Runnable() {
 *   public void run() {
 *    HtmlUnitDriver driver = new HtmlUnitDriver();
 *    driver.get("http://localhost:9000");
 *    assertEquals(driver.getPageSource().contains("hello"),true);
 *   }
 *  } )
 * } 
 * }}}
 */
public abstract class IntegrationTest {
  /**
   * executes runnable in real play application context
   * @param runnable
   */
  public static void withNettyServer(Runnable r) { 
    play.api.test.IntegrationTest.withNettyServer(play.api.test.IntegrationTest.run(r));
  }  
}
