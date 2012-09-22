package play.test;

import org.junit.Test;
import play.Play;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WithApplicationTest extends WithApplication {

    @Test
    public void withApplicationShouldProvideAnApplication() {
        start();
        assertNotNull(app);
        assertNotNull(Play.application());
    }

    @Test
    public void withApplicationShouldCleanUpApplication() {
        start();
        stopPlay();
        assertNull(app);
        assertTrue(play.api.Play.maybeApplication().isEmpty());
    }
}
