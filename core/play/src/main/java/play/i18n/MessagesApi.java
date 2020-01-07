/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.i18n;

import play.api.mvc.Cookie;
import play.libs.Scala;
import play.mvc.Http;
import play.mvc.Result;
import scala.collection.immutable.Seq;
import scala.collection.mutable.Buffer;
import scala.compat.java8.OptionConverters;
import scala.Option;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** The messages API. */
@Singleton
public class MessagesApi {

  private final play.api.i18n.MessagesApi messages;

  @Inject
  public MessagesApi(play.api.i18n.MessagesApi messages) {
    this.messages = messages;
  }

  /** @return the Scala versions of the Messages API. */
  public play.api.i18n.MessagesApi asScala() {
    return messages;
  }

  /**
   * Converts the varargs to a scala buffer, takes care of wrapping varargs into a intermediate list
   * if necessary
   *
   * @param args the message arguments
   * @return scala type for message processing
   */
  private static Seq<Object> convertArgsToScalaBuffer(final Object... args) {
    return scala.collection.JavaConverters.asScalaBufferConverter(wrapArgsToListIfNeeded(args))
        .asScala()
        .toList();
  }

  /**
   * Wraps arguments passed into a list if necessary.
   *
   * <p>Returns the first value as is if it is the only argument and a subtype of `java.util.List`
   * Otherwise, it calls Arrays.asList on args
   *
   * @param args arguments as a List
   */
  @SafeVarargs
  @SuppressWarnings("unchecked")
  private static <T> List<T> wrapArgsToListIfNeeded(final T... args) {
    List<T> out;
    if (args == null) {
      out = Collections.emptyList();
    } else if (args.length == 1 && args[0] instanceof List) {
      out = (List<T>) args[0];
    } else {
      out = Arrays.asList(args);
    }
    return out;
  }

  /**
   * Translates a message.
   *
   * <p>Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param lang the message lang
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn't defined
   */
  public String get(play.api.i18n.Lang lang, String key, Object... args) {
    Seq<Object> scalaArgs = convertArgsToScalaBuffer(args);
    return messages.apply(key, scalaArgs, lang);
  }

  /**
   * Translates the first defined message.
   *
   * <p>Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param lang the message lang
   * @param keys the messages keys
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn't defined
   */
  public String get(play.api.i18n.Lang lang, List<String> keys, Object... args) {
    Buffer<String> keyArgs = scala.collection.JavaConverters.asScalaBufferConverter(keys).asScala();
    Seq<Object> scalaArgs = convertArgsToScalaBuffer(args);
    return messages.apply(keyArgs.toSeq(), scalaArgs, lang);
  }

  /**
   * Check if a message key is defined.
   *
   * @param lang the message lang
   * @param key the message key
   * @return a Boolean
   */
  public Boolean isDefinedAt(play.api.i18n.Lang lang, String key) {
    return messages.isDefinedAt(key, lang);
  }

  /**
   * Get a messages context appropriate for the given candidates.
   *
   * <p>Will select a language from the candidates, based on the languages available, and fallback
   * to the default language if none of the candidates are available.
   *
   * @param candidates the candidate languages
   * @return the most appropriate Messages instance given the candidate languages
   */
  public Messages preferred(Collection<Lang> candidates) {
    play.api.i18n.Messages msgs = messages.preferred(Scala.asScala(candidates));
    return new MessagesImpl(new Lang(msgs.lang()), this);
  }

  /**
   * Get a messages context appropriate for the given request.
   *
   * <p>Will select a language from the request, based on the languages available, and fallback to
   * the default language if none of the candidates are available.
   *
   * @param request the incoming request
   * @return the preferred messages context for the request
   */
  public Messages preferred(Http.RequestHeader request) {
    play.api.i18n.Messages msgs = messages.preferred(request);
    return new MessagesImpl(new Lang(msgs.lang()), this);
  }

  /**
   * Given a Result and a Lang, return a new Result with the lang cookie set to the given Lang.
   *
   * @param result the result where the lang will be set.
   * @param lang the lang to set on the result
   * @return a new result with the lang.
   */
  public Result setLang(Result result, Lang lang) {
    return messages.setLang(result.asScala(), lang).asJava();
  }

  /**
   * Given a Result, return a new Result with the lang cookie discarded.
   *
   * @param result the result to clear the lang.
   * @return a new result with a cleared lang.
   */
  public Result clearLang(Result result) {
    return messages.clearLang(result.asScala()).asJava();
  }

  /** Name for the language Cookie. */
  public String langCookieName() {
    return messages.langCookieName();
  }

  /** An optional max age in seconds for the language Cookie. */
  public OptionalInt langCookieMaxAge() {
    Option<Object> langCookieMaxAge = messages.langCookieMaxAge();
    return langCookieMaxAge.isEmpty()
        ? OptionalInt.empty()
        : OptionalInt.of((Integer) langCookieMaxAge.get());
  }

  /** Whether the secure attribute of the cookie is true or not. */
  public boolean langCookieSecure() {
    return messages.langCookieSecure();
  }

  /** Whether the HTTP only attribute of the cookie should be set to true or not. */
  public boolean langCookieHttpOnly() {
    return messages.langCookieHttpOnly();
  }

  /**
   * The value of the [[SameSite]] attribute of the cookie. If None, then no SameSite attribute is
   * set.
   */
  public Optional<Http.Cookie.SameSite> langCookieSameSite() {
    return OptionConverters.toJava(messages.langCookieSameSite()).map(Cookie.SameSite::asJava);
  }
}
