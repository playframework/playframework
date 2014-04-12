#!/bin/bash

# #context
export PW=`cat password`

# Create a JKS keystore that trusts the example CA, with the default password.
keytool -import -v \
  -alias exampleca \
  -file exampleca.crt \
  -keypass:env PW \
  -storepass changeit \
  -keystore exampletrust.jks << EOF
yes
EOF

# List out the details of the store password.
keytool -list -v \
  -keystore exampletrust.jks \
  -storepass changeit

# #context