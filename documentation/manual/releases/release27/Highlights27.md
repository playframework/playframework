# What's new in Play 2.7

This page highlights the new features of Play 2.7. If you want to learn about the changes you need to make when you migrate to Play 2.7, check out the [[Play 2.7 Migration Guide|Migration27]].

## Constraint annotations offered for Play Java are now @Repeatable

All of the constraint annotations defined by `play.data.validation.Constraints` are now `@Repeatable`. This lets you, for example, reuse the same annotation on the same element several times but each time with different `groups`. For some constraints however it makes sense to let them repeat itself anyway, like `@ValidateWith`:

```java
@Validate(groups={GroupA.class})
@Validate(groups={GroupB.class})
public class MyForm {

    @ValidateWith(MyValidator.class)
    @ValidateWith(MyOtherValidator.class)
    @Pattern(value="[a-k]", message="Should be a - k")
    @Pattern(value="[c-v]", message="Should be c - v")
    @MinLength(value=4, groups={GroupA.class})
    @MinLength(value=7, groups={GroupB.class})
    private String name;

    //...
}
```

You can of course also make your own custom constraints `@Repeatable` as well and Play will automatically recognise that.

## Support for Caffeine

Play now offers a CacheApi implementation based on [Caffeine](https://github.com/ben-manes/caffeine/). Caffeine is the recomended cache implementation for Play users.

To migrate from EhCache to Caffeine you will have to remove `ehcache` from your dependencies and replace it with `caffeine`. To customize the settings from the defaults you will also need to update the configuration in application.conf as explained in the documentation.

Read the documentation for the [[Java cache API|JavaCache]] and [[Scala cache API|ScalaCache]] to learn more about configuring caching with Play.