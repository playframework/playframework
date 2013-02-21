package test;

import org.junit.Test;
import play.api.libs.Files;

public class CheckJUnitRunningTest {
    @Test
    public void performSomeSideEffectSoCanBeCheckedLater() {
       Files.writeFile(new java.io.File("target/junit-running"), "true");
    }
}
