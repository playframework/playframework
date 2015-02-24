/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.i18n;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import play.Application;
import play.test.WithApplication;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableMap;

import play.i18n.Messages;


public class JavaI18N extends WithApplication {

    @Override
    public Application provideApplication() {
        return fakeApplication(ImmutableMap.of("messages.path", "javaguide/i18n"));
    }

    @Test
    public void testSingleApostrophe() {
        assertTrue(singleApostrophe());
    }

    private Boolean singleApostrophe() {
//#single-apostrophe
        String errorMessage = Messages.get("info.error");
        Boolean areEqual = errorMessage.equals("You aren't logged in!");
//#single-apostrophe

        return areEqual;
    }

    @Test
    public void testEscapedParameters() {
        assertTrue(escapedParameters());
    }

    private Boolean escapedParameters() {
//#parameter-escaping
        String errorMessage = Messages.get("example.formatting");
        Boolean areEqual = errorMessage.equals("When using MessageFormat, '{0}' is replaced with the first parameter.");
//#parameter-escaping

        return areEqual;
    }
}
