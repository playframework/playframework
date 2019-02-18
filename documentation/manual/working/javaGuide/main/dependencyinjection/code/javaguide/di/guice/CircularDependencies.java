/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice;

import javax.inject.Inject;
import javax.inject.Provider;

class CircularDependencies {

  class NoProvider {
    // #circular
    public class Foo {
      @Inject
      public Foo(Bar bar) {
        // ...
      }
    }

    public class Bar {
      @Inject
      public Bar(Baz baz) {
        // ...
      }
    }

    public class Baz {
      @Inject
      public Baz(Foo foo) {
        // ...
      }
    }
    // #circular
  }

  class WithProvider {
    // #circular-provider
    public class Foo {
      @Inject
      public Foo(Bar bar) {
        // ...
      }
    }

    public class Bar {
      @Inject
      public Bar(Baz baz) {
        // ...
      }
    }

    public class Baz {
      @Inject
      public Baz(Provider<Foo> fooProvider) {
        // ...
      }
    }
    // #circular-provider
  }
}
