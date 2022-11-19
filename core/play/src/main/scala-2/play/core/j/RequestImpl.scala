/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.j

import java.util
import java.util.Locale

import play.api.mvc.Request
import play.i18n
import play.libs.typedmap.TypedEntry
import play.libs.typedmap.TypedKey
import play.libs.typedmap.TypedMap
import play.mvc.Http.{ Request => JRequest }
import play.mvc.Http.{ RequestImpl => JRequestImpl }
import play.mvc.Http.RequestBody

/**
 * trait needed as workaround for https://github.com/scala/bug/issues/11944
 * Also see original pull request: https://github.com/playframework/playframework/pull/10199
 * sealed so that lack of implementation can't be accidentally used elsewhere
 */
private[j] sealed trait RequestImplHelper extends JRequest {
  override def addAttrs(entries: TypedEntry[_]*): JRequest = ???
}

class RequestImpl(request: Request[RequestBody]) extends RequestHeaderImpl(request) with RequestImplHelper {
  override def asScala: Request[RequestBody] = request

  override def attrs: TypedMap = new TypedMap(asScala.attrs)

  override def withAttrs(newAttrs: TypedMap): JRequest = new JRequestImpl(request.withAttrs(newAttrs.asScala))

  override def addAttr[A](key: TypedKey[A], value: A): JRequest = withAttrs(attrs.put(key, value))

  override def addAttrs(e1: TypedEntry[_]): JRequest = withAttrs(attrs.putAll(e1))

  override def addAttrs(e1: TypedEntry[_], e2: TypedEntry[_]): JRequest = withAttrs(attrs.putAll(e1, e2))

  override def addAttrs(e1: TypedEntry[_], e2: TypedEntry[_], e3: TypedEntry[_]): JRequest =
    withAttrs(attrs.putAll(e1, e2, e3))

  override def addAttrs(entries: TypedEntry[_]*): JRequest = withAttrs(attrs.putAll(entries: _*))

  override def addAttrs(entries: util.List[TypedEntry[_]]): JRequest = withAttrs(attrs.putAll(entries))

  override def removeAttr(key: TypedKey[_]): JRequest = withAttrs(attrs.remove(key))

  override def body: RequestBody = request.body

  override def hasBody: Boolean = request.hasBody

  override def withBody(body: RequestBody): JRequest = new JRequestImpl(request.withBody(body))

  override def withTransientLang(lang: play.i18n.Lang): JRequest =
    addAttr(i18n.Messages.Attrs.CurrentLang, lang)

  @deprecated
  override def withTransientLang(code: String): JRequest =
    withTransientLang(play.i18n.Lang.forCode(code))

  override def withTransientLang(locale: Locale): JRequest =
    withTransientLang(new play.i18n.Lang(locale))

  override def withoutTransientLang(): JRequest =
    removeAttr(i18n.Messages.Attrs.CurrentLang)
}
