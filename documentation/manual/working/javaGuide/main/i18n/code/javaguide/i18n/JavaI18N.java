/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.i18n;

import org.junit.Test;
import org.junit.Before;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javaguide.testhelpers.MockJavaAction;
import javaguide.testhelpers.MockJavaActionHelper;
import javaguide.i18n.html.hellotemplate;
import javaguide.i18n.html.helloscalatemplate;
import play.Application;
import play.mvc.Result;
import play.test.WithApplication;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import play.i18n.Messages;


public class JavaI18N extends WithApplication {

    @Override
    public Application provideApplication() {
        return fakeApplication(ImmutableMap.of(
            "play.i18n.langs", ImmutableList.of("en", "en-US", "fr"),
            "messages.path", "javaguide/i18n"
            ));
    }

    @Test
    public void checkDefaultHello() {
        Result result = MockJavaActionHelper.call(new DefaultLangController(), fakeRequest("GET", "/"));
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class DefaultLangController extends MockJavaAction {
        //#default-lang-render
        public Result index() {
            return ok(hellotemplate.render()); // "hello"
        }
        //#default-lang-render
    }

    @Test
    public void checkDefaultScalaHello() {
        Result result = MockJavaActionHelper.call(new DefaultScalaLangController(), fakeRequest("GET", "/"));
        assertThat(contentAsString(result), containsString("hello"));
    }

    public static class DefaultScalaLangController extends MockJavaAction {
        public Result index() {
            return ok(helloscalatemplate.render()); // "hello"
        }
    }

    @Test
    public void checkChangeLangHello() {
        Result result = MockJavaActionHelper.call(new ChangeLangController(), fakeRequest("GET", "/"));
        assertThat(contentAsString(result), containsString("bonjour"));
    }

    public static class ChangeLangController extends MockJavaAction {
        //#change-lang-render
        public Result index() {
            ctx().changeLang("fr");
            return ok(hellotemplate.render()); // "bonjour"
        }
        //#change-lang-render
    }

    @Test
    public void checkSetTransientLangHello() {
        Result result = MockJavaActionHelper.call(new SetTransientLangController(), fakeRequest("GET", "/"));
        assertThat(contentAsString(result), containsString("howdy"));
    }

    public static class SetTransientLangController extends MockJavaAction {
        //#set-transient-lang-render
        public Result index() {
            ctx().setTransientLang("en-US");
            return ok(hellotemplate.render()); // "howdy"
        }
        //#set-transient-lang-render
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
