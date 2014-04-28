package play.test;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class WithBrowserTest extends WithBrowser {
    @Test
    public void withBrowserShouldProvideABrowser() {
        assertNotNull(browser);
        browser.goTo("/");
        assertThat(browser.pageSource(), containsString("Action not found"));
    }
}
