/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

public class MultiParamTest extends AbstractRoutesTest {

  @Test
  public void checkMultiplyParams() {
    var result =
        route(
            app,
            fakeRequest(
                GET,
                "/multi-params?a=true&b=b&c=c&d=1&e=2&f=3&g=4&h=5&i=c62a07fc-7e02-485d-85af-9be3fbe1b3f9&j=6&k=7&l=8&m=m&n=n&o=o&p=p&q=q&r=r&s=s&t=t&u=u&v=v&w=w&x=x&y=y&z=z"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("true,b,c,1,2,3,4.0,5.0,c62a07fc-7e02-485d-85af-9be3fbe1b3f9,OptionalInt[6],OptionalLong[7],OptionalDouble[8.0],m,n,o,p,q,r,s,t,u,v,w,x,y,z");
  }
}
