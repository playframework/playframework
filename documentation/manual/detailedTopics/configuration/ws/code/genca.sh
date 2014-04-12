#!/bin/bash

# #context
export PW=`cat password`

# Create a self signed key pair root CA certificate.
keytool -genkeypair -v \
  -alias exampleca \
  -dname "CN=exampleCA, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US" \
  -keystore exampleca.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 4096 \
  -ext KeyUsage:critical="keyCertSign" \
  -ext BasicConstraints:critical="ca:true" \
  -validity 9999

# Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
keytool -export -v \
  -alias exampleca \
  -file exampleca.crt \
  -keypass:env PW \
  -storepass:env PW \
  -keystore exampleca.jks \
  -rfc
# #context
