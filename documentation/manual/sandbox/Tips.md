# Play 2.0 tips and tricks

## To use Play Framework 2 with Twitter Bootstrap 2:

### Renaming the Bootstrap include files
* Build Play from last sources (near RC3) (Since less version has been updated to 1.2 )
* Copy bootstrap's less files into `app/assets/stylesheets/bootstrap`
* Add `bootstrap/_` before each `bootstrap.less` import declaration and rename each file the same way ( This is the convention to not compile theses files while packaging compiled assets )
* Move `bootstrap.less` file into `app/assets/stylesheets`

This can be done with a script like this (run in `app/assets/stylesheets/bootstrap`)

```bash
    for a in *.less; do mv $a _$a; done 
    sed -i "" 's|@import "|@import "bootstrap/_|' _bootstrap.less 
    mv _bootstrap.less ../bootstrap.less
    sed -i "" 's|@import "|@import "bootstrap/_|' _responsive.less 
    mv _responsive.less ../bootstrap-responsive.less
```

### Alternative: using the `Build.scala` configuration file
* Copy bootstrap's less files into `app/assets/stylesheets/bootstrap`
* Modify the `project/Build.scala` file to filter out the Bootstrap LESS files we *don't* want to compile:

* Define a new [BuildPath](https://github.com/harrah/xsbt/wiki/Paths) resolver function:

```scala
    // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory
    def customLessEntryPoints(base: File): PathFinder = (
        (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
        (base / "app" / "assets" / "stylesheets" * "*.less")
    )
```

*Note:* this wil only compile `stylesheets/bootstrap/bootstrap.less` and `stylesheets/*.less` files. If you have any other LESS files in other subdirectories under `stylesheets`; adjust the function accordingly.

* Override the default `lessEntryPoints` setting key with the new function:

```scala
    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
        lessEntryPoints <<= baseDirectory(customLessEntryPoints)
    )
```

* Include the `bootstrap.min.css` file in your view(s):

    ```html
    <link rel="stylesheet" media="screen" href="@routes.Assets.at("stylesheets/bootstrap/bootstrap.min.css")" />
```


## MongoDB

When using [Salat](https://github.com/novus/salat), make sure to disable SQL evolutions in your `application.conf` via `evolutionplugin=disabled` or you will run into inexplicable ClassCastExceptions.