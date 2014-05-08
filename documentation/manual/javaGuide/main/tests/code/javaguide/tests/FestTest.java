package javaguide.tests;

//#test-fest
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class FestTest {
  
  @Test
  public void testSum() {
    int a = 1 + 1;
    assertThat(a).isEqualTo(2);
  }

}
//#test-fest