# The Play data validation library

## Overview

The Play validation API aims to provide a comprehensive toolkit to parse data of any format, and validate them against user defined rules.

It's also a unification of the Form Validation API, and the Json validation API.
Being based on the same concepts as the Json validation API available in previous versions, it should feel very similar to any developer already working with the Json API. The validation API is, rather than a totally new design, a simple generalization of those concepts.

## Design

The validation API is designed around a core defined in package `play.api.data.mapping`, and "extensions", each providing primitives to validate and serialize data from / to a particular format ([[Json | ScalaValidationJson]], [[form encoded request body | ScalaValidationForm]], etc.). See [[the extensions documentation | ScalaValidationExtensions]] for more informations.

TODO: links to the doc
