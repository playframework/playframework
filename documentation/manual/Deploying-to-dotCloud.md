# Deploying to dotCloud

[[dotCoud | http://www.dotcloud.com/]] is a very flexible cloud application platform.

## Clone play2-on-dotcloud project

```bash
$ git clone git://github.com/mchv/play2-on-dotcloud.git
```

When the clone is finished you could delete the _.git_ directory.

## Copy your application code

Copy your application code in the _application_ folder.

## Create a new application on dotCloud

> Note that you need an dotCloud account, and to [[install dotCloud client | http://docs.dotcloud.com/firststeps/install/]]. 

```bash
$ dotcloud create myplay2app
Created application "myplay2app"
```

## Deploy your application

The dotCloud client check if you use git or mercurial, and if so will deploy by default only [[committed changes | http://docs.dotcloud.com/guides/git-hg/]]. Otherwise it will simply upload only the diff with the previous deployment using rsync.

```bash
$ dotcloud push myplay2app
```

If you want to commit all files and not only the diff or last committed changes use:

```bash
$ dotcloud push myplay2app --all
```

## Checking logs

If something goes weird, you could check the logs to see what's happening.

```bash
$ dotcloud logs myplay2app.play
```