#!/bin/bash

# #context
export PW=`cat password`

# Export example.com's public certificate for use with nginx.
keytool -export -v \
  -alias example.com \
  -file example.com.crt \
  -keypass:env PW \
  -storepass:env PW \
  -keystore example.com.jks \
  -rfc

# Create a PKCS#12 keystore containing the public and private keys.
keytool -importkeystore -v \
  -srcalias example.com \
  -srckeystore example.com.jks \
  -srcstoretype jks \
  -srcstorepass:env PW \
  -destkeystore example.com.p12 \
  -destkeypass:env PW \
  -deststorepass:env PW \
  -deststoretype PKCS12

# Export the example.com private key for use in nginx.  Note this requires the use of OpenSSL.
openssl pkcs12 \
  -nocerts \
  -nodes \
  -passout env:PW \
  -passin env:PW \
  -in example.com.p12 \
  -out example.com.key

# Clean up.
rm example.com.p12
# #context
