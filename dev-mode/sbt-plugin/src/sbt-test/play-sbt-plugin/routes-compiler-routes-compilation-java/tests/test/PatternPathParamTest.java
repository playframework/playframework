/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

public class PatternPathParamTest extends AbstractRoutesTest {

    @Test
    public void checkPattern() {
        var result = route(app, fakeRequest(GET, "/pattern/123"));
        assertThat(result.status()).isEqualTo(OK);
        assertThat(contentAsString(result)).isEqualTo("123");
        // Invalid
        result = route(app, fakeRequest(GET, "/pattern/abc"));
        assertThat(result.status()).isEqualTo(NOT_FOUND);
    }
}
