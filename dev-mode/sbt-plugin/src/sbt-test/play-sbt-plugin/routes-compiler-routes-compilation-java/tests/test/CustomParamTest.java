/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import models.UserId;
import org.junit.Test;

import static controllers.routes.Application;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomParamTest extends AbstractRoutesTest {

    @Test
    public void checkCustomParametersInQuery() {
        assertThat(Application.queryUser(new UserId.UserIdQueryParam("foo")).url())
                .isEqualTo("/query-user?userId=foo");
        assertThat(Application.queryUser(new UserId.UserIdQueryParam("foo/bar")).url())
                .isEqualTo("/query-user?userId=foo%2Fbar");
        assertThat(Application.queryUser(new UserId.UserIdQueryParam("foo?bar")).url())
                .isEqualTo("/query-user?userId=foo%3Fbar");
        assertThat(Application.queryUser(new UserId.UserIdQueryParam("foo%bar")).url())
                .isEqualTo("/query-user?userId=foo%25bar");
        assertThat(Application.queryUser(new UserId.UserIdQueryParam("foo&bar")).url())
                .isEqualTo("/query-user?userId=foo%26bar");
    }

    @Test
    public void checkCustomParametersInPath() {
        assertThat(Application.user(new UserId("foo")).url()).isEqualTo("/users/foo");
        assertThat(Application.user(new UserId("foo/bar")).url()).isEqualTo("/users/foo%2Fbar");
        assertThat(Application.user(new UserId("foo?bar")).url()).isEqualTo("/users/foo%3Fbar");
        assertThat(Application.user(new UserId("foo%bar")).url()).isEqualTo("/users/foo%25bar");
        // & is not special for path segments
        assertThat(Application.user(new UserId("foo&bar")).url()).isEqualTo("/users/foo&bar");
    }
}
