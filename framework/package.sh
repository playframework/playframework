#!/bin/bash
#
# Author: Jonathan Dray <jonathan.dray@gmail.com> aka Spiroid
# License: Apache 2
#          Licensed under the Apache License, Version 2.0 (the "License"); 
#          you may not use this project except in compliance with the License.
#          You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
#
# This package script is a rewrite of the one shipped with play2
# It launches the compilation process and generates a "production ready" archive
#
# See the usage section below


# Checking given parameters
if [ -z $1 ]
then
	echo "Usage: package.sh <version>"
	echo -e "Example: package.sh 2.1-SNAPSHOT"
	echo -e "         package.sh 2.1-SNAPSHOT incremental"
	echo -e "         package.sh test"
	exit 1
fi

set -e

SCRIPT_DIR=`dirname $0`
export PLAY_VERSION=$1


# Define helper functions
# -------------------------- #
displaymessage() {
  echo "[info] $*"
}


# First parameter: MESSAGE
# Others parameters: COMMAND (! not |)
displayandexec() {
  local message=$1
  displaymessage $message
  shift
  #$* >> $LOG_FILE 2>&1
  $*
  local ret=$?
  if [ $ret -ne 0 ]; then
    echo -e "\r\e[0;31m   [ERROR]\e[0m $message"
  else
    echo -e "\r\e[0;32m      [OK]\e[0m $message"
  fi 
  return $ret
}
# -------------------------- #


# Clean if needed
if [[ "$1" != "test" && "$2" != "incremental" ]]; then
  displaymessage "Cleaning previous builds artifacts"
  ${SCRIPT_DIR}/cleanEverything
  ${SCRIPT_DIR}/build clean
fi


# Building 
displayandexec "Building play" ${SCRIPT_DIR}/build -Dplay.version=${PLAY_VERSION} publish-local build-repository api-docs


# Removing previously generated play packages
displayandexec "Cleaning previous package" rm -rf /tmp/play-*


# Copying the current build to the temp directory
displayandexec "Copying to /tmp/play-${PLAY_VERSION}..." cp -rf ${SCRIPT_DIR}/.. /tmp/play-${PLAY_VERSION}
cd /tmp


# Replace version number in sbt config files
displaymessage "Fixing version"
sed -i -e '7s/version: .*/version: '$PLAY_VERSION'/' play-${PLAY_VERSION}/framework/sbt/play.boot.properties
sed -i -e 's/PLAY_VERSION=.*/PLAY_VERSION="'$PLAY_VERSION'"/' play-${PLAY_VERSION}/framework/build
sed -i -e 's/PLAY_VERSION=.*/PLAY_VERSION="'$PLAY_VERSION'"/' play-${PLAY_VERSION}/framework/build.bat


# Generating the final archive
displayandexec "Creating archive..." tar --exclude='*~' --exclude='*.lock' --exclude='*.history' --exclude='*.DS_Store' --exclude='*.gitmodules' --exclude='.gitignore' --exclude='.git' --exclude='framework/sbt/boot' --exclude='repository/cache' --exclude='samples/workinprogress' --exclude='*.log' --exclude='*/dist' --exclude='*/target' --exclude='*/logs' --exclude='*/tmp' --exclude='*/project/project' -jcvf play-${PLAY_VERSION}.tar.bz2 play-${PLAY_VERSION}

displayandexec "Cleaning temporary directory" rm -rf /tmp/play-${PLAY_VERSION} 
displaymessage "Package is available at /tmp/play-${PLAY_VERSION}.tar.bz2"
