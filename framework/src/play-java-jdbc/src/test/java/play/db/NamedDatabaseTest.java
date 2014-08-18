/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import com.google.common.collect.ImmutableMap;
import javax.inject.Inject;
import org.junit.Test;
import play.test.FakeApplication;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class NamedDatabaseTest extends WithApplication {

    @Override
    protected FakeApplication provideFakeApplication() {
        return fakeApplication(ImmutableMap.of(
            "db.default.driver", "org.h2.Driver",
            "db.default.url", "jdbc:h2:mem:default",
            "db.other.driver", "org.h2.Driver",
            "db.other.url", "jdbc:h2:mem:other"
        ));
    }

    @Test
    public void bindDatabasesByName() {
      DatabaseComponent component = app.getWrappedApplication().injector().instanceOf(DatabaseComponent.class);
      assertThat(component.db.getUrl(), equalTo("jdbc:h2:mem:default"));
      assertThat(component.named.getUrl(), equalTo("jdbc:h2:mem:default"));
      assertThat(component.other.getUrl(), equalTo("jdbc:h2:mem:other"));
    }

    public static class DatabaseComponent {
        @Inject Database db;
        @Inject @NamedDatabase("default") Database named;
        @Inject @NamedDatabase("other") Database other;

        public DatabaseComponent() {}
    }

}
