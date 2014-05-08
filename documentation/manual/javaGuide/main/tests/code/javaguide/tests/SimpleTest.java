package javaguide.tests;

//#test-simple
import static org.junit.Assert.*;

import org.junit.Test;

public class SimpleTest {

  @Test
  public void testSum() {
    int a = 1 + 1;
    assertEquals(2, a);
  }
    
  @Test
  public void testString() {
    String str = "Hello world";
    assertFalse(str.isEmpty());
  }

}
//#test-simple
