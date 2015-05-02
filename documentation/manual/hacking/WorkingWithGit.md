<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Working with Git

This guide is designed to help new contributors get started with Play.  Some of the things mentioned here are conventions that we think are good and make contributing to Play easier, but they are certainly not prescriptive, you should use what works best for you.

## Git remotes

We recommend the convention of calling the remote for the official Play repository `origin`, and the remote for your fork your username.  This convention works well when sharing code between multiple forks, and is the convention we'll use for all the remaining git commands in this guide.  It is also the convention that works best out of the box with the [GitHub command line tool](https://github.com/github/hub).

## Branches

Typically all work should be done in branches.  If you do work directly on master, then you can only submit one pull request at a time, since if you try to submit a second from master, the second will contain commits from both your first and your second.  Working in branches allows you to isolate pull requests from each other.

It's up to you what you call your branches, some people like to include issue numbers in their branch name, others like to use a hierarchical structure.

## Squashing commits

We prefer that all pull requests be a single commit.  There are a few reasons for this:

* It's much easier and less error prone to backport single commits to stable branches than backport groups of commits.  If the change is just in one commit, then there is no opportunity for error, either the whole change is cherry picked, or it isn't.
* We aim to have our master branch to always be releasable, not just now, but also for all points in history.  If we need to back something out, we want to be confident that the commit before that is stable.
* It's much easier to get a complete picture of what happened in history when changes are self contained in one commit.

Of course, there are some situations where it's not appropriate to squash commits, this will be decided on a case by case basis, but examples of when we won't require commits to be squashed include:

* When the pull request contains commits by more than one person.  In this case, we'd prefer, where it makes sense, to squash contiguous commits by the same person.
* When the pull request is coming from a fork or branch that has been shared among the community, where rewriting history will cause issues for people that have pull changes from that fork or branch.
* Where the pull request is a very large amount of work, and the commit log is useful in understanding the evolution of that work.

However, for the general case, if your pull request contains more than one commit, then you will need to squash it.  To do this, or if you already have submitted a pull request and we ask you to squash it, then you should follow these steps:

1. Ensure that you have all the changes from the core master branch in your repo:

        git fetch origin

2. Start an interactive rebase

        git rebase -i origin/master

3. This will open up a screen in your editor, allowing you to say what should be done with each commit.  If the commit message for the first commit is suitable for describing all of the commits, then leave it as is, otherwise, change the command for it from `pick` to `reword`.
4. For each remaining commit, change the command from `pick` to `fixup`.  This tells git to merge that commit into the previous commit, using the commit message from the previous commit.
5. Save the file and exit your editor.  Git will now start the rebase.  If you told it to reword the first commit, it will prompt you in a new editor for the wording for that commit.  If all goes well, then you're done, but it may be the case that there were conflicts when applying your changes to the most recent master branch.  If that's the case, fix the conflicts, stage the fixes, and then run:

        git rebase --continue

    This may need to be repeated if there are more changes that have conflicts.
6. Now that you've rebased you can push your changes.  If you've already pushed this branch before (including if you've already created the pull request), then you will have to do a force push.  This can be done like so:

        git push yourremote yourbranch --force

### Responding to reviews/build breakages

If your pull request doesn't pass the CI build, if we review it and ask you to update your pull request, or if for any other reason you want to update your pull request, then rather than creating a new commit, amend the existing one.  This can be done by supplying the `--amend` flag when committing:

    git commit --amend

After doing an amend, you'll need to do a force push using the `--force` flag:

    git push yourremote yourbranch --force

## Starting over

Sometimes people find that they get their pull request completely wrong and want to start over.  This is fine, however there is no need to close the original pull request and open a new one.  You can use a force push to push a completely new branch into the pull request.

To start over, make sure you've got the latest changes from Play core, and create a new branch from that point:

    git fetch origin
    git checkout -b mynewbranch origin/master

Now make your changes, and then when you're ready to submit a pull request, assuming your old branch was called `myoldbranch`, push your new branch into that old branch in your repository:

    git push yourremote mynewbranch:myoldbranch --force

Now the pull request should be updated with your new branch.

## A word on changing history

You may have heard it said that you shouldn't change git history once you publish it.  Using `rebase` and `commit --amend` both change history, and using `push --force` will publish your changed history.

There are definitely times when git history shouldn't be changed after being published.  The main times are when it's likely that other people have forked your repository, or pulled changes from your repository.  Changing history in those cases will make it impossible for them to safely merge changes from your repo into their repository.  For this reason, we never change history in the official Play Framework repository.

However, when it comes to your personal fork, for branches that are just intended to be pull requests, then it's a different matter - the intent of the workflow is that your changes get "published" once they get merged into the master branch.  Before that, the history can be considered a work in progress.

If however your branch is a collaboration of many people, and you believe that other people have pull from your branch for good reasons, please let us know, we won't force squashing commits where it doesn't make sense.
