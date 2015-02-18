# Play Project Developer & Contributor Guidelines

## Reporting Issues

If you wish to report an issue for Play Framework, please ensure you have done the following things:

* If it is a documentation issue with a simple fix, don't raise an issue, just edit the documentation yourself directly in GitHub and submit a pull request.  This will be quicker for you and everybody.
* If you are not 100% sure that it is a bug, then ask about it on the [mailing list](http://groups.google.com/forum/#!forum/play-framework) first.  You will get a lot more help a lot quicker on the mailing list if you raise it there.  The issue tracker is for verified bugs, not for questions.
* If you have a feature request, please raise it on the developer mailing list first.  The mailing list is the best forum to discuss new features, and it may be that Play already provides something to achieve what you want to achieve and you didn't realise.
* If you are sure you have found a bug, then raise an issue.  Please be as specific as possible, including sample code that reproduces the problem, stack traces if there are any exceptions thrown, and versions of Play, OS, Java, etc.

## Contributor Workflow

This is the process for a contributor (that is, a non Play core developer) to contribute to Play Framework.

1. Make sure you have signed the [Typesafe CLA](http://www.typesafe.com/contribute/cla); if not, sign it online.
2. Ensure that your contribution meets the following guidelines:
    1. Live up to the current code standard:
        - Not violate [DRY](http://programmer.97things.oreilly.com/wiki/index.php/Don%27t_Repeat_Yourself).
        - [Boy Scout Rule](http://programmer.97things.oreilly.com/wiki/index.php/The_Boy_Scout_Rule) needs to have been applied.
    2. Regardless if the code introduces new features or fixes bugs or regressions, it must have comprehensive tests.
    3. The code must be well documented in the Play standard documentation format (see the ‘Documentation’ section below). Each API change must have the corresponding documentation change.
    4. Implementation-wise, the following things should be avoided as much as possible:
        * Global state
        * Public mutable state
        * Implicit conversions
        * ThreadLocal
        * Locks
        * Casting
        * Introducing new, heavy external dependencies
    5. The Play API design rules are the following:
        * Play is a Java and Scala framework, make sure your changes are working for both API-s
        * Java APIs should go to `framework/play/src/main/java`, package structure is `play.myapipackage.xxxx`
        * Scala APIs should go to `framework/play/src/main/scala`, where the package structure is `play.api.myapipackage`
        * Java and Scala APIs should be implemented the following way:
            * implement the core API in scala (`play.api.xxx`)
            * if your component requires life cycle management or needs to be swappable, create a plugin, otherwise skip this step
            * wrap core API for scala users ([example]  (https://github.com/playframework/playframework/blob/master/framework/src/play-cache/src/main/scala/play/api/cache/Cache.scala#L69))
            * wrap scala API for java users ([example](https://github.com/playframework/playframework/blob/master/framework/src/play-cache/src/main/java/play/cache/Cache.java))
        * Features are forever, always think about whether a new feature really belongs to the core framework or it should be implemented as a plugin
        * Code must conform to standard style guidelines and pass all tests (see [validatePullRequest](https://github.com/playframework/playframework/blob/master/framework/validatePullRequest))
    6. New files must:
       *  Have a Typesafe copyright header in the style of ``Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>``.
       * Not use ``@author`` tags since it does not encourage [Collective Code Ownership](http://www.extremeprogramming.org/rules/collective.html).
3. Submit a pull request.  If an issue already exists for the pull request, then follow [these](http://opensoul.org/blog/archives/2012/11/09/convert-a-github-issue-into-a-pull-request/) instructions for converting an issue into a pull request.

If the pull request does not meet the above requirements then the code should **not** be merged into master, or even reviewed - regardless of how good or important it is. No exceptions.

## Core Developer Workflow

This is the process for committing code into master. There are of course exceptions to these rules, for example minor changes to comments and documentation, fixing a broken build etc.

1. Make sure you have signed the [Typesafe CLA](http://www.typesafe.com/contribute/cla), if not, sign it online.
2. Before starting to work on a feature or a fix, you have to make sure that:
    1. There is a ticket for your work in the project's issue tracker. If not, create it first (See: https://github.com/playframework/playframework/issues).
    2. The ticket has been scheduled for the current milestone.
    3. The ticket is estimated by the team.
    4. The ticket has been discussed and prioritized by the team.
3. You should always perform your work in a Git feature branch. The branch should be given a descriptive name that explains its intent.
4. When the feature or fix is completed you should open a [Pull Request](https://help.github.com/articles/using-pull-requests) on GitHub.  In order to avoid excess issues in GitHub, use the GitHub CLI to [attach the pull request to an existing issue](http://opensoul.org/blog/archives/2012/11/09/convert-a-github-issue-into-a-pull-request/).
5. The Pull Request should be reviewed by other maintainers (as many as feasible/practical). Note that the maintainers can consist of outside contributors, both within and outside the Play team. Outside contributors are encouraged to participate in the review process, it is not a closed process.
6. After the review you should fix the issues as needed (pushing a new commit for new review etc.), iterating until the reviewers give their thumbs up.
7. Once the code has passed review the Pull Request can be merged into the master branch. 

## Developer group & discussions

To discuss features, proposal and pull-requests, use the dedicated group at https://groups.google.com/forum/#!forum/play-framework-dev.


## Documentation

The documentation lives as markdown pages in the `documentation/manual` directory. Each Play branch has it own documentation version.  The documentation must conform to the Play [documentation guidelines](http://www.playframework.com/documentation/latest/Documentation).

## Work In Progress

It is ok to work on a public feature branch in the GitHub repository. Something that can sometimes be useful for early feedback etc. If so then it is preferable to name the branch accordingly. This can be done by either prefix the name with ``wip-`` as in ‘Work In Progress’, or use hierarchical names like ``wip/..``, ``feature/..`` or ``topic/..``. Either way is fine as long as it is clear that it is work in progress and not ready for merge. This work can temporarily have a lower standard. However, to be merged into master it will have to go through the regular process outlined above, with Pull Request, review etc.. 

Also, to facilitate both well-formed commits and working together, the ``wip`` and ``feature``/``topic`` identifiers also have special meaning.   Any branch labelled with ``wip`` is considered “git-unstable” and may be rebased and have its history rewritten.   Any branch with ``feature``/``topic`` in the name is considered “stable” enough for others to depend on when a group is working on a feature.

## Creating Commits And Writing Commit Messages

Follow these guidelines when creating public commits and writing commit messages.

1. If your work spans multiple local commits (for example; if you do safe point commits while working in a feature branch or work in a branch for long time doing merges/rebases etc.) then please do not commit it all but rewrite the history by squashing the commits into a single big commit which you write a good commit message for (like discussed in the following sections). For more info read this article: [Git Workflow](http://sandofsky.com/blog/git-workflow.html). Every commit should be able to be used in isolation, cherry picked etc.
2. First line should be a descriptive sentence what the commit is doing. It should be possible to fully understand what the commit does by just reading this single line. It is **not ok** to only list the ticket number, type "minor fix" or similar. Include reference to ticket number, prefixed with #, at the end of the first line. If the commit is a small fix, then you are done. If not, go to 3.
3. Following the single line description should be a blank line followed by an enumerated list with the details of the commit.
4. Add keywords for your commit (depending on the degree of automation we reach, the list may change over time):
    * ``Review by @gituser`` - if you want to notify someone on the team. The others can, and are encouraged to participate.
    * ``backport to _branch name_`` - if the fix needs to be cherry-picked to another branch (like 2.9.x, 2.10.x, etc)

Example:

    Adding monadic API to Future. Fixes #2731

      * Details 1
      * Details 2
      * Details 3

## Backporting policy

Generally, all bug fixes, improvements and new features will go to the master branch.  Backports and other commits to stable branches will only be accepted if they meet the following conditions:

* The change only affects the documentation
* The change fixes a regression that was introduced in a previous stable release from that branch
* The change fixes a bug that impacts significant number of members of the open source community with no simple work arounds available
* Any other reason that Typesafe deems appropriate

All backports and other commits to stable branches, in addition to satisfying the regular contributor guidelines, must also be binary and source compatible with previous releases on that branch.  The only exception to this is if a serious bug is impossible to fix without breaking the API, for example, a particular feature is not possible to use due to flaws in the API.

