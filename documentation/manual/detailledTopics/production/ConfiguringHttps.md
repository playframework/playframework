# Configuring HTTPS

Play can configured to serve HTTPS.  To enable this, simply tell Play which port to listen using the `https.port` system property.  For example:

    ./start -Dhttps.port=9443

## SSL Certificates

By default, Play will generate itself a self signed certificate, however typically this will not be suitable for serving a website.  Play uses Java key stores to configure SSL certificates and keys.

Signing authorities often provide instructions on how to create a Java keystore (typically with reference to Tomcat configuration).  The official Oracle documentation on how to generate keystores using the JDK keytool utility can be found [here](http://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html).

Having created your keystore, the following system properties can be used to configure Play to use it:

* **https.keyStore** - The path to the keystore containing the private key and certificate, if not provided generates a keystore for you
* **https.keyStoreType** - The key store type, defaults to `JKS`
* **https.keyStorePassword** - The password, defaults to a blank password
* **https.keyStoreAlgorithm** - The key store algorithm, defaults to the platforms default algorithm

An example of using these properties might be:

    ./start -Dhttps.port=9443 -Dhttps.keyStore=/path/to/keystore -Dhttps.keyStorePassword=changeme
