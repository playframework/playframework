# Aggregating reverse routers

In some situations you want to share reverse routers between sub projects that are not dependent on each other.

For example, you might have a `web` sub project, and an `api` sub project.  These sub projects may have no dependence on each other, except that the `web` project wants to render links to the `api` project (for making AJAX calls), while the `api` project wants to render links to the `web` (rendering the web link for a resource in JSON).  In this situation, it would be convenient to use the reverse router, but since these projects don't depend on each other, you can't.

Play's routes compiler offers a feature that allows a common dependency to generate the reverse routers for projects that depend on it so that the reverse routers can be shared between those projects.  This is configured using the `aggregateReverseRoutes` sbt configuration item, like this:

@[content](code/aggregate.sbt)

In this setup, the reverse routers for `api` and `web` will be generated as part of the `common` project.  Meanwhile, the forwards routers for `api` and `web` will still generate forwards routers, but not reverse routers, because their reverse routers have already been generated in the `common` project which they depend on, so they don't need to generate them.

> Note that the `common` project has a type of `Project` explicitly declared.  This is because there is a recursive reference between it and the `api` and `web` projects, through the `dependsOn` method and `aggregateReverseRoutes` setting, so the Scala type checker needs an explicit type somewhere in the chain of recursion.