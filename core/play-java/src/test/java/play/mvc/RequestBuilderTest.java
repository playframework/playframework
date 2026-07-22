/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import com.google.common.net.InetAddresses;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.apache.pekko.stream.javadsl.FileIO;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.Test;
import play.api.Application;
import play.api.Play;
import play.api.inject.guice.GuiceApplicationBuilder;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.Files;
import play.libs.Files.TemporaryFileCreator;
import play.libs.typedmap.TypedKey;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.test.Helpers;

public class RequestBuilderTest {

  @Test
  public void testUri_absolute() {
    Request request = new RequestBuilder().uri("https://www.benmccann.com/blog").build();
    assertEquals("https://www.benmccann.com/blog", request.uri());
  }

  @Test
  public void testUri_relative() {
    Request request = new RequestBuilder().uri("/blog").build();
    assertEquals("/blog", request.uri());
  }

  @Test
  public void testUri_asterisk() {
    Request request = new RequestBuilder().method("OPTIONS").uri("*").build();
    assertEquals("*", request.uri());
  }

  @Test
  public void testSecure() {
    RequestBuilder builder = new RequestBuilder();

    // Changing the target to https:// does not change the builder's default HTTP effective scheme.
    assertFalse(builder.uri("https://www.benmccann.com/blog").build().secure());
    // Make the build set secure
    assertTrue(builder.secure(true).build().secure());
    // Changing the target to http:// does not replace the explicitly selected HTTPS effective
    // scheme.
    assertTrue(builder.uri("http://www.benmccann.com/blog").build().secure());
    assertFalse(builder.secure(false).build().secure());
  }

