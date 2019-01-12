<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Play contributor guidelines

The canonical version of this document can be found on the [Play contributor guidelines](https://playframework.com/contributing) page of the Play website.

### Prerequisites

Before making a contribution, it is important to make sure that the change you wish to make and the approach you wish to take will likely be accepted, otherwise you may end up doing a lot of work for nothing.  If the change is only small, for example, if it's a documentation change or a simple bugfix, then it's likely to be accepted with no prior discussion.  However, new features, or bigger refactorings should first be discussed on the [developer mailing list](https://groups.google.com/forum/#!forum/play-framework-dev).  Additionally, any issues with the [community label](https://github.com/playframework/playframework/issues?q=is%3Aopen+is%3Aissue+label%3Acommunity) have been agreed to be a change that will likely be accepted.

### Procedure

1. Make sure you have signed the [Lightbend CLA](http://www.lightbend.com/contribute/cla); if not, sign it online.
2. Ensure that your contribution meets the following guidelines:
    1. Live up to the current code standard:
        - Not violate [DRY](https://97-things-every-x-should-know.gitbooks.io/97-things-every-programmer-should-know/content/en/thing_30/index.html).
        - [Boy Scout Rule](https://97-things-every-x-should-know.gitbooks.io/97-things-every-programmer-should-know/content/en/thing_08/index.html) needs to have been applied.
    2. Regardless of whether the code introduces new features or fixes bugs or regressions, it must have comprehensive tests.  This includes when modifying existing code that isn't tested.
    3. The code must be well documented in the Play standard documentation format (see the [documentation guidelines](https://playframework.com/documentation/latest/Documentation).)  Each API change must have the corresponding documentation change.
    4. Implementation-wise, the following things should be avoided as much as possible:
        * Global state
        * Public mutable state
        * Implicit conversions
        * ThreadLocal
        * Locks
        * Casting
        * Introducing new, heavy external dependencies
    5. The Play API design rules are the following:
        * Play is a Java and Scala framework, make sure any changes have feature parity in both the Scala and Java APIs.
        * Java APIs should go to `framework/play/src/main/java`, package structure is `play.myapipackage.xxxx`
        * Scala APIs should go to `framework/play/src/main/scala`, where the package structure is `play.api.myapipackage`
        * Features are forever, always think about whether a new feature really belongs to the core framework or if it should be implemented as a module
        * Code must conform to standard style guidelines and pass all tests (see [Run tests](https://www.playframework.com/documentation/latest/BuildingFromSource#run-tests))
    6. New files must:
        * Have a Lightbend copyright header in the style of ``Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>``.
        * Not use ``@author`` tags since it does not encourage [Collective Code Ownership](http://www.extremeprogramming.org/rules/collective.html).
3. Ensure that your commits are squashed.  See [working with git](https://playframework.com/documentation/latest/WorkingWithGit) for more information.
4. Submit a pull request.

If the pull request does not meet the above requirements then the code should **not** be merged into master, or even reviewed - regardless of how good or important it is. No exceptions.

The pull request will be reviewed according to the [implementation decision process](https://playframework.com/community-process#Implementation-decisions).
