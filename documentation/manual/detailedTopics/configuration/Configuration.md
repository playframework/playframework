<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuration file syntax and features

> The configuration file used by Play is based on the [Typesafe config library](https://github.com/typesafehub/config).

The default configuration file of a Play application must be defined in `conf/application.conf`. It uses the HOCON format ( "Human-Optimized Config Object Notation").

## Specifying an alternative configuration file

System properties can be used to force a different config source:

* `config.resource` specifies a resource name including the extension, i.e. `application.conf` and not just `application`
* `config.file` specifies a filesystem path, again it should include the extension, not be a basename
* `config.url` specifies a URL

These system properties specify a replacement for `application.conf`, not an addition. In the replacement config file, you can use include "application" to include the original default config file; after the include statement you could go on to override certain settings.

## Using with Akka

Akka 2.0 will use the same configuration file as the one defined for your Play application. Meaning that you can configure anything in Akka in the `application.conf` directory.

## HOCON Syntax

Much of this is defined with reference to JSON; you can find the JSON spec at http://json.org/ of course.

### Unchanged from JSON

 - files must be valid UTF-8
 - quoted strings are in the same format as JSON strings
 - values have possible types: string, number, object, array, boolean, null
 - allowed number formats matches JSON; as in JSON, some possible
   floating-point values are not represented, such as `NaN`

### Comments

Anything between `//` or `#` and the next newline is considered a comment and ignored, unless the `//` or `#` is inside a quoted string.

### Omit root braces

JSON documents must have an array or object at the root. Empty files are invalid documents, as are files containing only a non-array non-object value such as a string.

In HOCON, if the file does not begin with a square bracket or curly brace, it is parsed as if it were enclosed with `{}` curly braces.

A HOCON file is invalid if it omits the opening `{` but still has a closing `}`; the curly braces must be balanced.

### Key-value separator

The `=` character can be used anywhere JSON allows `:`, i.e. to separate keys from values.

If a key is followed by `{`, the `:` or `=` may be omitted. So `"foo" {}` means `"foo" : {}"`

### Commas

Values in arrays, and fields in objects, need not have a comma between them as long as they have at least one ASCII newline (`\n`, decimal value 10) between them.

The last element in an array or last field in an object may be followed by a single comma. This extra comma is ignored.

 - `[1,2,3,]` and `[1,2,3]` are the same array.
 - `[1\n2\n3]` and `[1,2,3]` are the same array.
 - `[1,2,3,,]` is invalid because it has two trailing commas.
 - `[,1,2,3]` is invalid because it has an initial comma.
 - `[1,,2,3]` is invalid because it has two commas in a row.
 - these same comma rules apply to fields in objects.

### Duplicate keys

The JSON spec does not clarify how duplicate keys in the same object should be handled. In HOCON, duplicate keys that appear later override those that appear earlier, unless both values are objects. If both values are objects, then the objects are merged.

Note: this would make HOCON a non-superset of JSON if you assume that JSON requires duplicate keys to have a behavior. The assumption here is that duplicate keys are invalid JSON.

To merge objects:

 - add fields present in only one of the two objects to the merged object.
 - for non-object-valued fields present in both objects, the field found in the second object must be used.
 - for object-valued fields present in both objects, the object values should be recursively merged according to these same rules.

Object merge can be prevented by setting the key to another value first. This is because merging is always done two values at a time; if you set a key to an object, a non-object, then an object, first the non-object falls back to the object (non-object always wins), and then the object falls back to the non-object (no merging, object is the new value). So the two objects never see each other.

These two are equivalent:

    {
        "foo" : { "a" : 42 },
        "foo" : { "b" : 43 }
    }

    {
        "foo" : { "a" : 42, "b" : 43 }
    }

And these two are equivalent:

    {
        "foo" : { "a" : 42 },
        "foo" : null,
        "foo" : { "b" : 43 }
    }

    {
        "foo" : { "b" : 43 }
    }

The intermediate setting of `"foo"` to `null` prevents the object merge.

### Paths as keys

If a key is a path expression with multiple elements, it is expanded to create an object for each path element other than the last. The last path element, combined with the value, becomes a field in the most-nested object.

In other words:

    foo.bar : 42

is equivalent to:

    foo { bar : 42 }

and:

    foo.bar.baz : 42

is equivalent to:

    foo { bar { baz : 42 } }

and so on. These values are merged in the usual way; which implies that:

    a.x : 42, a.y : 43

is equivalent to:

    a { x : 42, y : 43 }

Because path expressions work like value concatenations, you can have whitespace in keys:

    a b c : 42

is equivalent to:

    "a b c" : 42

Because path expressions are always converted to strings, even single values that would normally have another type become strings.

   - `true : 42` is `"true" : 42`
   - `3.14 : 42` is `"3.14" : 42`

As a special rule, the unquoted string `include` may not begin a path expression in a key, because it has a special interpretation (see below).

## Substitutions

Substitutions are a way of referring to other parts of the configuration tree.

The syntax is `${pathexpression}` or `${?pathexpression}` where the `pathexpression` is a path expression as described above. This path expression has the same syntax that you could use for an object key.

The `?` in `${?pathexpression}` must not have whitespace before it; the three characters `${?` must be exactly like that, grouped together.

For substitutions which are not found in the configuration tree, implementations may try to resolve them by looking at system environment variables or other external sources of configuration. (More detail on environment variables in a later section.)

Substitutions are not parsed inside quoted strings. To get a string containing a substitution, you must use value concatenation with the substitution in the unquoted portion:

    key : ${animal.favorite} is my favorite animal

Or you could quote the non-substitution portion:

    key : ${animal.favorite}" is my favorite animal"

Substitutions are resolved by looking up the path in the configuration. The path begins with the root configuration object, i.e. it is "absolute" rather than "relative."

Substitution processing is performed as the last parsing step, so a substitution can look forward in the configuration. If a configuration consists of multiple files, it may even end up retrieving a value from another file. If a key has been specified more than once, the substitution will always evaluate to its latest-assigned value (the merged object or the last non-object value that was set).

If a configuration sets a value to `null` then it should not be looked up in the external source. Unfortunately there is no way to "undo" this in a later configuration file; if you have `{ "HOME" : null }` in a root object, then `${HOME}` will never look at the environment variable. There is no equivalent to JavaScript's `delete` operation in other words.

If a substitution does not match any value present in the configuration and is not resolved by an external source, then it is undefined. An undefined substitution with the `${foo}` syntax is invalid and should generate an error.

If a substitution with the `${?foo}` syntax is undefined:

 - if it is the value of an object field then the field should not
   be created. If the field would have overridden a previously-set
   value for the same field, then the previous value remains.
 - if it is an array element then the element should not be added.
 - if it is part of a value concatenation then it should become an
   empty string.
 - `foo : ${?bar}` would avoid creating field `foo` if `bar` is
   undefined, but `foo : ${?bar} ${?baz}` would be a value
   concatenation so if `bar` or `baz` are not defined, the result
   is an empty string.

Substitutions are only allowed in object field values and array elements (value concatenations), they are not allowed in keys or nested inside other substitutions (path expressions).

A substitution is replaced with any value type (number, object, string, array, true, false, null). If the substitution is the only part of a value, then the type is preserved. Otherwise, it is value-concatenated to form a string.

Circular substitutions are invalid and should generate an error.

Implementations must take care, however, to allow objects to refer to paths within themselves. For example, this must work:

    bar : { foo : 42,
            baz : ${bar.foo}
          }

Here, if an implementation resolved all substitutions in `bar` as part of resolving the substitution `${bar.foo}`, there would be a cycle. The implementation must only resolve the `foo` field in `bar`, rather than recursing the entire `bar` object.

## Includes

### Include syntax

An _include statement_ consists of the unquoted string `include` and a single quoted string immediately following it. An include statement can appear in place of an object field.

If the unquoted string `include` appears at the start of a path expression where an object key would be expected, then it is not interpreted as a path expression or a key.

Instead, the next value must be a _quoted_ string. The quoted string is interpreted as a filename or resource name to be included.

Together, the unquoted `include` and the quoted string substitute for an object field syntactically, and are separated from the following object fields or includes by the usual comma (and as usual the comma may be omitted if there's a newline).

If an unquoted `include` at the start of a key is followed by anything other than a single quoted string, it is invalid and an error should be generated.

There can be any amount of whitespace, including newlines, between the unquoted `include` and the quoted string.

Value concatenation is NOT performed on the "argument" to `include`. The argument must be a single quoted string. No substitutions are allowed, and the argument may not be an unquoted string or any other kind of value.

Unquoted `include` has no special meaning if it is not the start of a key's path expression.

It may appear later in the key:

    # this is valid
    { foo include : 42 }
    # equivalent to
    { "foo include" : 42 }

It may appear as an object or array value:

    { foo : include } # value is the string "include"
    [ include ]       # array of one string "include"

You can quote `"include"` if you want a key that starts with the word `"include"`, only unquoted `include` is special:

    { "include" : 42 }

### Include semantics: merging

An _including file_ contains the include statement and an _included file_ is the one specified in the include statement. (They need not be regular files on a filesystem, but assume they are for the moment.)

An included file must contain an object, not an array. This is significant because both JSON and HOCON allow arrays as root values in a document.

If an included file contains an array as the root value, it is invalid and an error should be generated.

The included file should be parsed, producing a root object. The keys from the root object are conceptually substituted for the include statement in the including file.

 - If a key in the included object occurred prior to the include
   statement in the including object, the included key's value
   overrides or merges with the earlier value, exactly as with
   duplicate keys found in a single file.
 - If the including file repeats a key from an earlier-included
   object, the including file's value would override or merge
   with the one from the included file.

### Include semantics: substitution

Substitutions in included files are looked up at two different paths; first, relative to the root of the included file; second, relative to the root of the including configuration.

Recall that substitution happens as a final step, _after_ parsing. It should be done for the entire app's configuration, not for single files in isolation.

Therefore, if an included file contains substitutions, they must be "fixed up" to be relative to the app's configuration root.

Say for example that the root configuration is this:

    { a : { include "foo.conf" } }

And "foo.conf" might look like this:

    { x : 10, y : ${x} }

If you parsed "foo.conf" in isolation, then `${x}` would evaluate to 10, the value at the path `x`. If you include "foo.conf" in an object at key `a`, however, then it must be fixed up to be `${a.x}` rather than `${x}`.

Say that the root configuration redefines `a.x`, like this:

    {
        a : { include "foo.conf" }
        a : { x : 42 }
    }

Then the `${x}` in "foo.conf", which has been fixed up to `${a.x}`, would evaluate to `42` rather than to `10`. Substitution happens _after_ parsing the whole configuration.

However, there are plenty of cases where the included file might intend to refer to the application's root config. For example, to get a value from a system property or from the reference configuration. So it's not enough to only look up the "fixed up" path, it's necessary to look up the original path as well.

### Include semantics: missing files

If an included file does not exist, the include statement should be silently ignored (as if the included file contained only an empty object).

### Include semantics: locating resources

Conceptually speaking, the quoted string in an include statement identifies a file or other resource "adjacent to" the one being parsed and of the same type as the one being parsed. The meaning of "adjacent to", and the string itself, has to be specified separately for each kind of resource.

Implementations may vary in the kinds of resources they support including.

On the Java Virtual Machine, if an include statement does not identify anything "adjacent to" the including resource, implementations may wish to fall back to a classpath resource. This allows configurations found in files or URLs to access classpath resources.

For resources located on the Java classpath:

 - included resources are looked up by calling `getResource()` on
   the same class loader used to look up the including resource.
 - if the included resource name is absolute (starts with '/')
   then it should be passed to `getResource()` with the '/'
   removed.
 - if the included resource name does not start with '/' then it
   should have the "directory" of the including resource.
   prepended to it, before passing it to `getResource()`.  If the
   including resource is not absolute (no '/') and has no "parent
   directory" (is just a single path element), then the included
   relative resource name should be left as-is.
 - it would be wrong to use `getResource()` to get a URL and then
   locate the included name relative to that URL, because a class
   loader is not required to have a one-to-one mapping between
   paths in its URLs and the paths it handles in `getResource()`.
   In other words, the "adjacent to" computation should be done
   on the resource name not on the resource's URL.

For plain files on the filesystem:

 - if the included file is an absolute path then it should be kept
   absolute and loaded as such.
 - if the included file is a relative path, then it should be
   located relative to the directory containing the including
   file.  The current working directory of the process parsing a
   file must NOT be used when interpreting included paths.
 - if the file is not found, fall back to the classpath resource.
   The classpath resource should not have any package name added
   in front, it should be relative to the "root"; which means any
   leading "/" should just be removed (absolute is the same as
   relative since it's root-relative). The "/" is handled for
   consistency with including resources from inside other
   classpath resources, where the resource name may not be
   root-relative and "/" allows specifying relative to root.

URLs:

 - for both filesystem files and Java resources, if the
   included name is a URL (begins with a protocol), it would
   be reasonable behavior to try to load the URL rather than
   treating the name as a filename or resource name.
 - for files loaded from a URL, "adjacent to" should be based
   on parsing the URL's path component, replacing the last
   path element with the included name.
 - file: URLs should behave in exactly the same way as a plain
   filename

## Duration format

The supported unit strings for duration are case sensitive and must be lowercase. Exactly these strings are supported:

 - `ns`, `nanosecond`, `nanoseconds`
 - `us`, `microsecond`, `microseconds`
 - `ms`, `millisecond`, `milliseconds`
 - `s`, `second`, `seconds`
 - `m`, `minute`, `minutes`
 - `h`, `hour`, `hours`
 - `d`, `day`, `days`

## Size in bytes format

For single bytes, exactly these strings are supported:

 - `B`, `b`, `byte`, `bytes`

For powers of ten, exactly these strings are supported:

 - `kB`, `kilobyte`, `kilobytes`
 - `MB`, `megabyte`, `megabytes`
 - `GB`, `gigabyte`, `gigabytes`
 - `TB`, `terabyte`, `terabytes`
 - `PB`, `petabyte`, `petabytes`
 - `EB`, `exabyte`, `exabytes`
 - `ZB`, `zettabyte`, `zettabytes`
 - `YB`, `yottabyte`, `yottabytes`

For powers of two, exactly these strings are supported:

 - `K`, `k`, `Ki`, `KiB`, `kibibyte`, `kibibytes`
 - `M`, `m`, `Mi`, `MiB`, `mebibyte`, `mebibytes`
 - `G`, `g`, `Gi`, `GiB`, `gibibyte`, `gibibytes`
 - `T`, `t`, `Ti`, `TiB`, `tebibyte`, `tebibytes`
 - `P`, `p`, `Pi`, `PiB`, `pebibyte`, `pebibytes`
 - `E`, `e`, `Ei`, `EiB`, `exbibyte`, `exbibytes`
 - `Z`, `z`, `Zi`, `ZiB`, `zebibyte`, `zebibytes`
 - `Y`, `y`, `Yi`, `YiB`, `yobibyte`, `yobibytes`

## Conventional override by system properties

For an application's config, Java system properties _override_ settings found in the configuration file. This supports specifying config options on the command line. ie. `play -Dkey=value run`

Note : Play forks the JVM for tests - and so to use command line overrides in tests you must add `Keys.fork in Test := false` in `build.sbt` before you can use them for a test.
