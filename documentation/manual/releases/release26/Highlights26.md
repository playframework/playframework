# What's new in Play 2.6

This page highlights the new features of Play 2.6. If you want to learn about the changes you need to make when you migrate to Play 2.6, check out the [[Play 2.6 Migration Guide|Migration26]].

## Request attributes

Requests in Play 2.6 now contain *attributes*. Attributes allow you to store extra information inside request objects. For example, you can write a filter that sets an attribute in the request and then access the attribute value later from within your actions.

Attributes are stored in a `TypedMap` that is attached to each request. `TypedMap`s are immutable maps that store type-safe keys and values. Attributes are indexed by a key and the key's type indicates the type of the attribute.

Java:
```java
// Create a TypedKey to store a User object
class Attrs {
  public static final TypedKey<User> USER = TypedKey.<User>create("user");
}

// Get the User object from the request
User user = req.attrs().get(Attrs.USER);
// Put a User object into the request
Request newReq = req.withAttrs(req.attrs().put(Attrs.USER, newUser));
```

Scala:
```scala
// Create a TypedKey to store a User object
object Attrs {
  val User: TypedKey[User] = TypedKey[User].apply("user")
}

// Get the User object from the request
val user: User = req.attrs(Attrs.User)
// Put a User object into the request
val newReq = req.withAttrs(req.attrs.updated(Attrs.User, newUser))
```

Attributes are stored in a `TypedMap`. You can read more about attributes in the `TypedMap` documentation: [Javadoc](api/java/play/libs/typedmap/TypedMap.html), [Scaladoc](api/scala/play/api/libs/typedmap/TypedMap.html).

Request tags have now been deprecated and you should migrate to use attributes instead. See the [[tags section|Migration26#Request-tags-deprecation]] in the migration docs for more information.