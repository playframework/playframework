/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

import java.util.HashMap;

public class SessionTest {

    @Test
    public void initializeSessionWithKeyValue(){
        HashMap<String,String> data = new HashMap<String, String>();
        data.put("key", "value");
        Http.Session session = new Http.Session(data);
        assertFalse(session.isEmpty());
        assertEquals("value",session.get("key"));
    }


    @Test
    public void createAnEmptySession(){
        Http.Session emptySession = new Http.Session(new HashMap<String, String>());
        assertTrue(emptySession.isEmpty());
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void initializeSessionWithNullValue() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", null);
        exception.expect(IllegalArgumentException.class);
        new Http.Session(initialisationData);
    }


    @Test
    public void initializeSessionWithNullKey(){
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put(null, "value");
        exception.expect(IllegalArgumentException.class);
        new Http.Session(initialisationData);
    }


    @Test
    public void initializeSessionWithNullKeyAndNullValue() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put(null, null);
        exception.expect(IllegalArgumentException.class);
        new Http.Session(initialisationData);
    }


    @Test
    public void putNullValueIntoSession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Session session = new Http.Session(initialisationData);
        exception.expect(IllegalArgumentException.class);
        session.put("anOtherKey", null);
    }

    @Test
    public void putNullKeyIntoSession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Session session = new Http.Session(initialisationData);
        exception.expect(IllegalArgumentException.class);
        session.put(null, "anOtherValue");
    }

    @Test
    public void putNullKeyAndNullValueIntoSession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Session session = new Http.Session(initialisationData);
        exception.expect(IllegalArgumentException.class);
        session.put(null, null);
    }

    @Test
    public void putAllNullValueIntoSession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");

        Http.Session session = new Http.Session(initialisationData);

        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        otherData.put("3rdKey", null);

        exception.expect(IllegalArgumentException.class);
        session.putAll(otherData);
    }

    @Test
    public void putAllNullKeyIntoSession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");

        Http.Session session = new Http.Session(initialisationData);

        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        otherData.put(null, "3rdValue");

        exception.expect(IllegalArgumentException.class);
        session.putAll(otherData);
    }

    @Test
    public void putAllNullKeyAndNullValueIntoSession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");

        Http.Session session = new Http.Session(initialisationData);

        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        otherData.put(null, null);

        exception.expect(IllegalArgumentException.class);
        session.putAll(otherData);
    }

    @Test
    public void isDirtyIsInitializedToFalseWithAnEmptySession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        assertFalse(session.isDirty);
    }

    @Test
    public void isDirtyIsInitializedToFalseWithANonEmptySession() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Session session = new Http.Session(initialisationData);
        assertFalse(session.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToPut() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        session.put("key", "value");
        assertTrue(session.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToPutAll() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("2ndKey", "2ndValue");
        session.putAll(otherData);
        assertTrue(session.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToRemove() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Session session = new Http.Session(initialisationData);
        session.remove("key");
        assertTrue(session.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToRemoveANonExistingKey() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        session.remove("key");
        assertTrue(session.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToClear() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        initialisationData.put("key", "value");
        Http.Session session = new Http.Session(initialisationData);
        session.clear();
        assertTrue(session.isDirty);
    }

    @Test
    public void isDirtyBecomesTrueWithACallToClearAnEmptySessions() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        session.clear();
        assertTrue(session.isDirty);
    }

    @Test
    public void isDirtyDoesNotChangeWithAnIllegalArgumentToPut() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        try {
            session.put("key", null);
        } catch (Exception e ) {}
        assertFalse(session.isDirty);
    }

    @Test
    public void isDirtyDoesNotChangeWithAnIllegalArgumentToPutAll() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("key", "value");
        otherData.put("otherKey", null);
        try {
            session.putAll(otherData);
        } catch (Exception e ) {}
        assertFalse(session.isDirty);
    }

    @Test
    public void putAllDoesNotInsertAnyValueIfPassAnIllegalArgument() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("key", "value");
        otherData.put("otherKey", null);
        try {
            session.putAll(otherData);
        } catch (Exception e ) {}
        assertFalse(session.containsKey("key"));
    }

    @Test
    public void putANonNullKeyAndNonNullValue() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        session.put("key", "value");
        assertTrue(session.containsKey("key"));
    }

    @Test
    public void putAllNonNullKeysAndNonNullValues() {
        HashMap<String,String> initialisationData = new HashMap<String, String>();
        Http.Session session = new Http.Session(initialisationData);
        HashMap<String,String> otherData = new HashMap<String, String>();
        otherData.put("key", "value");
        otherData.put("otherKey", "anOtherValue");
        session.putAll(otherData);
        assertTrue(session.containsKey("key"));
        assertTrue(session.containsKey("otherKey"));
    }
}
