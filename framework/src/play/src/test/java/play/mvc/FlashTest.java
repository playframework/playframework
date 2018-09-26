/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;

import static org.junit.Assert.*;

public class FlashTest {

    @Test
    public void initializeFlashWithKeyValue(){
        HashMap<String,String> data = new HashMap<String, String>();
        data.put("key", "value");
        Http.Flash flash = new Http.Flash(data);
        assertFalse(flash.isEmpty());
        assertEquals("value",flash.get("key"));
    }


    @Test
    public void createAnEmptyFlash(){
        Http.Flash emptyFlash = new Http.Flash(new HashMap<String, String>());
        assertTrue(emptyFlash.isEmpty());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void initializeFlashWithNullValue() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", null);
        exception.expect(IllegalArgumentException.class);
        new Http.Flash(initialisationData);
    }


    @Test
    public void initializeFlashWithNullKey(){
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put(null, "value");
        exception.expect(IllegalArgumentException.class);
        new Http.Flash(initialisationData);
    }


    @Test
    public void initializeFlashWithNullKeyAndNullValue() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put(null, null);
        exception.expect(IllegalArgumentException.class);
        new Http.Flash(initialisationData);
    }


    @Test
    public void putNullValueIntoFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Flash flash = new Http.Flash(initialisationData);
        exception.expect(IllegalArgumentException.class);
        flash.put("anOtherKey", null);
    }

    @Test
    public void putNullKeyIntoFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Flash flash = new Http.Flash(initialisationData);
        exception.expect(IllegalArgumentException.class);
        flash.put(null, "anOtherValue");
    }

    @Test
    public void putNullKeyAndNullValueIntoFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Flash flash = new Http.Flash(initialisationData);
        exception.expect(IllegalArgumentException.class);
        flash.put(null, null);
    }

    @Test
    public void putAllNullValueIntoFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");

        Http.Flash flash = new Http.Flash(initialisationData);

        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        otherData.put("3rdKey", null);

        exception.expect(IllegalArgumentException.class);
        flash.putAll(otherData);
    }

    @Test
    public void putAllNullKeyIntoFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");

        Http.Flash flash = new Http.Flash(initialisationData);

        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        otherData.put(null, "3rdValue");

        exception.expect(IllegalArgumentException.class);
        flash.putAll(otherData);
    }

    @Test
    public void putAllNullKeyAndNullValueIntoFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");

        Http.Flash flash = new Http.Flash(initialisationData);

        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        otherData.put(null, null);

        exception.expect(IllegalArgumentException.class);
        flash.putAll(otherData);
    }

    @Test
    public void isDirtyIsInitializedToFalseWithAnEmptyFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        assertFalse(flash.isDirty);
    }

    @Test
    public void isDirtyIsInitializedToFalseWithANonEmptyFlash() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Flash flash = new Http.Flash(initialisationData);
        assertFalse(flash.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToPut() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        flash.put("key", "value");
        assertTrue(flash.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToPutAll() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        flash.putAll(otherData);
        assertTrue(flash.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToRemove() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Flash flash = new Http.Flash(initialisationData);
        flash.remove("key");
        assertTrue(flash.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToRemoveANonExistingKey() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        flash.remove("key");
        assertTrue(flash.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToClear() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Flash flash = new Http.Flash(initialisationData);
        flash.clear();
        assertTrue(flash.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToClearAnEmptyFlashs() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        flash.clear();
        assertTrue(flash.isDirty);
    }

    @Test
    public void isDirtyDoesNotChangeWithAnIllegalArgumentToPut() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        try {
            flash.put("key", null);
        } catch (Exception e ) {}
        assertFalse(flash.isDirty);
    }

    @Test
    public void isDirtyDoesNotChangeWithAnIllegalArgumentToPutAll() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("key", "value");
        otherData.put("otherKey", null);
        try {
            flash.putAll(otherData);
        } catch (Exception e ) {}
        assertFalse(flash.isDirty);
    }

    @Test
    public void putAllDoesNotInsertAnyValueIfPassAnIllegalArgument() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("key", "value");
        otherData.put("otherKey", null);
        try {
            flash.putAll(otherData);
        } catch (Exception e ) {}
        assertFalse(flash.containsKey("key"));
    }

    @Test
    public void putANonNullKeyAndNonNullValue() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        flash.put("key", "value");
        assertTrue(flash.containsKey("key"));
    }

    @Test
    public void putAllNonNullKeysAndNonNullValues() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Flash flash = new Http.Flash(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("key", "value");
        otherData.put("otherKey", "anOtherValue");
        flash.putAll(otherData);
        assertTrue(flash.containsKey("key"));
        assertTrue(flash.containsKey("otherKey"));
    }
}
