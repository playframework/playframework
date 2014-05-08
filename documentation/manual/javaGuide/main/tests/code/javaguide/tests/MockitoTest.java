package javaguide.tests;

import static org.junit.Assert.*;

//#test-mockito-import
import static org.mockito.Mockito.*;
//#test-mockito-import

import java.util.List;

import org.junit.Test;

public class MockitoTest {
  
  @Test
  public void testMockList() {
    
    //#test-mockito
    // Create and train mock
    List<String> mockedList = mock(List.class);
    when(mockedList.get(0)).thenReturn("first");
    
    // check value
    assertEquals("first", mockedList.get(0));
    
    // verify interaction
    verify(mockedList).get(0);
    //#test-mockito
  }

}