  @Test
  public void testSchemeAndAuthorityValueTypes() throws Exception {
    Http.Scheme customScheme = new Http.Scheme("Git+SSH.v1-2");
    assertEquals("git+ssh.v1-2", customScheme.render());
    assertEquals(customScheme, customScheme.asScala().asJava());
    assertFalse(customScheme.isSecure());
    assertEquals(Http.Scheme.HTTPS, new Http.Scheme("HTTPS"));

    Http.RequestAuthority registered = Http.RequestAuthority.parse("EXAMPLE.%63om:00080");
    Http.RequestAuthority ipv4 = Http.RequestAuthority.parse("192.0.2.43:8080");
    Http.RequestAuthority ipv6 = Http.RequestAuthority.parse("[2001:0DB8::1]:443");
    Http.RequestAuthority mappedIpv6 = Http.RequestAuthority.parse("[::ffff:192.0.2.43]");
    Http.RequestAuthority ipvFuture = Http.RequestAuthority.parse("[VF.FOO:BAR]:8443");
    Http.RequestAuthority lowerIpvFuture = Http.RequestAuthority.parse("[vf.foo:bar]:8443");

    assertEquals("example.com:80", registered.render());
    assertTrue(registered.host() instanceof Http.AuthorityHost.RegName);
    assertTrue(ipv4.host() instanceof Http.AuthorityHost.IPv4);
    assertEquals("192.0.2.43:8080", ipv4.render());
    assertTrue(ipv6.host() instanceof Http.AuthorityHost.IPv6);
    assertEquals("[2001:db8::1]:443", ipv6.render());
    assertTrue(mappedIpv6.host() instanceof Http.AuthorityHost.IPv6);
    assertEquals("[::ffff:c000:22b]", mappedIpv6.render());
    assertTrue(ipvFuture.host() instanceof Http.AuthorityHost.IPvFuture);
    assertEquals("[vf.foo:bar]:8443", ipvFuture.render());
    assertEquals(lowerIpvFuture, ipvFuture);
    assertEquals(lowerIpvFuture.host(), ipvFuture.host());
    assertEquals(lowerIpvFuture.hashCode(), ipvFuture.hashCode());
    assertEquals(lowerIpvFuture.host().hashCode(), ipvFuture.host().hashCode());

    for (Http.RequestAuthority authority : List.of(registered, ipv4, ipv6, mappedIpv6, ipvFuture)) {
      assertEquals(authority, authority.asScala().asJava());
      assertEquals(authority.host().render(), authority.host().toString());
      assertEquals(authority.render(), authority.toString());
    }

    Http.AuthorityPort huge =
        new Http.AuthorityPort(new BigInteger("123456789012345678901234567890"));
    assertEquals(Optional.of(0), new Http.AuthorityPort(BigInteger.ZERO).tcpPort());
    assertEquals(Optional.of(65535), new Http.AuthorityPort(BigInteger.valueOf(65535)).tcpPort());
    assertEquals(Optional.empty(), new Http.AuthorityPort(BigInteger.valueOf(65536)).tcpPort());
    assertEquals(Optional.empty(), huge.tcpPort());
    assertEquals(huge, huge.asScala().asJava());
    assertEquals("example.com:" + huge.render(), registered.withPort(Optional.of(huge)).render());

    for (String invalid :
        List.of("[fe80::1%1]", "[fe80::1%eth0]", "[fe80::1%25eth0]", "１２７.０.０.１", "١٢٧.٠.٠.١")) {
      assertThatThrownBy(() -> Http.RequestAuthority.parse(invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }

    Inet6Address unscoped = (Inet6Address) InetAddresses.forString("fe80::1");
    for (int scope : List.of(0, 1)) {
      Inet6Address scoped = Inet6Address.getByAddress(null, unscoped.getAddress(), scope);
      assertThatThrownBy(() -> new Http.AuthorityHost.IPv6(scoped))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testSchemeAuthorityAndUriAreIndependentBuilderState() {
    Http.Scheme scheme = new Http.Scheme("Git+SSH");
    Http.RequestAuthority authority = Http.RequestAuthority.parse("PUBLIC.example:08443");
    RequestBuilder builder =
        new RequestBuilder()
            .scheme(scheme)
            .authority(authority)
            .uri("https://internal.example:9443/original?x=1");

    assertEquals("https://internal.example:9443/original?x=1", builder.uri());
    assertEquals(scheme, builder.scheme());
    assertEquals(Optional.of(authority), builder.authority());
    assertEquals("public.example:8443", builder.host());
    assertEquals(Optional.of("public.example:8443"), builder.headers().get(Http.HeaderNames.HOST));
    assertFalse(builder.secure());

    builder.path("/changed");
    assertEquals("/changed", builder.path());
    assertEquals(scheme, builder.scheme());
    assertEquals(Optional.of(authority), builder.authority());
    assertEquals("public.example:8443", builder.host());
  }

  @Test
  public void testRemoteInfoValueTypes() {
    Http.RemoteNode.Ip ip =
        new Http.RemoteNode.Ip(
            InetAddresses.forString("192.0.2.43"), Optional.of(new Http.NodePort.Numeric(53124)));
    Http.RemoteNode.Obfuscated by = new Http.RemoteNode.Obfuscated("_edge", Optional.empty());
    Http.RemoteInfo remote = new Http.RemoteInfo(ip, Optional.of(by));

    assertEquals(ip, remote.node());
    assertEquals(Optional.of(by), remote.byNode());
    assertEquals("192.0.2.43", remote.identity());
    assertEquals(Optional.of(InetAddresses.forString("192.0.2.43")), remote.ipAddress());
    assertEquals(Optional.of(new Http.NodePort.Numeric(53124)), remote.nodePort());
    assertEquals(Optional.of(53124), remote.port());
    assertEquals(remote, remote.asScala().asJava());
    assertEquals(new Http.RemoteInfo(ip, Optional.of(by)), remote);

    Http.RemoteInfo obfuscated =
        new Http.RemoteInfo(
            new Http.RemoteNode.Obfuscated(
                "_client", Optional.of(new Http.NodePort.Obfuscated("_port"))),
            Optional.empty());
    assertEquals("_client", obfuscated.identity());
    assertEquals(Optional.empty(), obfuscated.ipAddress());
    assertEquals(Optional.of(new Http.NodePort.Obfuscated("_port")), obfuscated.nodePort());
    assertEquals(Optional.empty(), obfuscated.port());
    assertEquals(obfuscated, obfuscated.asScala().asJava());

    Http.RemoteInfo unknown =
        new Http.RemoteInfo(new Http.RemoteNode.Unknown(Optional.empty()), Optional.empty());
    assertEquals("unknown", unknown.identity());
    assertEquals(Optional.empty(), unknown.ipAddress());
    assertEquals(Optional.empty(), unknown.port());
    assertEquals(unknown, unknown.asScala().asJava());

    assertThatThrownBy(() -> new Http.RemoteInfo(null, Optional.empty()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Http.RemoteInfo(ip, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Http.RemoteNode.Obfuscated("not-obfuscated", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testRemoteForwardingValueTypes() {
    Http.RemoteEndpoint selected =
        new Http.RemoteEndpoint(
            new Http.RemoteNode.Ip(InetAddresses.forString("203.0.113.43"), Optional.empty()),
            Optional.of(new Http.RemoteNode.Obfuscated("_edge", Optional.empty())));
    Http.RemoteEndpoint proxy =
        new Http.RemoteEndpoint(
            new Http.RemoteNode.Ip(InetAddresses.forString("192.0.2.10"), Optional.empty()),
            Optional.of(new Http.RemoteNode.Obfuscated("_internal", Optional.empty())));
    List<Http.RemoteEndpoint> mutableVia = new java.util.ArrayList<>(List.of(proxy));
    Http.ForwardingInfo forwarding =
        new Http.ForwardingInfo(Http.ForwardingSource.RFC_7239, mutableVia);
    Http.RemoteInfo remote =
        new Http.RemoteInfo(selected.node(), selected.byNode(), Optional.of(forwarding));

    mutableVia.clear();

    assertTrue(remote.isForwarded());
    assertEquals(Http.ForwardingSource.RFC_7239, remote.forwarding().orElseThrow().source());
    assertEquals(List.of(proxy), remote.forwarding().orElseThrow().via());
    assertEquals(List.of(selected, proxy), remote.path());
    assertEquals(remote, remote.asScala().asJava());
    assertThatThrownBy(
            () ->
                remote
                    .forwarding()
                    .orElseThrow()
                    .via()
                    .add(
                        new Http.RemoteEndpoint(
                            new Http.RemoteNode.Unknown(Optional.empty()), Optional.empty())))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> new Http.ForwardingInfo(null, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Http.ForwardingInfo(Http.ForwardingSource.X_FORWARDED, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Http.RemoteInfo(selected.node(), selected.byNode(), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testNodePortValidation() {
    assertEquals(0, new Http.NodePort.Numeric(0).value());
    assertEquals(65535, new Http.NodePort.Numeric(65535).value());
    assertEquals("_Edge.1_test-port", new Http.NodePort.Obfuscated("_Edge.1_test-port").value());

    assertThatThrownBy(() -> new Http.NodePort.Numeric(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Http.NodePort.Numeric(65536))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Http.NodePort.Obfuscated(null))
        .isInstanceOf(NullPointerException.class);
    for (String value : List.of("", "_", "port", "_bad value", "_bad!")) {
      assertThatThrownBy(() -> new Http.NodePort.Obfuscated(value))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testRemoteAndTransportAreIndependentBuilderState() {
    Http.RemoteInfo remote =
        new Http.RemoteInfo(
            new Http.RemoteNode.Obfuscated("_client", Optional.empty()), Optional.empty());
    Http.TransportConnection transport =
        new Http.TransportConnection(
            new Http.PeerEndpoint(InetAddresses.forString("192.0.2.10"), Optional.of(53124)),
            Optional.of(new Http.TransportTls(List.of())));

    Request request = new RequestBuilder().remote(remote).transport(transport).build();

    assertEquals(remote, request.remote());
    assertEquals(transport, request.transport());
    assertTrue(request.transport().tls().isPresent());
    assertEquals(List.of(), request.transport().tls().orElseThrow().peerCertificates());
    assertFalse(request.secure());
  }

  @Test
  public void testClientCertificateIsBuilderState() {
    X509Certificate leaf = mock(X509Certificate.class);
    X509Certificate intermediate = mock(X509Certificate.class);
    Http.ClientCertificateInfo certificate =
        new Http.ClientCertificateInfo(
            leaf, List.of(intermediate), Http.ClientCertificateSource.RFC_9440);

    RequestBuilder builder = new RequestBuilder().clientCertificate(certificate);
    Request request = builder.build();

    assertEquals(Optional.of(certificate), builder.clientCertificate());
    assertEquals(Optional.of(certificate), request.clientCertificate());
    assertEquals(certificate, request.asScala().clientCertificate().get().asJava());

    builder.clientCertificate(Optional.empty());
    assertEquals(Optional.empty(), builder.clientCertificate());
    assertEquals(Optional.empty(), builder.build().clientCertificate());
  }

  @Test
  public void testXForwardedClientCertificatesAreOrderedBuilderState() {
    Http.XForwardedClientCert client =
        new Http.XForwardedClientCert(
            List.of("spiffe://example/edge"),
            Optional.empty(),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of("spiffe://example/client"),
            List.of("client.example"));
    Http.XForwardedClientCert proxy =
        new Http.XForwardedClientCert(
            List.of("spiffe://example/sidecar"),
            Optional.empty(),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of("spiffe://example/proxy"),
            List.of());
    List<Http.XForwardedClientCert> mutableAssertions = new ArrayList<>(List.of(client, proxy));

    Request request = new RequestBuilder().xForwardedClientCertificates(mutableAssertions).build();
    mutableAssertions.clear();

    assertEquals(List.of(client, proxy), request.xForwardedClientCertificates());
    assertEquals(2, request.asScala().xForwardedClientCertificates().size());
    assertEquals(client, request.asScala().xForwardedClientCertificates().apply(0).asJava());
    assertThrows(
        UnsupportedOperationException.class,
        () -> request.xForwardedClientCertificates().add(client));
  }

  @Test
  public void testPathPreservesRawTargetAuthority() {
    RequestBuilder oversizedPort =
        new RequestBuilder()
            .uri("https://example.com:123456789012345678901234567890/old?x=1")
            .path("/changed path");
    assertEquals(
        "https://example.com:123456789012345678901234567890/changed%20path?x=1",
        oversizedPort.uri());

    RawTargetRequestBuilder ipvFuture =
        new RawTargetRequestBuilder()
            .rawTarget("https://[vF.FOO:BAR]:8443/old?x=1#fragment")
            .pathOnRawTarget("/changed");
    assertEquals("https://[vF.FOO:BAR]:8443/changed?x=1#fragment", ipvFuture.uri());
    assertEquals("/changed", ipvFuture.path());
  }

  @Test
  public void testAuthorityIsTheCanonicalHostState() {
    RequestBuilder builder = new RequestBuilder().host("EXAMPLE.com:00080");
    Http.Headers withoutHost = new Http.Headers(Map.of("X-Test", Collections.singletonList("one")));
    Http.Headers replacementHost =
        new Http.Headers(Map.of("host", Collections.singletonList("OTHER.example:00081")));
    Http.Headers duplicateHost =
        new Http.Headers(
            Map.of(Http.HeaderNames.HOST, List.of("example.com:80", "example.com:80")));
    Http.Headers emptyHost =
        new Http.Headers(Map.of(Http.HeaderNames.HOST, Collections.emptyList()));
    Http.Headers invalidHost =
        new Http.Headers(Map.of(Http.HeaderNames.HOST, Collections.singletonList("[invalid")));

    assertEquals("example.com:80", builder.host());
    assertEquals(Optional.of(Http.RequestAuthority.parse("example.com:80")), builder.authority());

    builder.headers(withoutHost);
    assertEquals(Optional.of("example.com:80"), builder.headers().get(Http.HeaderNames.HOST));

    builder.headers(replacementHost);
    assertEquals("other.example:81", builder.host());
    assertEquals(Optional.of("other.example:81"), builder.headers().get(Http.HeaderNames.HOST));

    builder.header("HOST", "THIRD.example:00082");
    assertEquals("third.example:82", builder.host());
    assertEquals(Optional.of("third.example:82"), builder.headers().get(Http.HeaderNames.HOST));

    builder.header("host", Collections.singletonList("[2001:0DB8::1]:00443"));
    assertEquals("[2001:db8::1]:443", builder.host());
    assertEquals(Optional.of("[2001:db8::1]:443"), builder.headers().get(Http.HeaderNames.HOST));

    assertThatThrownBy(() -> builder.headers(duplicateHost))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly one Host");
    assertThatThrownBy(() -> builder.headers(emptyHost))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly one Host");
    assertThatThrownBy(() -> builder.headers(invalidHost))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> builder.header("host", "[invalid"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> builder.header(Http.HeaderNames.HOST, List.of("one.example", "two.example")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly one Host");
    assertEquals("[2001:db8::1]:443", builder.host());

    builder.authority(Optional.empty());
    assertEquals(Optional.empty(), builder.authority());
    assertEquals("", builder.host());
    assertEquals(Optional.empty(), builder.headers().get(Http.HeaderNames.HOST));

    builder.header("Host", "RESTORED.example:00083");
    assertEquals(
        Optional.of(Http.RequestAuthority.parse("restored.example:83")), builder.authority());
    assertEquals(Optional.of("restored.example:83"), builder.headers().get(Http.HeaderNames.HOST));

    builder.authority(Http.RequestAuthority.parse("NEW.example"));
    assertEquals("new.example", builder.host());
    assertEquals(Optional.of("new.example"), builder.headers().get(Http.HeaderNames.HOST));
  }

  @Test
  public void testAttrs() {
    final TypedKey<Long> NUMBER = TypedKey.create("number");
    final TypedKey<String> COLOR = TypedKey.create("color");

    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");
    assertFalse(builder.attrs().containsKey(NUMBER));
    assertFalse(builder.attrs().containsKey(COLOR));

    Request req1 = builder.build();

    builder.attr(NUMBER, 6L);
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertFalse(builder.attrs().containsKey(COLOR));

    Request req2 = builder.build();

    builder.attr(NUMBER, 70L);
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertFalse(builder.attrs().containsKey(COLOR));

    Request req3 = builder.build();

    builder.attrs(builder.attrs().putAll(NUMBER.bindValue(6L), COLOR.bindValue("blue")));
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertTrue(builder.attrs().containsKey(COLOR));

    Request req4 = builder.build();

    builder.attrs(builder.attrs().putAll(COLOR.bindValue("red")));
    assertTrue(builder.attrs().containsKey(NUMBER));
    assertTrue(builder.attrs().containsKey(COLOR));

    Request req5 = builder.build();

    assertFalse(req1.attrs().containsKey(NUMBER));
    assertFalse(req1.attrs().containsKey(COLOR));

    assertEquals(Optional.of(6L), req2.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req2.attrs().get(NUMBER));
    assertFalse(req2.attrs().containsKey(COLOR));

    assertEquals(Optional.of(70L), req3.attrs().getOptional(NUMBER));
    assertEquals((Long) 70L, req3.attrs().get(NUMBER));
    assertFalse(req3.attrs().containsKey(COLOR));

    assertEquals(Optional.of(6L), req4.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req4.attrs().get(NUMBER));
    assertEquals(Optional.of("blue"), req4.attrs().getOptional(COLOR));
    assertEquals("blue", req4.attrs().get(COLOR));

    assertEquals(Optional.of(6L), req5.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req5.attrs().get(NUMBER));
    assertEquals(Optional.of("red"), req5.attrs().getOptional(COLOR));
    assertEquals("red", req5.attrs().get(COLOR));

    Request req6 = req4.removeAttr(COLOR).removeAttr(NUMBER);

    assertFalse(req6.attrs().containsKey(NUMBER));
    assertFalse(req6.attrs().containsKey(COLOR));

    Request req7 = req4.removeAttr(COLOR);

    assertEquals(Optional.of(6L), req7.attrs().getOptional(NUMBER));
    assertEquals((Long) 6L, req7.attrs().get(NUMBER));
    assertFalse(req7.attrs().containsKey(COLOR));
  }

  @Test
  public void testNewRequestsShouldNotHaveATransientLang() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Request request = builder.build();
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testAddATransientLangToRequest() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.build().withTransientLang(lang);

    assertTrue(request.transientLang().isPresent());
    assertEquals(lang, request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByCodeToRequest() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    String lang = "de";
    Request request = builder.build().withTransientLang(Lang.forCode(lang));

    assertTrue(request.transientLang().isPresent());
    assertEquals(Lang.forCode(lang), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByLocaleToRequest() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Locale locale = Locale.GERMAN;
    Request request = builder.build().withTransientLang(locale);

    assertTrue(request.transientLang().isPresent());
    assertEquals(new Lang(locale), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testClearRequestTransientLang() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.build().withTransientLang(lang);
    assertTrue(request.transientLang().isPresent());

    // Language attr should be removed
    assertFalse(request.withoutTransientLang().transientLang().isPresent());
  }

  @Test
  public void testAddATransientLangToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.transientLang(lang).build();

    assertTrue(request.transientLang().isPresent());
    assertEquals(lang, request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByCodeToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    String lang = "de";
    Request request = builder.transientLang(Lang.forCode(lang)).build();

    assertTrue(request.transientLang().isPresent());
    assertEquals(Lang.forCode(lang), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testAddATransientLangByLocaleToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Locale locale = Locale.GERMAN;
    Request request = builder.transientLang(locale).build();

    assertTrue(request.transientLang().isPresent());
    assertEquals(new Lang(locale), request.attrs().get(Messages.Attrs.CurrentLang));
  }

  @Test
  public void testClearRequestBuilderTransientLang() {
    Lang lang = new Lang(Locale.GERMAN);
    RequestBuilder builder =
        new RequestBuilder().uri("http://www.playframework.com/").transientLang(lang);

    assertTrue(builder.build().transientLang().isPresent());
    assertEquals(Optional.of(lang), builder.transientLang());

    // Language attr should be removed
    assertFalse(builder.withoutTransientLang().build().transientLang().isPresent());
  }

  @Test
  public void testNewRequestsShouldNotHaveALangCookie() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Request request = builder.build();
    assertFalse(request.cookie(Helpers.stubMessagesApi().langCookieName()).isPresent());
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testAddALangCookieToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Lang lang = new Lang(Locale.GERMAN);
    Request request = builder.langCookie(lang, Helpers.stubMessagesApi()).build();

    assertEquals(
        Optional.of(lang.code()),
        request.cookie(Helpers.stubMessagesApi().langCookieName()).map(Http.Cookie::value));
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testAddALangCookieByLocaleToRequestBuilder() {
    RequestBuilder builder = new RequestBuilder().uri("http://www.playframework.com/");

    Locale locale = Locale.GERMAN;
    Request request = builder.langCookie(locale, Helpers.stubMessagesApi()).build();

    assertEquals(
        Optional.of(locale.toLanguageTag()),
        request.cookie(Helpers.stubMessagesApi().langCookieName()).map(Http.Cookie::value));
    assertFalse(request.transientLang().isPresent());
    assertFalse(request.attrs().getOptional(Messages.Attrs.CurrentLang).isPresent());
  }

  @Test
  public void testFlash() {
    final Request req =
        new RequestBuilder().flash("a", "1").flash("b", "1").flash("b", "2").build();
    assertEquals(Optional.of("1"), req.flash().get("a"));
    assertEquals(Optional.of("2"), req.flash().get("b"));
  }

  @Test
  public void testSession() {
    final Request req =
        new RequestBuilder().session("a", "1").session("b", "1").session("b", "2").build();
    assertEquals(Optional.of("1"), req.session().get("a"));
    assertEquals(Optional.of("2"), req.session().get("b"));
  }

  @Test
  public void testUsername() {
    final Request req1 = new RequestBuilder().uri("http://playframework.com/").build();
    final Request req2 = req1.addAttr(Security.USERNAME, "user2");
    final Request req3 = req1.addAttr(Security.USERNAME, "user3");
    final Request req4 =
        new RequestBuilder()
            .uri("http://playframework.com/")
            .attr(Security.USERNAME, "user4")
            .build();

    assertFalse(req1.attrs().containsKey(Security.USERNAME));

    assertTrue(req2.attrs().containsKey(Security.USERNAME));
    assertEquals("user2", req2.attrs().get(Security.USERNAME));

    assertTrue(req3.attrs().containsKey(Security.USERNAME));
    assertEquals("user3", req3.attrs().get(Security.USERNAME));

    assertTrue(req4.attrs().containsKey(Security.USERNAME));
    assertEquals("user4", req4.attrs().get(Security.USERNAME));
  }

  @Test
  public void testGetQuery_doubleEncoding() {
    final Optional<String> query =
        new Http.RequestBuilder().uri("path?query=x%2By").build().queryString("query");
    assertEquals(Optional.of("x+y"), query);
  }

  @Test
  public void testQuery_doubleEncoding() {
    final Optional<String> query =
        new Http.RequestBuilder().uri("path?query=x%2By").build().queryString("query");
    assertEquals(Optional.of("x+y"), query);
  }

  @Test
  public void testGetQuery_multipleParams() {
    final Request req = new Http.RequestBuilder().uri("/path?one=1&two=a+b&").build();
    assertEquals(Optional.of("1"), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
  }

  @Test
  public void testQuery_multipleParams() {
    final Request req = new Http.RequestBuilder().uri("/path?one=1&two=a+b&").build();
    assertEquals(Optional.of("1"), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
  }

  @Test
  public void testQuery_emptyParam() {
    final Request req = new Http.RequestBuilder().uri("/path?one=&two=a+b&").build();
    assertEquals(Optional.of(""), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
    assertEquals(Optional.empty(), req.queryString("three"));
  }

  @Test
  public void testQuery_noValueParam() {
    final Request req = new Http.RequestBuilder().uri("/path?one&two=a+b&").build();
    assertEquals(Optional.of(""), req.queryString("one"));
    assertEquals(Optional.of("a b"), req.queryString("two"));
    assertEquals(Optional.empty(), req.queryString("three"));
  }

  @Test
  public void testQuery_keyDecoding() {
    final Request req = new Http.RequestBuilder().uri("/path?one?%2B=&two%3D").build();
    assertEquals(Optional.of(""), req.queryString("one?+"));
    assertEquals(Optional.of(""), req.queryString("two="));
    assertEquals(Optional.empty(), req.queryString("three"));
  }

  @Test
  public void testGetUri_badEncoding() {
    final Request req =
        new Http.RequestBuilder().uri("/test.html?one=hello=world&two=false").build();
    assertEquals(Optional.of("hello=world"), req.queryString("one"));
    assertEquals(Optional.of("false"), req.queryString("two"));
  }

  @Test
  public void testUri_badEncoding() {
    final Request req =
        new Http.RequestBuilder().uri("/test.html?one=hello=world&two=false").build();
    assertEquals(Optional.of("hello=world"), req.queryString("one"));
    assertEquals(Optional.of("false"), req.queryString("two"));
  }

  @Test
  public void multipartForm() throws ExecutionException, InterruptedException {
    Application app = new GuiceApplicationBuilder().build();
    Play.start(app);
    TemporaryFileCreator temporaryFileCreator =
        app.injector().instanceOf(TemporaryFileCreator.class);
    Http.MultipartFormData.DataPart dp = new Http.MultipartFormData.DataPart("hello", "world");
    final Request request =
        new RequestBuilder()
            .uri("http://playframework.com/")
            .bodyRaw(Collections.singletonList(dp), temporaryFileCreator, app.materializer())
            .build();

    Optional<Http.MultipartFormData<Files.TemporaryFile>> parts =
        app.injector()
            .instanceOf(BodyParser.MultipartFormData.class)
            .apply(request)
            .run(Source.single(request.body().asBytes()), app.materializer())
            .toCompletableFuture()
            .get()
            .right;
    assertTrue(parts.isPresent());
    assertArrayEquals(new String[] {"world"}, parts.get().asFormUrlEncoded().get("hello"));

    Play.stop(app);
  }

  @Test
  public void multipartForm_bodyRaw_correctEscapedParams() throws URISyntaxException, IOException {
    Application app = new GuiceApplicationBuilder().build();
    Play.start(app);

    File file = new File(this.getClass().getResource("/testassets/foo.txt").toURI());
    Http.MultipartFormData.Part<Source<ByteString, ?>> filePart =
        new Http.MultipartFormData.FilePart<>(
            "f\"i\rl\nef\"ie\nld\r1",
            "f\rir\"s\ntf\ril\"e\n.txt",
            "text/plain",
            FileIO.fromPath(file.toPath()),
            java.nio.file.Files.size(file.toPath()));

    Http.MultipartFormData.DataPart dataPart =
        new Http.MultipartFormData.DataPart("f\ni\re\"l\nd1", "value1");

    TemporaryFileCreator temporaryFileCreator =
        app.injector().instanceOf(TemporaryFileCreator.class);
    final Request request =
        new RequestBuilder()
            .uri("http://playframework.com/")
            // bodyRaw, as its name tells us, saves the body in raw bytes.
            // To do that it needs to render the body, so bodyRaw(...) goes through
            // play.mvc.MultipartFormatter#transform(...), so eventually
            // play.core.formatters.Multipart, which renders the multipart/form-data elements and
            // escapes params, will be used
            .bodyRaw(List.of(dataPart, filePart), temporaryFileCreator, app.materializer())
            .build();

    String body =
        request.body().asBytes().utf8String(); // Let's get the text representation of the bytes
    assertThat(body)
        .contains("Content-Disposition: form-data; name=\"f%0Ai%0De%22l%0Ad1\"")
        .contains(
            "Content-Disposition: form-data; name=\"f%22i%0Dl%0Aef%22ie%0Ald%0D1\";"
                + " filename=\"f%0Dir%22s%0Atf%0Dil%22e%0A.txt\"");

    Play.stop(app);
  }

  @Test
  public void multipartFormContentLength() {
    final Map<String, String[]> dataParts = new HashMap<>();
    dataParts.put("f\ni\re\"l\nd1", new String[] {"value1"});
    dataParts.put("field2", new String[] {"value2-1", "value2.2"});

    final List<Http.MultipartFormData.FilePart> fileParts = new ArrayList<>();
    fileParts.add(
        new Http.MultipartFormData.FilePart<>(
            "f\"i\rl\nef\"ie\nld\r1", "f\rir\"s\ntf\ril\"e\n.txt", "text/plain", "abc", 3));
    fileParts.add(
        new Http.MultipartFormData.FilePart<>(
            "file_field_2", "secondfile.txt", "text/plain", "hello world", 11));

    final Request request =
        new RequestBuilder()
            .uri("http://playframework.com/")
            .bodyMultipart(dataParts, fileParts)
            .build();

    assertNotNull(request.body().asMultipartFormData());
    assertEquals(dataParts, request.body().asMultipartFormData().asFormUrlEncoded());
    assertEquals(fileParts, request.body().asMultipartFormData().getFiles());

    // Now let's check the calculated Content-Length. The request body should look like this when
    // stringified:
    // (You can copy the lines, save it with an editor with UTF-8 encoding and Windows line endings
    // (\r\n) and the file size should be 590 bytes
    /*
    --somerandomboundary
    Content-Disposition: form-data; name="f%0Ai%0De%22l%0Ad1"

    value1
    --somerandomboundary
    Content-Disposition: form-data; name="field2[]"

    value2-1
    --somerandomboundary
    Content-Disposition: form-data; name="field2[]"

    value2.2
    --somerandomboundary
    Content-Disposition: form-data; name="f%22i%0Dl%0Aef%22ie%0Ald%0D1"; filename="f%0Dir%22s%0Atf%0Dil%22e%0A.txt"
    Content-Type: text/plain

    abc
    --somerandomboundary
    Content-Disposition: form-data; name="file_field_2"; filename="secondfile.txt"
    Content-Type: text/plain

    hello world
    --somerandomboundary--
    */
    assertEquals(request.header(Http.HeaderNames.CONTENT_LENGTH).get(), "590");
  }

  private static final class RawTargetRequestBuilder extends RequestBuilder {
    private RawTargetRequestBuilder rawTarget(String uri) {
      req = req.withTarget(req.target().withUriString(uri).withPath("/old"));
      return this;
    }

    private RawTargetRequestBuilder pathOnRawTarget(String path) {
      path(path);
      return this;
    }
  }
}
