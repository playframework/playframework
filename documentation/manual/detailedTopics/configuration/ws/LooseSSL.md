# Loose Options

## We understand

Setting up SSL is not all that fun.  It is not lost on anyone that setting up even a single web service with HTTPS involves setting up several certificates, reading boring cryptography documentation and dire warnings.

Despite that, all the security features in SSL are there for good reasons, and turning them off, even for development purposes, has led to even less fun than setting up SSL properly.  

## Please read this before turning anything off!

### Man in the Middle attacks are well known

The information security community is very well aware of how insecure most internal networks are, and uses that to their advantage.  A video discussing attacks detailed a [wide range of possible attacks](http://2012.video.sector.ca/page/6).

### Man in the Middle attacks are common

The average company can expect to have seven or eight [Man in the Middle](https://sites.google.com/site/cse825maninthemiddle/) attacks a year.  In some cases, up to 300,000 users can be compromised [over several months](https://security.stackexchange.com/questions/12041/are-man-in-the-middle-attacks-extremely-rare).

### Attackers have a suite of tools that automatically exploit flaws

The days of the expert hacker are over.  Most security professionals use automated linux environments such as Kali Linux to do penetration testing, packed with hundreds of tools to check for exploits.  A video of [Cain & Abel](https://www.youtube.com/watch?v=pfHsRscy540) shows passwords being compromised in less than 20 seconds.

Hackers won't bother to see whether something will "look encrypted" or not.  Instead, they'll set up a machine with a toolkit that will run through every possible exploit, and go out for coffee.

### Security is increasingly important and public

More and more information flows through computers every day.  The public and the media are taking increasing notice of the possibility that their private communications can be intercepted.  Google, Facebook, Yahoo, and other leading companies have made secure communication a priority and have devoted millions to ensuring that [data cannot be read](https://www.eff.org/deeplinks/2013/11/encrypt-web-report-whos-doing-what).

### Ethernet / Password protected WiFi does not provide a meaningful level of security.

A networking auditing tool such as a [Wifi Pineapple](https://wifipineapple.com/) costs around $100, picks up all traffic sent over a wifi network, and is so good at intercepting traffic that people have turned it on and started [intercepting traffic accidentally](http://www.troyhunt.com/2013/04/the-beginners-guide-to-breaking-website.html).

### Companies have been sued for inadequate security

PCI compliance is not the only thing that companies have to worry about.  The FTC sued [Fandango and Credit Karma](http://www.ftc.gov/news-events/press-releases/2014/03/fandango-credit-karma-settle-ftc-charges-they-deceived-consumers) on charges that they failed to securely transmit information, including credit card information.

### Correctly configured HTTPS clients are important

Sensitive, company confidential information goes over web services.  A paper discussing insecurities in WS clients was titled [The Most Dangerous Code in the World: Validating SSL Certificates in Non-Browser Software](http://www.cs.utexas.edu/~shmat/shmat_ccs12.pdf), and lists poor default configuration and explicit disabling of security options as the primary reason for exposure.  The WS client has been configured as much as possible to be secure by default, and there are example configurations provided for your benefit.

## Mitigation

If you must turn on loose options, there are a couple of things you can do to minimize your exposure.

**Custom WSClient**: You can create a [[custom WSClient|ScalaWS]] specifically for the server, using the [`DefaultWSConfigParser`](api/scala/index.html#play.api.libs.ws.DefaultWSConfigParser) together with `ConfigFactory.parseString`, and ensure it is never used outside that context.

**Environment Scoping**: You can define [environment variables in HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md#substitution-fallback-to-environment-variables) to ensure that any loose options are not hardcoded in configuration files, and therefore cannot escape an development environment.

**Runtime / Deployment Checks**: You can add code to your deployment scripts or program that checks that `ws.ssl.loose` options are not enabled in a production environment.  The runtime mode can be found in the [`Application.mode`](api/scala/index.html#play.api.Application) method.

## Loose Options

Finally, here are the options themselves.

### Disabling Certificate Verification

> **NOTE**: In most cases, people turn off certificate verification because they haven't generated certificates.  **There are other options besides disabling certificate verification.**
>
> * [[Quick Start to WS SSL|WSQuickStart]] shows how to connect directly to a server using a self signed certificate.
> * [[Generating X.509 Certificates|CertificateGeneration]] lists a number of GUI applications that will generate certificates for you.
> * [[Example Configurations|ExampleSSLConfig]] shows complete configuration of TLS using self signed certificates.
> * If you want to view your application through HTTPS, you can use [ngrok](https://ngrok.com/) to proxy your application.
> * If you need a certificate authority but don't want to pay money, [StartSSL](https://www.startssl.com/?app=1) or [CACert](http://www.cacert.org/) will give you a free certificate.
* If you want a self signed certificate and private key without typing on the command line, you can use [selfsignedcertificate.com](http://www.selfsignedcertificate.com/).

If you've read the above and you still want to completely disable certificate verification, set the following;

```
play.ws.ssl.loose.acceptAnyCertificate=true
```

With certificate verification completely disabled, you are vulnerable to attack from anyone on the network using a tool such as [mitmproxy](http://mitmproxy.org/).

### Disabling Weak Ciphers Checking

There are some ciphers which are known to have flaws, and are [disabled](http://sim.ivi.co/2011/08/jsse-oracle-provider-default-disabled.html) in 1.7.  WS will throw an exception if a weak cipher is found in the `ws.ssl.enabledCiphers` list.  If you specifically want a weak cipher, set this flag:

```
play.ws.ssl.loose.allowWeakCiphers=true
```

With weak cipher checking disabled, you are vulnerable to attackers that use forged certificates, such as [Flame](http://arstechnica.com/security/2012/06/flame-crypto-breakthrough/).

### Disabling Hostname Verification

If you want to disable hostname verification, you can set a loose flag:

```
play.ws.ssl.loose.disableHostnameVerification=true
```

With hostname verification disabled, a DNS proxy such as `dnschef` can [easily intercept communication](http://tersesystems.com/2014/03/31/testing-hostname-verification/).

### Disabled Protocols

WS recognizes "SSLv3", "SSLv2" and "SSLv2Hello" as weak protocols with a number of [security issues](https://www.schneier.com/paper-ssl.pdf), and will throw an exception if they are in the `ws.ssl.enabledProtocols` list.  Virtually all servers support `TLSv1`, so there is no advantage in using these older protocols.

If you specifically want a weak protocol, set the loose flag to disable the check:

```
play.ws.ssl.loose.allowWeakProtocols=true
```

SSLv2 and SSLv2Hello (there is no v1) are obsolete and usage in the field is [down to 25% on the public Internet](https://www.trustworthyinternet.org/ssl-pulse/).  SSLv3 is known to have [security issues](http://www.yaksman.org/~lweith/ssl.pdf) compared to TLS.  The only reason to turn this on is if you are connecting to a legacy server, but doing so does not make you vulnerable per se.

> **Next**:  [[Testing SSL|TestingSSL]]
