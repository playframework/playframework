package test;

import org.junit.*;
import static org.junit.Assert.assertEquals;
import play.test.*;
import static play.test.IntegrationTest.*;
import java.util.*;
import play.api.mvc.*;
import play.Logger;
import fr.javafreelance.fluentlenium.core.test.FluentTest;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.WebDriver;

public class FunctionalTest extends FluentTest{
    
  public WebDriver webDriver = new HtmlUnitDriver();

  @Override
  public WebDriver getDefaultDriver() {
        return webDriver;
  }

  @Before public void init() {
    Logger.warn("starting ApplicationTest...");
  }

  @Test public void Test() {
     withNettyServer(new Runnable() {
          public void run() {
            goTo("http://localhost:9000");
            submit("#evolution-button");
            assertEquals(pageSource().contains("574 computers found"), true);  
          }});
  }

}