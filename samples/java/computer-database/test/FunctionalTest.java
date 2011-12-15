package test;

import org.junit.*;
import play.test.*;
import static play.test.IntegrationTest.*;
import java.util.*;
import play.api.mvc.*;
import play.Logger;
import fr.javafreelance.fluentlenium.core.test.FluentTest;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.WebDriver;
import static org.junit.Assert.assertEquals;

public class FunctionalTest extends FluentTest{
  
  public WebDriver webDriver = new HtmlUnitDriver();

  @Override
  public WebDriver getDefaultDriver() {
        return webDriver;
  }

  @Before public void init() {
    Logger.warn("starting FunctionalTest...");
    play.api.db.evolutions.OfflineEvolutions.applyScript("default");
  }

  @Test public void Test() {
     withNettyServer(new Runnable() {
          public void run() {
            goTo("http://localhost:9001");
            boolean cond =  pageSource().contains("574 computers found");
            assertEquals(cond, true);
          }});
  }

}
