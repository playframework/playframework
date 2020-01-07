#!/usr/bin/env bash

# Copyright (C) Lightbend Inc. <https://www.lightbend.com>

# shellcheck source=scripts/scriptLib
. "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/scriptLib"

start validation "RUNNING FRAMEWORK VALIDATION"
sbt headerCheck test:headerCheck javafmtCheckAll scalafmtCheckAll scalafmtSbtCheck
start validation "FRAMEWORK VALIDATION DONE"

pushd "$DOCUMENTATION"
start doc-validation "RUNNING DOCUMENTATION VALIDATION"
sbt headerCheck test:headerCheck javafmtCheckAll scalafmtCheckAll scalafmtSbtCheck || (
  echo "WARN: Format and/or license headers validaton failed."
  echo "You need to run in sbt ';headerCreate ;test:headerCreate ;javafmtAll ;scalafmtAll ;scalafmtSbt'"
  echo "then commit the new changes or amend the existing commit."
  echo "See more information about amending commits in our docs:"
  echo "https://playframework.com/documentation/latest/WorkingWithGit"
  false
)

popd

end doc-validation "ALL VALIDATIONS DONE"
