# Setup requirejs to have the right baseUrl
global.requirejs = require("requirejs")

requirejs.config(
  nodeRequire: require
  baseUrl: __dirname
)

# A few modules that all tests will use
global.Squire = requirejs("lib/squirejs/Squire")
global.assert = require("assert")
