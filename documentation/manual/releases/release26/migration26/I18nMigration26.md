# Internationalization (I18N) and Messages Migration

## MessagesApi Request Attribute

A `play.api.i18n.MessagesApi` instance is now pulled from dependency injection and is inserted into every request as a required request attribute, using a system filter.

This means that a dependency injected `MessagesApi` instance is a requirement for executing requests.
 
## Adding MessagesApi to Runtime DI 
 
Play's `reference.conf` defines `play.api.i18n.I18nModule` as an enabled module out of the box, and so for applications using runtime dependency injection, this should have no effect in development.

## Adding MessagesApi to Compile Time DI

There may be issues for integration tests that override Guice modules or make use of compile time dependency injection and use `BuiltInComponents` or `BuiltInComponentsFromContext` directly, without mixing in `I18nComponents`.  

Because `BuiltInComponentsFromContext` does not include a `MessagesApi`, there is a new class `BuiltInAndI18nComponentsFromContext` which mixes in `I18nComponents` and adds `MessagesApi` and `Lang` to the injector.

The "right" way to resolve a missing `MessagesApi` is to include `I18nComponents` or extend `BuiltInAndI18nComponentsFromContext`, but you can also specify a fallback `DefaultMessagesApi` with `play.i18n.fallback=true` in `application.conf` if this behavior is not significant to the test.

## Providers for MessagesApi and Langs

The configuration loading behavior `DefaultMessagesApi` and `DefaultLangs` classes have been broken apart so that the configuration and loading of messages and languages are done by `DefaultMessagesApiProvider` and `DefaultLangsProvider`, respectively.  These providers implement `javax.inject.Provider` and so can be safely used in JSR-330. 

If you want to load messages from configuration in an integration test, you can add the following to code:

```scala
val conf = Configuration.reference
val messagesApi = new DefaultMessagesApiProvider(Environment.simple(), conf, new DefaultLangsProvider(conf).get).get
```

But now, if you want to use a `MessagesApi` instance without going through configuration, you can create an instance of `DefaultMessagesApi` directly, or leave it empty:

```scala 
val messagesApi = new DefaultMessagesApi() // defaults to Seq.empty
val langs: Langs = DefaultLangs() // defaults to Lang.defaultLang
```

This is especially useful in situations where a `MessagesApi` instance is required, but no error message needs to be handled in the test. 
