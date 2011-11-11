package play.test;

/**
 * Provides a webdriver/selenium based integration test runner
 * by default HtmlUnitDriver and ChromeDriver are in context.
 */
public abstract class IntegrationTest {
    
    /**
     * executes runnable in real play application context
     * @param runnable
     */
    public static void withNettyServer(Runnable runnable) { 
       play.api.test.IntegrationTest.withNettyServer(play.api.test.IntegrationTest.run(runnable));
    }  
  
}
