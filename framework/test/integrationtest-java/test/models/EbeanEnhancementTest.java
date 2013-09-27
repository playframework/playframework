package models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

public class EbeanEnhancementTest {
    @Test
    public void testHelloWorld() {
        String hw = HelloWorld.HELLO_WORLD;
        assertEquals(hw, "Hello world!");
    }

    @Test
    public void testFieldAccess() {
        running(fakeApplication(), new Runnable() {
            public void run() {

                HelloWorld hw = new HelloWorld();
                hw.field1 = "Hi, I'm field1!";
                hw.save();

                HelloWorld result = HelloWorld.find.byId(hw.id);
                assertEquals(result.field1, "Hi, I'm field1!");
            }
        });
    }

    @Test
    public void testPropertyAccess() {
        running(fakeApplication(), new Runnable() {
            public void run() {

                HelloWorld hw = new HelloWorld();
                hw.field1 = "Hi, I'm field1!";
                hw.save();
                hw.setField2("Hi, I'm field2!");
                hw.save();

                HelloWorld result = HelloWorld.find.byId(hw.id);
                assertEquals(result.field1, "Hi, I'm field1!");
                assertEquals(result.field2, "Hi, I'm field2!");
            }
        });
    }

    @Test
    public void testFieldToPropertyEnhancement() {
        running(fakeApplication(), new Runnable() {
            public void run() {

                HelloWorld hw = new HelloWorld();
                hw.field1 = "Hi, I'm field1!";
                hw.save();
                hw.field2 = "Hi, I'm field2!";
                hw.save();

                HelloWorld result = HelloWorld.find.byId(hw.id);
                assertEquals(result.field1, "Hi, I'm field1!");
                assertEquals(result.field2, "Hi, I'm field2!");

                result.field1 = "newName1";
                assertEquals(result._ebean_getIntercept().isDirty(), true);
            }
        });
    }

}
