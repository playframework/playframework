/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di.guice;

import javax.inject.Inject;
import javax.inject.Provider;

class CircularDependencies {

class NoProvider {
//#circular
public class Foo {
  @Inject Bar bar;
}
public class Bar {
  @Inject Baz baz;
}
public class Baz {
  @Inject Foo foo;
}
//#circular
}

class WithProvider {
//#circular-provider
public class Foo {
  @Inject Bar bar;
}
public class Bar {
  @Inject Baz baz;
}
public class Baz {
  @Inject Provider<Foo> fooProvider;
}
//#circular-provider
}

}
