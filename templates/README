Play Activator templates
========================

This project contains the built in Play Activator templates.

Developing
----------

The templates are parameterised, which means you can't run them as is.

However, there is a sync-templates command that will sync them to a working directory
where you can develop them.  The most convenvient way to use this is to use triggered
execution:

    ./build ~sync-templates

Then in another window, you can test/run the template, for example, to run the
play-scala template:

    ./buildTemplate play-scala run

Or you can even run activator in it:

    ./buildTemplate play-java ui

Now edit the template itself in this directory, and every time you save, the changes
will be saved, the template processend and synced to the directory where activator
is running.

Publishing
----------

You can publish templates by running

    ./build publish-templates

You can publish specific templates by passing them as a comma separated list to the
templates system property:

    ./build -Dtemplates=play-2.3-feature-tour publish-templates

The above will usually be more useful if you publish it for a specific version of Play:

    ./build -Dtemplates=play-2.3-feature-tour -Dplay.version=2.3-M1 publish-templates
